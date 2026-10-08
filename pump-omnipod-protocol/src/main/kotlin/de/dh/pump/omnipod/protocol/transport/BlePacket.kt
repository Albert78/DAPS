package de.dh.pump.omnipod.protocol.transport

import java.nio.ByteBuffer

sealed class BlePacket {
    abstract val payload: ByteArray
    abstract fun toByteArray(): ByteArray

    companion object {
        const val MAX_SIZE = 20
    }
}

data class FirstBlePacket(
    val fullFragments: Int,
    override val payload: ByteArray,
    val size: Byte? = null,
    val crc32: Long? = null,
    val oneExtraPacket: Boolean = false
) : BlePacket() {

    override fun toByteArray(): ByteArray {
        val bb = ByteBuffer.allocate(MAX_SIZE)
            .put(0)
            .put(fullFragments.toByte())
        crc32?.let { bb.putInt(it.toInt()) }
        size?.let { bb.put(it) }
        bb.put(payload)

        val pos = bb.position()
        val ret = ByteArray(MAX_SIZE)
        bb.flip()
        bb.get(ret, 0, pos)
        return ret
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as FirstBlePacket
        if (fullFragments != other.fullFragments) return false
        if (!payload.contentEquals(other.payload)) return false
        if (size != other.size) return false
        if (crc32 != other.crc32) return false
        if (oneExtraPacket != other.oneExtraPacket) return false
        return true
    }

    override fun hashCode(): Int {
        var result = fullFragments
        result = 31 * result + payload.contentHashCode()
        result = 31 * result + (size?.hashCode() ?: 0)
        result = 31 * result + (crc32?.hashCode() ?: 0)
        result = 31 * result + oneExtraPacket.hashCode()
        return result
    }

    companion object {
        fun parse(payload: ByteArray): FirstBlePacket {
            require(payload.isNotEmpty() && payload.size >= HEADER_SIZE_WITH_MIDDLE_PACKETS) { "Payload too short for FirstBlePacket" }
            require(payload[0].toInt() == 0) { "Incorrect FirstBlePacket index: ${payload[0]}" }

            val fullFragments = payload[1].toInt()
            require(fullFragments < MAX_FRAGMENTS) { "Received more than $MAX_FRAGMENTS fragments" }

            return when {
                fullFragments == 0 -> {
                    val rest = payload[6].toInt() and 0xFF
                    val end = kotlin.math.min(rest + HEADER_SIZE_WITHOUT_MIDDLE_PACKETS, payload.size)
                    FirstBlePacket(
                        fullFragments = fullFragments,
                        payload = payload.copyOfRange(HEADER_SIZE_WITHOUT_MIDDLE_PACKETS, end),
                        crc32 = ByteBuffer.wrap(payload.copyOfRange(2, 6)).int.toLong() and 0xFFFFFFFFL,
                        size = rest.toByte(),
                        oneExtraPacket = rest + HEADER_SIZE_WITHOUT_MIDDLE_PACKETS > end
                    )
                }
                payload.size < MAX_SIZE -> throw IllegalArgumentException("Incorrect FirstBlePacket size: ${payload.size}")
                else -> FirstBlePacket(
                    fullFragments = fullFragments,
                    payload = payload.copyOfRange(HEADER_SIZE_WITH_MIDDLE_PACKETS, MAX_SIZE)
                )
            }
        }

        private const val HEADER_SIZE_WITHOUT_MIDDLE_PACKETS = 7
        private const val HEADER_SIZE_WITH_MIDDLE_PACKETS = 2
        internal const val CAPACITY_WITHOUT_MIDDLE_PACKETS = MAX_SIZE - HEADER_SIZE_WITHOUT_MIDDLE_PACKETS
        internal const val CAPACITY_WITH_MIDDLE_PACKETS = MAX_SIZE - HEADER_SIZE_WITH_MIDDLE_PACKETS
        internal const val CAPACITY_WITH_THE_OPTIONAL_PLUS_ONE_PACKET = 18
        private const val MAX_FRAGMENTS = 15
    }
}

data class MiddleBlePacket(val index: Byte, override val payload: ByteArray) : BlePacket() {
    override fun toByteArray(): ByteArray = byteArrayOf(index) + payload

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as MiddleBlePacket
        if (index != other.index) return false
        return payload.contentEquals(other.payload)
    }

    override fun hashCode(): Int {
        var result = index.toInt()
        result = 31 * result + payload.contentHashCode()
        return result
    }

    companion object {
        fun parse(payload: ByteArray): MiddleBlePacket {
            require(payload.size >= MAX_SIZE) { "MiddleBlePacket size must be $MAX_SIZE" }
            return MiddleBlePacket(
                index = payload[0],
                payload = payload.copyOfRange(1, MAX_SIZE)
            )
        }

        internal const val CAPACITY = 19
    }
}

data class LastBlePacket(
    val index: Byte,
    val size: Byte,
    override val payload: ByteArray,
    val crc32: Long,
    val oneExtraPacket: Boolean = false
) : BlePacket() {

    override fun toByteArray(): ByteArray {
        val bb = ByteBuffer.allocate(MAX_SIZE)
            .put(index)
            .put(size)
            .putInt(crc32.toInt())
            .put(payload)
        val pos = bb.position()
        val ret = ByteArray(MAX_SIZE)
        bb.flip()
        bb.get(ret, 0, pos)
        return ret
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as LastBlePacket
        if (index != other.index) return false
        if (size != other.size) return false
        if (!payload.contentEquals(other.payload)) return false
        if (crc32 != other.crc32) return false
        if (oneExtraPacket != other.oneExtraPacket) return false
        return true
    }

    override fun hashCode(): Int {
        var result = index.toInt()
        result = 31 * result + size.hashCode()
        result = 31 * result + payload.contentHashCode()
        result = 31 * result + crc32.hashCode()
        result = 31 * result + oneExtraPacket.hashCode()
        return result
    }

    companion object {
        fun parse(payload: ByteArray): LastBlePacket {
            require(payload.size >= HEADER_SIZE) { "Payload too short for LastBlePacket" }
            val rest = payload[1].toInt() and 0xFF
            val end = kotlin.math.min(rest + HEADER_SIZE, payload.size)
            return LastBlePacket(
                index = payload[0],
                crc32 = ByteBuffer.wrap(payload.copyOfRange(2, 6)).int.toLong() and 0xFFFFFFFFL,
                oneExtraPacket = rest + HEADER_SIZE > end,
                size = rest.toByte(),
                payload = payload.copyOfRange(HEADER_SIZE, end)
            )
        }

        private const val HEADER_SIZE = 6
        internal const val CAPACITY = MAX_SIZE - HEADER_SIZE
    }
}

data class LastOptionalPlusOneBlePacket(
    val index: Byte,
    override val payload: ByteArray,
    val size: Byte
) : BlePacket() {

    override fun toByteArray(): ByteArray {
        return byteArrayOf(index, size) + payload + ByteArray(MAX_SIZE - payload.size - 2)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as LastOptionalPlusOneBlePacket
        if (index != other.index) return false
        if (!payload.contentEquals(other.payload)) return false
        if (size != other.size) return false
        return true
    }

    override fun hashCode(): Int {
        var result = index.toInt()
        result = 31 * result + payload.contentHashCode()
        result = 31 * result + size.hashCode()
        return result
    }

    companion object {
        fun parse(payload: ByteArray): LastOptionalPlusOneBlePacket {
            require(payload.size >= 2) { "Payload too short for LastOptionalPlusOneBlePacket" }
            val size = payload[1].toInt() and 0xFF
            require(payload.size >= HEADER_SIZE + size) { "Payload truncated for LastOptionalPlusOneBlePacket" }
            return LastOptionalPlusOneBlePacket(
                index = payload[0],
                payload = payload.copyOfRange(HEADER_SIZE, HEADER_SIZE + size),
                size = size.toByte()
            )
        }

        private const val HEADER_SIZE = 2
    }
}