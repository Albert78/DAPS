package de.dh.pump.omnipod.protocol.transport

import de.dh.pump.protocol.toHex
import java.nio.ByteBuffer

class StringLengthPrefixEncoding private constructor() {

    companion object {

        private const val LENGTH_BYTES = 2

        fun parseKeys(keys: Array<String>, payload: ByteArray): Array<ByteArray> {
            val ret = Array(keys.size) { ByteArray(0) }
            var remaining = payload
            for ((index, key) in keys.withIndex()) {
                require(remaining.size >= key.length) { "Payload too short for key: $key in ${payload.toHex()}" }
                when {
                    remaining.copyOfRange(0, key.length).decodeToString() != key ->
                        throw IllegalArgumentException("Key not found: $key in ${payload.toHex()}")
                    index == keys.size - 1 && remaining.size == key.length ->
                        return ret
                }
                require(remaining.size >= key.length + LENGTH_BYTES) { "Payload too short for key length header" }

                remaining = remaining.copyOfRange(key.length, remaining.size)
                val length = ((remaining[0].toInt() and 0xFF) shl 8) or (remaining[1].toInt() and 0xFF)
                require(remaining.size >= LENGTH_BYTES + length) { "Payload too short for length $length" }
                ret[index] = remaining.copyOfRange(LENGTH_BYTES, LENGTH_BYTES + length)
                remaining = remaining.copyOfRange(LENGTH_BYTES + length, remaining.size)
            }
            return ret
        }

        fun formatKeys(keys: Array<String>, payloads: Array<ByteArray>): ByteArray {
            val payloadTotalSize = payloads.fold(0) { acc, i -> acc + i.size }
            val keyTotalSize = keys.fold(0) { acc, i -> acc + i.length }
            val zeros = payloads.fold(0) { acc, i -> acc + if (i.isEmpty()) 1 else 0 }

            val bb = ByteBuffer.allocate(2 * (keys.size - zeros) + keyTotalSize + payloadTotalSize)
            for (idx in keys.indices) {
                val k = keys[idx]
                val payload = payloads[idx]
                bb.put(k.toByteArray())
                if (payload.isNotEmpty()) {
                    bb.putShort(payload.size.toShort())
                    bb.put(payload)
                }
            }

            val ret = ByteArray(bb.position())
            bb.flip()
            bb.get(ret)

            return ret
        }
    }
}