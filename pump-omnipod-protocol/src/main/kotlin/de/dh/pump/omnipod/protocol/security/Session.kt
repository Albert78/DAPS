package de.dh.pump.omnipod.protocol.security

import de.dh.pump.omnipod.protocol.transport.Id
import de.dh.pump.omnipod.protocol.transport.MessageIO
import de.dh.pump.omnipod.protocol.transport.MessagePacket
import de.dh.pump.omnipod.protocol.transport.MessageSendErrorConfirming
import de.dh.pump.omnipod.protocol.transport.MessageSendErrorSending
import de.dh.pump.omnipod.protocol.transport.MessageSendSuccess
import de.dh.pump.omnipod.protocol.transport.MessageType
import de.dh.pump.omnipod.protocol.transport.StringLengthPrefixEncoding
import de.dh.pump.omnipod.protocol.transport.StringLengthPrefixEncoding.Companion.parseKeys

sealed class CommandSendResult
object CommandSendSuccess : CommandSendResult()
data class CommandSendErrorSending(val msg: String) : CommandSendResult()
data class CommandSendErrorConfirming(val msg: String) : CommandSendResult()

sealed class CommandReceiveResult
data class CommandReceiveSuccess(val payload: ByteArray) : CommandReceiveResult() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as CommandReceiveSuccess
        return payload.contentEquals(other.payload)
    }

    override fun hashCode(): Int = payload.contentHashCode()
}
data class CommandReceiveError(val msg: String) : CommandReceiveResult()
data class CommandAckError(val payload: ByteArray, val msg: String) : CommandReceiveResult() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as CommandAckError
        if (!payload.contentEquals(other.payload)) return false
        if (msg != other.msg) return false
        return true
    }

    override fun hashCode(): Int {
        var result = payload.contentHashCode()
        result = 31 * result + msg.hashCode()
        return result
    }
}

class Session(
    private val msgIO: MessageIO,
    private val myId: Id,
    private val podId: Id,
    val sessionKeys: SessionKeys,
    val enDecrypt: EnDecrypt
) {

    fun sendCommand(commandBytes: ByteArray): CommandSendResult {
        sessionKeys.msgSequenceNumber++
        val msg = getCmdMessage(commandBytes)

        for (i in 0..MAX_TRIES) {
            when (val sendResult = msgIO.sendMessage(msg)) {
                is MessageSendSuccess -> return CommandSendSuccess
                is MessageSendErrorConfirming -> return CommandSendErrorConfirming(sendResult.msg)
                is MessageSendErrorSending -> { /* retry */ }
            }
        }
        return CommandSendErrorSending("Maximum tries reached. Could not send command.")
    }

    fun readAndAckResponse(): CommandReceiveResult {
        var responseMsgPacket: MessagePacket? = null
        for (i in 0..MAX_TRIES) {
            val responseMsg = msgIO.receiveMessage()
            if (responseMsg != null) {
                responseMsgPacket = responseMsg
                break
            }
        }

        responseMsgPacket ?: return CommandReceiveError("Could not read response")

        val decrypted = enDecrypt.decrypt(responseMsgPacket)
        val payloadData = parseResponsePayload(decrypted)

        sessionKeys.msgSequenceNumber++
        val ack = getAck(responseMsgPacket)
        val sendResult = msgIO.sendMessage(ack)

        if (sendResult !is MessageSendSuccess) {
            return CommandAckError(payloadData, "Could not ACK response: $sendResult")
        }
        return CommandReceiveSuccess(payloadData)
    }

    private fun parseResponsePayload(decrypted: MessagePacket): ByteArray {
        val data = parseKeys(arrayOf(RESPONSE_PREFIX), decrypted.payload)[0]
        require(data.size >= 8) { "Response payload too short" }
        return data.copyOfRange(6, data.size - 2)
    }

    private fun getAck(response: MessagePacket): MessagePacket {
        val msg = MessagePacket(
            type = MessageType.ENCRYPTED,
            sequenceNumber = sessionKeys.msgSequenceNumber,
            source = myId,
            destination = podId,
            payload = ByteArray(0),
            eqos = 0,
            ack = true,
            ackNumber = response.sequenceNumber.inc()
        )
        return enDecrypt.encrypt(msg)
    }

    private fun getCmdMessage(commandBytes: ByteArray): MessagePacket {
        val wrapped = StringLengthPrefixEncoding.formatKeys(
            arrayOf(COMMAND_PREFIX, COMMAND_SUFFIX),
            arrayOf(commandBytes, ByteArray(0))
        )
        val msg = MessagePacket(
            type = MessageType.ENCRYPTED,
            sequenceNumber = sessionKeys.msgSequenceNumber,
            source = myId,
            destination = podId,
            payload = wrapped,
            eqos = 1
        )
        return enDecrypt.encrypt(msg)
    }

    companion object {
        private const val COMMAND_PREFIX = "S0.0="
        private const val COMMAND_SUFFIX = ",G0.0"
        private const val RESPONSE_PREFIX = "0.0="
        private const val MAX_TRIES = 4
    }
}