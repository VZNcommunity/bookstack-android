package com.vzith.bookstack.data.sync

import android.util.Log
import com.vzith.bookstack.BookStackApplication
import com.vzith.bookstack.data.db.BookStackDatabase
import com.vzith.bookstack.data.db.entity.SyncQueueEntity
import com.vzith.bookstack.data.db.entity.YDocStateEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * BookStack Android App - Sync Manager (2026-01-05)
 *
 * Orchestrates CRDT synchronization between the editor and server.
 * Handles offline queueing and replay.
 */
class SyncManager(
    private val pageId: Int,
    private val onContentChange: (String) -> Unit
) {
    companion object {
        private const val TAG = "SyncManager"
    }

    private val database = BookStackDatabase.getDatabase(BookStackApplication.instance)
    private val syncQueueDao = database.syncQueueDao()
    private val yDocStateDao = database.yDocStateDao()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _syncState = MutableStateFlow(SyncState.DISCONNECTED)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private var yDoc: YDocWrapper? = null
    private var webSocketClient: SyncWebSocketClient? = null

    enum class SyncState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        SYNCED,
        ERROR
    }

    /**
     * Initialize the sync manager for a page
     * @param initialHtml Initial HTML content from the page
     */
    suspend fun initialize(initialHtml: String) {
        Log.d(TAG, "Initializing sync for page $pageId")

        // Load or create Y.Doc
        yDoc = YDocWrapper()

        // Try to load persisted state
        val savedState = yDocStateDao.getState(pageId)
        if (savedState != null) {
            Log.d(TAG, "Loading persisted Y.Doc state")
            yDoc?.loadEncodedState(savedState.documentState)
        } else {
            Log.d(TAG, "Initializing Y.Doc from HTML")
            yDoc?.initFromHtml(initialHtml)
        }

        // Connect to sync server
        connect()
    }

    /**
     * Connect to the sync server
     */
    fun connect() {
        if (webSocketClient != null) {
            webSocketClient?.disconnect()
        }

        _syncState.value = SyncState.CONNECTING

        webSocketClient = SyncWebSocketClient(
            pageId = pageId,
            onMessage = { handleMessage(it) },
            onStateChange = { handleConnectionState(it) }
        )

        webSocketClient?.connect()
    }

    /**
     * Handle incoming sync messages
     */
    private fun handleMessage(message: YSyncProtocolHandler.SyncMessage) {
        scope.launch {
            when (message) {
                is YSyncProtocolHandler.SyncMessage.Step2 -> {
                    // Apply server's updates
                    yDoc?.applyUpdate(message.update)
                    persistState()

                    // Notify content change
                    yDoc?.toHtml()?.let { html ->
                        withContext(Dispatchers.Main) {
                            onContentChange(html)
                        }
                    }

                    // Replay queued updates
                    replayQueuedUpdates()
                }

                is YSyncProtocolHandler.SyncMessage.Update -> {
                    // Apply incremental update
                    yDoc?.applyUpdate(message.update)
                    persistState()

                    // Notify content change
                    yDoc?.toHtml()?.let { html ->
                        withContext(Dispatchers.Main) {
                            onContentChange(html)
                        }
                    }
                }

                else -> {
                    Log.d(TAG, "Unhandled message type: $message")
                }
            }
        }
    }

    /**
     * Handle connection state changes
     */
    private fun handleConnectionState(state: SyncWebSocketClient.ConnectionState) {
        _syncState.value = when (state) {
            SyncWebSocketClient.ConnectionState.DISCONNECTED -> SyncState.DISCONNECTED
            SyncWebSocketClient.ConnectionState.CONNECTING -> SyncState.CONNECTING
            SyncWebSocketClient.ConnectionState.CONNECTED -> SyncState.CONNECTING
            SyncWebSocketClient.ConnectionState.AUTHENTICATED -> SyncState.CONNECTED
            SyncWebSocketClient.ConnectionState.SYNCED -> SyncState.SYNCED
            SyncWebSocketClient.ConnectionState.ERROR -> SyncState.ERROR
        }
    }

    /**
     * Called when the editor content changes
     * @param html New HTML content
     */
    suspend fun onEditorChange(html: String) {
        val update = yDoc?.setText(html) ?: return

        if (_syncState.value == SyncState.SYNCED) {
            // Send update immediately
            webSocketClient?.sendUpdate(update)
        } else {
            // Queue for later
            queueUpdate(update)
        }

        persistState()
    }

    /**
     * Queue an update for later sync
     */
    private suspend fun queueUpdate(update: ByteArray) {
        val entry = SyncQueueEntity(
            pageId = pageId,
            updateData = update
        )
        syncQueueDao.insert(entry)
        Log.d(TAG, "Queued update for offline sync")
    }

    /**
     * Replay queued updates after reconnection
     */
    private suspend fun replayQueuedUpdates() {
        val queued = syncQueueDao.getQueueForPage(pageId)
        if (queued.isEmpty()) return

        Log.d(TAG, "Replaying ${queued.size} queued updates")

        for (entry in queued) {
            webSocketClient?.sendUpdate(entry.updateData)
            syncQueueDao.delete(entry)
        }
    }

    /**
     * Persist Y.Doc state to database
     */
    private suspend fun persistState() {
        val doc = yDoc ?: return

        val state = YDocStateEntity(
            pageId = pageId,
            stateVector = doc.getEncodedStateVector(),
            documentState = doc.getEncodedState()
        )

        yDocStateDao.insertOrUpdate(state)
    }

    /**
     * Get current content
     */
    fun getContent(): String? = yDoc?.toHtml()

    /**
     * Disconnect and cleanup
     */
    fun disconnect() {
        webSocketClient?.disconnect()
        _syncState.value = SyncState.DISCONNECTED
    }

    /**
     * Destroy and release resources
     */
    fun destroy() {
        webSocketClient?.destroy()
        scope.cancel()
    }
}
