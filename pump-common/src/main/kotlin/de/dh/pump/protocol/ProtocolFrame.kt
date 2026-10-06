package de.dh.pump.protocol

/**
 * Generic request/response envelope used by the common client.
 *
 * Real pump modules may map this model to a very different on-wire packet format. The important
 * contract for [PumpClient][de.dh.pump.PumpClient] is that the decoded frame exposes a
 * sequence number and command id so a notification can be correlated with the command that caused it.
 */
data class ProtocolFrame(
    val sequence: Int,
    val commandId: CommandId,
    val flags: Int,
    val payload: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        return other is ProtocolFrame &&
            sequence == other.sequence &&
            commandId == other.commandId &&
            flags == other.flags &&
            payload.contentEquals(other.payload)
    }

    override fun hashCode(): Int {
        var result = sequence
        result = 31 * result + commandId.hashCode()
        result = 31 * result + flags
        result = 31 * result + payload.contentHashCode()
        return result
    }
}

/**
 * Small default frame codec for tests and non-Dana prototypes.
 *
 * Dana-specific modules should provide their own [PumpProtocolCodec] implementation once the
 * verified DanaRS/Dana-i packet framing, encryption, and checksum rules are available.
 */
object FrameCodec : PumpProtocolCodec {
    private const val MAGIC_0 = 0x50
    private const val MAGIC_1 = 0x42
    private const val VERSION = 1
    private const val HEADER_SIZE = 10
    private const val CRC_SIZE = 2
    private const val MAX_PAYLOAD_SIZE = 512

    override fun encode(frame: ProtocolFrame): ByteArray {
        require(frame.sequence in 0..0xffff) { "sequence must fit UInt16" }
        require(frame.flags in 0..0xff) { "flags must fit UInt8" }
        require(frame.payload.size <= MAX_PAYLOAD_SIZE) { "payload too large: ${frame.payload.size}" }

        val withoutCrc = ByteWriter()
            .writeUInt8(MAGIC_0)
            .writeUInt8(MAGIC_1)
            .writeUInt8(VERSION)
            .writeUInt8(frame.flags)
            .writeUInt16Le(frame.sequence)
            .writeUInt16Le(frame.commandId.value)
            .writeUInt16Le(frame.payload.size)
            .writeBytes(frame.payload)
            .toByteArray()

        val crc = Crc16.ccittFalse(withoutCrc)
        return ByteWriter()
            .writeBytes(withoutCrc)
            .writeUInt16Le(crc)
            .toByteArray()
    }

    override fun decode(bytes: ByteArray): ProtocolFrame {
        if (bytes.size < HEADER_SIZE + CRC_SIZE) {
            throw ProtocolException("Frame too short: ${bytes.size}")
        }

        val expectedCrc = ByteReader(bytes.copyOfRange(bytes.size - CRC_SIZE, bytes.size)).readUInt16Le()
        val withoutCrc = bytes.copyOfRange(0, bytes.size - CRC_SIZE)
        val actualCrc = Crc16.ccittFalse(withoutCrc)
        if (expectedCrc != actualCrc) {
            throw ProtocolException("CRC mismatch: expected $expectedCrc, actual $actualCrc")
        }

        val reader = ByteReader(withoutCrc)
        val magic0 = reader.readUInt8()
        val magic1 = reader.readUInt8()
        if (magic0 != MAGIC_0 || magic1 != MAGIC_1) {
            throw ProtocolException("Invalid frame magic")
        }

        val version = reader.readUInt8()
        if (version != VERSION) {
            throw ProtocolException("Unsupported frame version: $version")
        }

        val flags = reader.readUInt8()
        val sequence = reader.readUInt16Le()
        val commandId = CommandId(reader.readUInt16Le())
        val payloadSize = reader.readUInt16Le()
        if (payloadSize > MAX_PAYLOAD_SIZE) {
            throw ProtocolException("Payload too large: $payloadSize")
        }

        val payload = reader.readBytes(payloadSize)
        reader.requireFullyConsumed("Frame")
        return ProtocolFrame(sequence, commandId, flags, payload)
    }
}