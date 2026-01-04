package com.vzith.bookstack.data.sync

import com.vzith.bookstack.util.VarUintCodec
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * BookStack Android App - Y-Sync Protocol Handler (2026-01-05)
 *
 * Implements y-protocols for Hocuspocus server communication.
 * Reference: https://github.com/yjs/y-protocols/blob/master/sync.js
 *
 * Message Types:
 * - 0: SyncStep1 (send state vector, request missing updates)
 * - 1: SyncStep2 (respond with missing updates)
 * - 2: Update (incremental update)
 */
object YSyncProtocolHandler {

    // Message type constants
    const val MESSAGE_SYNC = 0
    const val SYNC_STEP1 = 0
    const val SYNC_STEP2 = 1
    const val SYNC_UPDATE = 2

    // Hocuspocus message types
    const val HOCUSPOCUS_SYNC = 0
    const val HOCUSPOCUS_AWARENESS = 1
    const val HOCUSPOCUS_AUTH = 2

    /**
     * State vector: Map of clientId to clock value
     * Represents "what updates has this client seen"
     */
    data class StateVector(
        val states: Map<Long, Long> = emptyMap()
    ) {
        companion object {
            val EMPTY = StateVector()
        }
    }

    /**
     * Encode a state vector to binary format
     * Format: [numClients] [clientId, clock]...
     */
    fun encodeStateVector(stateVector: StateVector): ByteArray {
        val output = ByteArrayOutputStream()

        // Write number of clients
        output.write(VarUintCodec.encode(stateVector.states.size))

        // Write each clientId:clock pair
        for ((clientId, clock) in stateVector.states) {
            output.write(VarUintCodec.encode(clientId))
            output.write(VarUintCodec.encode(clock))
        }

        return output.toByteArray()
    }

    /**
     * Decode a state vector from binary format
     */
    fun decodeStateVector(data: ByteArray): StateVector {
        val input = ByteArrayInputStream(data)
        val numClients = VarUintCodec.decode(input).toInt()

        val states = mutableMapOf<Long, Long>()
        repeat(numClients) {
            val clientId = VarUintCodec.decode(input)
            val clock = VarUintCodec.decode(input)
            states[clientId] = clock
        }

        return StateVector(states)
    }

    /**
     * Create SyncStep1 message (request sync from server)
     * Format: [messageType=0][syncType=0][stateVector]
     */
    fun createSyncStep1(stateVector: StateVector = StateVector.EMPTY): ByteArray {
        val output = ByteArrayOutputStream()

        // Hocuspocus wrapper: message type = sync
        output.write(HOCUSPOCUS_SYNC)

        // Sync message type = step1
        output.write(SYNC_STEP1)

        // Encode state vector
        val encodedStateVector = encodeStateVector(stateVector)
        output.write(encodedStateVector)

        return output.toByteArray()
    }

    /**
     * Create SyncStep2 message (respond with updates)
     * Format: [messageType=0][syncType=1][update]
     */
    fun createSyncStep2(update: ByteArray): ByteArray {
        val output = ByteArrayOutputStream()

        output.write(HOCUSPOCUS_SYNC)
        output.write(SYNC_STEP2)
        output.write(update)

        return output.toByteArray()
    }

    /**
     * Create Update message (incremental update)
     * Format: [messageType=0][syncType=2][update]
     */
    fun createUpdate(update: ByteArray): ByteArray {
        val output = ByteArrayOutputStream()

        output.write(HOCUSPOCUS_SYNC)
        output.write(SYNC_UPDATE)
        output.write(update)

        return output.toByteArray()
    }

    /**
     * Create authentication message for Hocuspocus
     * Format: [messageType=2][token]
     */
    fun createAuthMessage(token: String): ByteArray {
        val output = ByteArrayOutputStream()

        output.write(HOCUSPOCUS_AUTH)
        output.write(VarUintCodec.encodeString(token))

        return output.toByteArray()
    }

    /**
     * Parse incoming message from server
     */
    sealed class SyncMessage {
        data class Step1(val stateVector: StateVector) : SyncMessage()
        data class Step2(val update: ByteArray) : SyncMessage()
        data class Update(val update: ByteArray) : SyncMessage()
        data class Awareness(val data: ByteArray) : SyncMessage()
        data class Auth(val success: Boolean, val message: String?) : SyncMessage()
        data class Unknown(val type: Int) : SyncMessage()
    }

    /**
     * Parse a message from the server
     */
    fun parseMessage(data: ByteArray): SyncMessage {
        if (data.isEmpty()) return SyncMessage.Unknown(-1)

        val input = ByteArrayInputStream(data)
        val messageType = input.read()

        return when (messageType) {
            HOCUSPOCUS_SYNC -> {
                val syncType = input.read()
                val remaining = input.readBytes()

                when (syncType) {
                    SYNC_STEP1 -> SyncMessage.Step1(decodeStateVector(remaining))
                    SYNC_STEP2 -> SyncMessage.Step2(remaining)
                    SYNC_UPDATE -> SyncMessage.Update(remaining)
                    else -> SyncMessage.Unknown(syncType)
                }
            }
            HOCUSPOCUS_AWARENESS -> {
                SyncMessage.Awareness(input.readBytes())
            }
            HOCUSPOCUS_AUTH -> {
                // Auth response: first byte is success (1) or failure (0)
                val success = input.read() == 1
                val message = if (input.available() > 0) {
                    VarUintCodec.decodeString(input)
                } else null
                SyncMessage.Auth(success, message)
            }
            else -> SyncMessage.Unknown(messageType)
        }
    }
}
