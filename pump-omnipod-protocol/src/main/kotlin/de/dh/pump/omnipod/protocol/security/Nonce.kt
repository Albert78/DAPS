package de.dh.pump.omnipod.protocol.security

import java.nio.ByteBuffer

data class Nonce(val prefix: ByteArray, var sqn: Long) {
    init {
        require(prefix.size == 8) { "Nonce prefix should be 8 bytes long" }
    }

    fun increment(podReceiving: Boolean): ByteArray {
        sqn++
        val ret = ByteBuffer.allocate(8)
            .putLong(sqn)
            .array()
            .copyOfRange(3, 8)
        if (podReceiving) {
            ret[0] = (ret[0].toInt() and 127).toByte()
        } else {
            ret[0] = (ret[0].toInt() or 128).toByte()
        }
        return prefix + ret
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Nonce

        if (!prefix.contentEquals(other.prefix)) return false
        if (sqn != other.sqn) return false

        return true
    }

    override fun hashCode(): Int {
        var result = prefix.contentHashCode()
        result = 31 * result + sqn.hashCode()
        return result
    }
}