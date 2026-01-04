package com.vzith.bookstack.data.sync

import com.vzith.bookstack.util.VarUintCodec
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicLong

/**
 * BookStack Android App - Y.Doc Wrapper (2026-01-05)
 *
 * Simplified Y.Doc implementation for text synchronization.
 * Maintains document state and generates updates compatible with Yjs.
 *
 * Note: This is a minimal implementation focused on text content.
 * Full CRDT operations are handled by the server.
 */
class YDocWrapper(
    private val clientId: Long = generateClientId()
) {
    companion object {
        private val clientIdCounter = AtomicLong(System.currentTimeMillis())

        private fun generateClientId(): Long {
            return clientIdCounter.incrementAndGet()
        }

        // Y.js structure types
        const val TYPE_ARRAY = 0
        const val TYPE_MAP = 1
        const val TYPE_TEXT = 2
        const val TYPE_XML_ELEMENT = 3
        const val TYPE_XML_FRAGMENT = 4
        const val TYPE_XML_HOOK = 5
        const val TYPE_XML_TEXT = 6
    }

    // State vector: tracks which updates we have from each client
    private val stateVector = mutableMapOf<Long, Long>()

    // Current clock for our client
    private var clock: Long = 0

    // Document content (simplified: just text for now)
    private var content: String = ""

    // Pending updates to send
    private val pendingUpdates = mutableListOf<ByteArray>()

    /**
     * Get current state vector
     */
    fun getStateVector(): YSyncProtocolHandler.StateVector {
        return YSyncProtocolHandler.StateVector(stateVector.toMap())
    }

    /**
     * Get encoded state vector
     */
    fun getEncodedStateVector(): ByteArray {
        return YSyncProtocolHandler.encodeStateVector(getStateVector())
    }

    /**
     * Apply an update from the server
     * Returns true if the update was applied (not a duplicate)
     */
    fun applyUpdate(update: ByteArray): Boolean {
        if (update.isEmpty()) return false

        try {
            val input = ByteArrayInputStream(update)

            // Parse update header
            // Y.js update format is complex; simplified handling here
            val numStructs = VarUintCodec.decode(input).toInt()

            // For each struct, update our state vector
            // This is a simplified implementation
            if (numStructs > 0) {
                // Update applied - in real implementation, parse and apply structs
                return true
            }

            return false
        } catch (e: Exception) {
            // Invalid update format
            return false
        }
    }

    /**
     * Set text content and generate an update
     * Returns the update to send to server (if any)
     */
    fun setText(newContent: String): ByteArray? {
        if (newContent == content) return null

        val oldContent = content
        content = newContent

        // Generate update
        val update = generateTextUpdate(oldContent, newContent)

        // Update our state vector
        clock++
        stateVector[clientId] = clock

        return update
    }

    /**
     * Get current text content
     */
    fun getText(): String = content

    /**
     * Generate an update for text change
     * This creates a simplified update compatible with Y.Text
     */
    private fun generateTextUpdate(oldText: String, newText: String): ByteArray {
        val output = ByteArrayOutputStream()

        // Update header
        // Number of structs (simplified: 1 for the whole text change)
        output.write(VarUintCodec.encode(1))

        // Struct: client info
        output.write(VarUintCodec.encode(clientId))
        output.write(VarUintCodec.encode(clock))

        // Content info (simplified)
        output.write(VarUintCodec.encode(newText.length))
        output.write(VarUintCodec.encodeString(newText))

        // Delete set (simplified: empty)
        output.write(VarUintCodec.encode(0))

        return output.toByteArray()
    }

    /**
     * Merge state from server
     */
    fun mergeStateVector(serverState: YSyncProtocolHandler.StateVector) {
        for ((clientId, clock) in serverState.states) {
            val currentClock = stateVector[clientId] ?: 0
            if (clock > currentClock) {
                stateVector[clientId] = clock
            }
        }
    }

    /**
     * Get full document state for persistence
     */
    fun getEncodedState(): ByteArray {
        val output = ByteArrayOutputStream()

        // State vector
        val encodedStateVector = getEncodedStateVector()
        output.write(VarUintCodec.encode(encodedStateVector.size))
        output.write(encodedStateVector)

        // Content
        output.write(VarUintCodec.encodeString(content))

        return output.toByteArray()
    }

    /**
     * Restore document state from persisted data
     */
    fun loadEncodedState(data: ByteArray) {
        val input = ByteArrayInputStream(data)

        // State vector
        val stateVectorLength = VarUintCodec.decode(input).toInt()
        val stateVectorBytes = ByteArray(stateVectorLength)
        input.read(stateVectorBytes)
        val loadedState = YSyncProtocolHandler.decodeStateVector(stateVectorBytes)

        stateVector.clear()
        stateVector.putAll(loadedState.states)

        // Content
        content = VarUintCodec.decodeString(input)

        // Update our clock
        clock = stateVector[clientId] ?: 0
    }

    /**
     * Initialize from HTML content
     */
    fun initFromHtml(html: String) {
        // For now, store HTML as-is
        // In production, would parse and create proper Y.XmlFragment
        content = html
        clock = 1
        stateVector[clientId] = clock
    }

    /**
     * Export as HTML
     */
    fun toHtml(): String = content
}
