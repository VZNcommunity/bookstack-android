package com.vzith.bookstack.util

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * BookStack Android App - VarUint Codec (2026-01-05)
 *
 * Variable-length unsigned integer encoding used by Y.js protocol.
 * Uses 7 bits per byte with MSB as continuation flag.
 *
 * Reference: https://github.com/yjs/y-protocols
 */
object VarUintCodec {

    /**
     * Encode an unsigned integer to variable-length bytes
     * Each byte uses 7 bits for data and MSB as continuation flag
     */
    fun encode(value: Long): ByteArray {
        val output = ByteArrayOutputStream()
        var remaining = value

        while (remaining > 0x7F) {
            output.write((remaining.toInt() and 0x7F) or 0x80)
            remaining = remaining shr 7
        }
        output.write(remaining.toInt() and 0x7F)

        return output.toByteArray()
    }

    fun encode(value: Int): ByteArray = encode(value.toLong())

    /**
     * Decode a variable-length unsigned integer from byte array
     * Returns Pair(value, bytesConsumed)
     */
    fun decode(data: ByteArray, offset: Int = 0): Pair<Long, Int> {
        var value: Long = 0
        var shift = 0
        var pos = offset

        while (pos < data.size) {
            val byte = data[pos].toInt() and 0xFF
            value = value or ((byte.toLong() and 0x7F) shl shift)
            pos++

            if ((byte and 0x80) == 0) {
                break
            }
            shift += 7
        }

        return Pair(value, pos - offset)
    }

    /**
     * Decode from InputStream
     */
    fun decode(input: ByteArrayInputStream): Long {
        var value: Long = 0
        var shift = 0

        while (true) {
            val byte = input.read()
            if (byte == -1) break

            value = value or ((byte.toLong() and 0x7F) shl shift)

            if ((byte and 0x80) == 0) {
                break
            }
            shift += 7
        }

        return value
    }

    /**
     * Write a string with length prefix
     */
    fun encodeString(value: String): ByteArray {
        val bytes = value.toByteArray(Charsets.UTF_8)
        val output = ByteArrayOutputStream()
        output.write(encode(bytes.size))
        output.write(bytes)
        return output.toByteArray()
    }

    /**
     * Read a length-prefixed string
     */
    fun decodeString(input: ByteArrayInputStream): String {
        val length = decode(input).toInt()
        val bytes = ByteArray(length)
        input.read(bytes)
        return String(bytes, Charsets.UTF_8)
    }

    /**
     * Write bytes with length prefix
     */
    fun encodeBytes(value: ByteArray): ByteArray {
        val output = ByteArrayOutputStream()
        output.write(encode(value.size))
        output.write(value)
        return output.toByteArray()
    }

    /**
     * Read length-prefixed bytes
     */
    fun decodeBytes(input: ByteArrayInputStream): ByteArray {
        val length = decode(input).toInt()
        val bytes = ByteArray(length)
        input.read(bytes)
        return bytes
    }
}
