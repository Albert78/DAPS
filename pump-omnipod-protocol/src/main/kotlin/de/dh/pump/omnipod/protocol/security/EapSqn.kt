package de.dh.pump.omnipod.protocol.security

import java.nio.ByteBuffer

class EapSqn(val value: ByteArray) {
    constructor(v: Long) : this(fromLong(v))

    init {
        require(value.size == SIZE) { "Eap SQN must be $SIZE bytes long" }
    }

    fun increment(): EapSqn = EapSqn(toLong() + 1)

    fun toLong(): Long {
        return ByteBuffer.wrap(
            byteArrayOf(0x00, 0x00) + value
        ).long
    }

    override fun toString(): String = "EapSqn(value=${toLong()})"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as EapSqn
        return value.contentEquals(other.value)
    }

    override fun hashCode(): Int = value.contentHashCode()

    companion object {
        private const val SIZE = 6
        private fun fromLong(v: Long): ByteArray {
            return ByteBuffer.allocate(8).putLong(v).array().copyOfRange(2, 8)
        }
    }
}