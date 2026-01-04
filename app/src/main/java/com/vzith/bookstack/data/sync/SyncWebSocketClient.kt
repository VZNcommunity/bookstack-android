package com.vzith.bookstack.data.sync

import android.util.Log
import com.vzith.bookstack.BookStackApplication
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import okio.ByteString
import okio.ByteString.Companion.toByteString
import java.util.concurrent.TimeUnit

/**
 * BookStack Android App - Sync WebSocket Client (2026-01-05)
 *
 * WebSocket client for Hocuspocus server communication.
 * Handles connection, authentication, and message routing.
 */
class SyncWebSocketClient(
    private val pageId: Int,
    private val onMessage: (YSyncProtocolHandler.SyncMessage) -> Unit,
    private val onStateChange: (ConnectionState) -> Unit
) {
    companion object {
        private const val TAG = "SyncWebSocketClient"
        private const val RECONNECT_DELAY_MS = 3000L
        private const val MAX_RECONNECT_ATTEMPTS = 5
    }

    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        AUTHENTICATED,
        SYNCED,
        ERROR
    }

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private var webSocket: WebSocket? = null
    private var reconnectAttempts = 0
    private var reconnectJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d(TAG, "WebSocket opened for page-$pageId")
            updateState(ConnectionState.CONNECTED)
            reconnectAttempts = 0

            // Send auth message
            val keystoreManager = BookStackApplication.instance.keystoreManager
            val tokenId = keystoreManager.getTokenId()
            val tokenSecret = keystoreManager.getTokenSecret()

            if (tokenId != null && tokenSecret != null) {
                val authMessage = YSyncProtocolHandler.createAuthMessage("$tokenId:$tokenSecret")
                sendBinary(authMessage)
            } else {
                // No auth, proceed to sync
                sendSyncStep1()
            }
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            Log.d(TAG, "Received binary message: ${bytes.size} bytes")
            val message = YSyncProtocolHandler.parseMessage(bytes.toByteArray())

            when (message) {
                is YSyncProtocolHandler.SyncMessage.Auth -> {
                    if (message.success) {
                        Log.d(TAG, "Auth successful")
                        updateState(ConnectionState.AUTHENTICATED)
                        sendSyncStep1()
                    } else {
                        Log.e(TAG, "Auth failed: ${message.message}")
                        updateState(ConnectionState.ERROR)
                    }
                }
                is YSyncProtocolHandler.SyncMessage.Step2 -> {
                    Log.d(TAG, "Received SyncStep2: ${message.update.size} bytes")
                    updateState(ConnectionState.SYNCED)
                    onMessage(message)
                }
                is YSyncProtocolHandler.SyncMessage.Update -> {
                    Log.d(TAG, "Received Update: ${message.update.size} bytes")
                    onMessage(message)
                }
                else -> {
                    onMessage(message)
                }
            }
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.d(TAG, "Received text message (unexpected): $text")
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "WebSocket closing: $code $reason")
            webSocket.close(1000, null)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "WebSocket closed: $code $reason")
            updateState(ConnectionState.DISCONNECTED)
            scheduleReconnect()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(TAG, "WebSocket failure", t)
            updateState(ConnectionState.ERROR)
            scheduleReconnect()
        }
    }

    /**
     * Connect to the sync server
     */
    fun connect() {
        if (_connectionState.value == ConnectionState.CONNECTING ||
            _connectionState.value == ConnectionState.CONNECTED ||
            _connectionState.value == ConnectionState.SYNCED) {
            return
        }

        updateState(ConnectionState.CONNECTING)

        val keystoreManager = BookStackApplication.instance.keystoreManager
        val syncServerUrl = keystoreManager.getSyncServerUrl()
            ?: "ws://100.78.187.47:3032"

        // Connect to: ws://server:port/page-{pageId}
        val url = "$syncServerUrl/page-$pageId"
        Log.d(TAG, "Connecting to: $url")

        val request = Request.Builder()
            .url(url)
            .build()

        webSocket = client.newWebSocket(request, listener)
    }

    /**
     * Disconnect from server
     */
    fun disconnect() {
        reconnectJob?.cancel()
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
        updateState(ConnectionState.DISCONNECTED)
    }

    /**
     * Send SyncStep1 to initiate sync
     */
    fun sendSyncStep1(stateVector: YSyncProtocolHandler.StateVector = YSyncProtocolHandler.StateVector.EMPTY) {
        val message = YSyncProtocolHandler.createSyncStep1(stateVector)
        sendBinary(message)
    }

    /**
     * Send an update to the server
     */
    fun sendUpdate(update: ByteArray) {
        val message = YSyncProtocolHandler.createUpdate(update)
        sendBinary(message)
    }

    /**
     * Send binary data
     */
    private fun sendBinary(data: ByteArray): Boolean {
        return webSocket?.send(data.toByteString()) ?: false
    }

    /**
     * Update connection state
     */
    private fun updateState(state: ConnectionState) {
        _connectionState.value = state
        onStateChange(state)
    }

    /**
     * Schedule reconnection attempt
     */
    private fun scheduleReconnect() {
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            Log.w(TAG, "Max reconnect attempts reached")
            return
        }

        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(RECONNECT_DELAY_MS * (reconnectAttempts + 1))
            reconnectAttempts++
            Log.d(TAG, "Reconnecting (attempt $reconnectAttempts)")
            connect()
        }
    }

    /**
     * Clean up resources
     */
    fun destroy() {
        disconnect()
        scope.cancel()
    }
}
