package de.dh.pump.protocol

class ByteReader(private val bytes: ByteArray) {
    private var offset: Int = 0

    val remaining: Int
        get() = bytes.size - offset

    fun readUInt8(): Int {
        requireAvailable(1)
        return bytes[offset++].toInt() and 0xff
    }

    fun readUInt16Le(): Int {
        val lo = readUInt8()
        val hi = readUInt8()
        return lo or (hi shl 8)
    }

    fun readUInt16Be(): Int {
        val hi = readUInt8()
        val lo = readUInt8()
        return (hi shl 8) or lo
    }

    fun readUInt32Le(): Long {
        val b0 = readUInt8().toLong()
        val b1 = readUInt8().toLong()
        val b2 = readUInt8().toLong()
        val b3 = readUInt8().toLong()
        return b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
    }

    fun readBytes(length: Int): ByteArray {
        require(length >= 0) { "length must be >= 0" }
        requireAvailable(length)
        return bytes.copyOfRange(offset, offset + length).also {
            offset += length
        }
    }

    fun requireFullyConsumed(context: String) {
        if (remaining != 0) {
            throw ProtocolException("$context left $remaining unread byte(s)")
        }
    }

    private fun requireAvailable(count: Int) {
        if (remaining < count) {
            throw ProtocolException("Need $count byte(s), only $remaining available")
        }
    }
}