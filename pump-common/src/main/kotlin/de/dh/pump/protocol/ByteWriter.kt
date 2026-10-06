package de.dh.pump.protocol

class ByteWriter {
    private val bytes = ArrayList<Byte>()

    fun writeUInt8(value: Int): ByteWriter = apply {
        require(value in 0..0xff) { "UInt8 out of range: $value" }
        bytes += value.toByte()
    }

    fun writeUInt16Le(value: Int): ByteWriter = apply {
        require(value in 0..0xffff) { "UInt16 out of range: $value" }
        writeUInt8(value and 0xff)
        writeUInt8((value ushr 8) and 0xff)
    }

    fun writeUInt16Be(value: Int): ByteWriter = apply {
        require(value in 0..0xffff) { "UInt16 out of range: $value" }
        writeUInt8((value ushr 8) and 0xff)
        writeUInt8(value and 0xff)
    }

    fun writeUInt32Le(value: Long): ByteWriter = apply {
        require(value in 0..0xffff_ffffL) { "UInt32 out of range: $value" }
        writeUInt8((value and 0xff).toInt())
        writeUInt8(((value ushr 8) and 0xff).toInt())
        writeUInt8(((value ushr 16) and 0xff).toInt())
        writeUInt8(((value ushr 24) and 0xff).toInt())
    }

    fun writeBytes(value: ByteArray): ByteWriter = apply {
        value.forEach { bytes += it }
    }

    fun toByteArray(): ByteArray = ByteArray(bytes.size) { index -> bytes[index] }
}