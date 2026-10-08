package de.dh.pump.omnipod.protocol.transport

sealed class BleSendResult

object BleSendSuccess : BleSendResult()
data class BleSendErrorSending(val msg: String, val cause: Throwable? = null) : BleSendResult()
data class BleSendErrorConfirming(val msg: String, val cause: Throwable? = null) : BleSendResult()

sealed class BleConfirmResult

object BleConfirmSuccess : BleConfirmResult()
data class BleConfirmIncorrectData(val payload: ByteArray) : BleConfirmResult() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as BleConfirmIncorrectData
        return payload.contentEquals(other.payload)
    }

    override fun hashCode(): Int = payload.contentHashCode()
}
data class BleConfirmError(val msg: String) : BleConfirmResult()