package de.dh.pump.omnipod.protocol.security

import de.dh.pump.omnipod.protocol.transport.Id
import de.dh.pump.omnipod.protocol.transport.MessageIO
import de.dh.pump.omnipod.protocol.transport.MessagePacket
import de.dh.pump.omnipod.protocol.transport.MessageSendSuccess
import de.dh.pump.omnipod.protocol.transport.StringLengthPrefixEncoding.Companion.parseKeys
import de.dh.pump.protocol.hexToByteArray

class LTKExchanger(
    private val msgIO: MessageIO,
    private val myId: Id,
    private val podId: Id
) {

    private val podAddress = Id.fromLong(POD_ID_NOT_ACTIVATED)
    private val keyExchange = KeyExchange()
    private var seq: Byte = 1

    fun negotiateLTK(): PairResult {
        val sp1sp2 = PairMessage(
            sequenceNumber = seq,
            source = myId,
            destination = podAddress,
            keys = arrayOf(SP1, SP2),
            payloads = arrayOf(podId.address, sp2())
        )
        throwOnSendError(sp1sp2.messagePacket, SP1 + SP2)

        seq++
        val sps1 = PairMessage(
            sequenceNumber = seq,
            source = myId,
            destination = podAddress,
            keys = arrayOf(SPS1),
            payloads = arrayOf(keyExchange.pdmPublic + keyExchange.pdmNonce)
        )
        throwOnSendError(sps1.messagePacket, SPS1)

        val podSps1 = msgIO.receiveMessage() ?: throw IllegalStateException("Could not read SPS1")
        processSps1FromPod(podSps1)

        seq++
        val sps2 = PairMessage(
            sequenceNumber = seq,
            source = myId,
            destination = podAddress,
            keys = arrayOf(SPS2),
            payloads = arrayOf(keyExchange.pdmConf)
        )
        throwOnSendError(sps2.messagePacket, SPS2)

        val podSps2 = msgIO.receiveMessage() ?: throw IllegalStateException("Could not read SPS2")
        validatePodSps2(podSps2)

        seq++
        val sp0gp0 = PairMessage(
            sequenceNumber = seq,
            source = myId,
            destination = podAddress,
            keys = arrayOf(SP0GP0),
            payloads = arrayOf(ByteArray(0))
        )
        msgIO.sendMessage(sp0gp0.messagePacket)
        msgIO.receiveMessage()?.let { validateP0(it) }

        return PairResult(
            ltk = keyExchange.ltk,
            msgSeq = seq
        )
    }

    private fun throwOnSendError(msg: MessagePacket, msgType: String) {
        val result = msgIO.sendMessage(msg)
        if (result !is MessageSendSuccess) {
            throw IllegalStateException("Could not send or confirm $msgType: $result")
        }
    }

    private fun processSps1FromPod(msg: MessagePacket) {
        val payload = parseKeys(arrayOf(SPS1), msg.payload)[0]
        keyExchange.updatePodPublicData(payload)
    }

    private fun validatePodSps2(msg: MessagePacket) {
        val payload = parseKeys(arrayOf(SPS2), msg.payload)[0]
        require(payload.size == KeyExchange.CMAC_SIZE) { "Invalid payload size for SPS2" }
        keyExchange.validatePodConf(payload)
    }

    private fun sp2(): ByteArray = GET_POD_STATUS_HEX_COMMAND.hexToByteArray()

    private fun validateP0(msg: MessagePacket) {
        val payload = parseKeys(arrayOf(P0), msg.payload)[0]
        if (!payload.contentEquals(UNKNOWN_P0_PAYLOAD)) {
            // Ignored or logged
        }
    }

    companion object {
        private const val POD_ID_NOT_ACTIVATED = 0xFFFFFFFFL
        private const val GET_POD_STATUS_HEX_COMMAND = "ffc32dbd08030e0100008a"
        private const val SP1 = "SP1="
        private const val SP2 = ",SP2="
        private const val SPS1 = "SPS1="
        private const val SPS2 = "SPS2="
        private const val SP0GP0 = "SP0,GP0"
        private const val P0 = "P0="
        private val UNKNOWN_P0_PAYLOAD = byteArrayOf(0xa5.toByte())
    }
}