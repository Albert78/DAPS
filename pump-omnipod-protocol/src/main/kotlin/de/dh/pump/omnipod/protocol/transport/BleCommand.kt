package de.dh.pump.omnipod.protocol.transport

import de.dh.pump.protocol.toHex
import java.nio.ByteBuffer

enum class BleCommandType(val value: Byte) {
    RTS(0x00.toByte()),
    CTS(0x01.toByte()),
    NACK(0x02.toByte()),
    ABORT(0x03.toByte()),
    SUCCESS(0x04.toByte()),
    FAIL(0x05.toByte()),
    HELLO(0x06.toByte()),
    INCORRECT(0x09.toByte());

    companion object {
        fun byValue(value: Byte): BleCommandType =
            entries.firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Unknown BleCommandType: $value")
    }
}

object BleCommandRTS : BleCommand(BleCommandType.RTS)
object BleCommandCTS : BleCommand(BleCommandType.CTS)
object BleCommandAbort : BleCommand(BleCommandType.ABORT)
object BleCommandSuccess : BleCommand(BleCommandType.SUCCESS)
object BleCommandFail : BleCommand(BleCommandType.FAIL)

data class BleCommandNack(val idx: Byte) : BleCommand(BleCommandType.NACK, byteArrayOf(idx)) {
    companion object {
        fun parse(payload: ByteArray): BleCommand {
            return when {
                payload.size < 2 -> BleCommandIncorrect("Incorrect NACK payload", payload)
                payload[0] != BleCommandType.NACK.value -> BleCommandIncorrect("Incorrect NACK header", payload)
                else -> BleCommandNack(payload[1])
            }
        }
    }
}

data class BleCommandHello(private val controllerId: Int) : BleCommand(
    BleCommandType.HELLO,
    ByteBuffer.allocate(6)
        .put(1.toByte())
        .put(4.toByte())
        .putInt(controllerId)
        .array()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false
        other as BleCommandHello
        return controllerId == other.controllerId
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + controllerId
        return result
    }
}

data class BleCommandIncorrect(val msg: String, val payload: ByteArray) : BleCommand(BleCommandType.INCORRECT) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false
        other as BleCommandIncorrect
        if (msg != other.msg) return false
        return payload.contentEquals(other.payload)
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + msg.hashCode()
        result = 31 * result + payload.contentHashCode()
        return result
    }
}

sealed class BleCommand(val data: ByteArray) {
    constructor(type: BleCommandType) : this(byteArrayOf(type.value))
    constructor(type: BleCommandType, payload: ByteArray) : this(byteArrayOf(type.value) + payload)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BleCommand) return false
        return data.contentEquals(other.data)
    }

    override fun toString(): String = "Raw command: [${data.toHex()}]"

    override fun hashCode(): Int = data.contentHashCode()

    companion object {
        fun parse(payload: ByteArray): BleCommand {
            if (payload.isEmpty()) {
                return BleCommandIncorrect("Incorrect command: empty payload", payload)
            }
            return try {
                when (BleCommandType.byValue(payload[0])) {
                    BleCommandType.RTS -> BleCommandRTS
                    BleCommandType.CTS -> BleCommandCTS
                    BleCommandType.NACK -> BleCommandNack.parse(payload)
                    BleCommandType.ABORT -> BleCommandAbort
                    BleCommandType.SUCCESS -> BleCommandSuccess
                    BleCommandType.FAIL -> BleCommandFail
                    BleCommandType.HELLO -> BleCommandIncorrect("Incorrect hello command received", payload)
                    BleCommandType.INCORRECT -> BleCommandIncorrect("Incorrect command received", payload)
                }
            } catch (e: IllegalArgumentException) {
                BleCommandIncorrect("Incorrect command payload", payload)
            }
        }
    }
}