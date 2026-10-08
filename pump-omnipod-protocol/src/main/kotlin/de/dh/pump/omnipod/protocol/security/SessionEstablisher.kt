package de.dh.pump.omnipod.protocol.security

import de.dh.pump.omnipod.protocol.transport.Id
import de.dh.pump.omnipod.protocol.transport.MessageIO
import de.dh.pump.omnipod.protocol.transport.MessagePacket
import de.dh.pump.omnipod.protocol.transport.MessageSendSuccess
import de.dh.pump.omnipod.protocol.transport.MessageType
import de.dh.pump.protocol.toHex
import java.security.SecureRandom

class SessionEstablisher(
    private val msgIO: MessageIO,
    private val ltk: ByteArray,
    private val eapSqn: ByteArray,
    private val myId: Id,
    private val podId: Id,
    private var msgSeq: Byte
) {

    private val controllerIV = ByteArray(IV_SIZE)
    private var nodeIV = ByteArray(IV_SIZE)
    private val identifier = SecureRandom().nextInt().toByte()
    private val milenage = Milenage(k = ltk, sqn = eapSqn)

    init {
        require(eapSqn.size == 6) { "EAP-SQN has to be 6 bytes long" }
        require(ltk.size == 16) { "LTK has to be 16 bytes long" }
        SecureRandom().nextBytes(controllerIV)
    }

    fun negotiateSessionKeys(): SessionNegotiationResponse {
        msgSeq++
        val challenge = eapAkaChallenge()
        val sendResult = msgIO.sendMessage(challenge)
        if (sendResult !is MessageSendSuccess) {
            throw IllegalStateException("Could not send EAP AKA challenge: $sendResult")
        }

        val challengeResponse = msgIO.receiveMessage()
            ?: throw IllegalStateException("Could not establish session: response null")

        val newSqn = processChallengeResponse(challengeResponse)
        if (newSqn != null) {
            return SessionNegotiationResynchronization(
                synchronizedEapSqn = newSqn,
                msgSequenceNumber = msgSeq
            )
        }

        msgSeq++
        val success = eapSuccess()
        msgIO.sendMessage(success)

        return SessionKeys(
            ck = milenage.ck,
            nonce = Nonce(
                prefix = controllerIV + nodeIV,
                sqn = 0
            ),
            msgSequenceNumber = msgSeq
        )
    }

    private fun eapAkaChallenge(): MessagePacket {
        val attributes = arrayOf(
            EapAkaAttributeAutn(milenage.autn),
            EapAkaAttributeRand(milenage.rand),
            EapAkaAttributeCustomIV(controllerIV)
        )
        val eapMsg = EapMessage(
            code = EapCode.REQUEST,
            identifier = identifier,
            attributes = attributes
        )
        return MessagePacket(
            type = MessageType.SESSION_ESTABLISHMENT,
            sequenceNumber = msgSeq,
            source = myId,
            destination = podId,
            payload = eapMsg.toByteArray()
        )
    }

    private fun processChallengeResponse(challengeResponse: MessagePacket): EapSqn? {
        val eapMsg = EapMessage.parse(challengeResponse.payload)
        require(eapMsg.identifier == identifier) { "EAP-AKA: incorrect identifier ${eapMsg.identifier}, expected: $identifier" }

        val eapSqn = isResynchronization(eapMsg)
        if (eapSqn != null) {
            return eapSqn
        }

        require(eapMsg.attributes.size == 2) { "Expecting two EAP attributes, got: ${eapMsg.attributes.size}" }

        for (attr in eapMsg.attributes) {
            when (attr) {
                is EapAkaAttributeRes -> {
                    require(milenage.res.contentEquals(attr.payload)) {
                        "RES mismatch. Expected: ${milenage.res.toHex()}. Actual: ${attr.payload.toHex()}"
                    }
                }
                is EapAkaAttributeCustomIV -> {
                    nodeIV = attr.payload.copyOfRange(0, IV_SIZE)
                }
                else -> throw IllegalStateException("Unknown attribute received: $attr")
            }
        }
        return null
    }

    private fun isResynchronization(eapMsg: EapMessage): EapSqn? {
        if (eapMsg.subType != EapMessage.SUBTYPE_SYNCHRONIZATION_FAILURE ||
            eapMsg.attributes.size != 1 ||
            eapMsg.attributes[0] !is EapAkaAttributeAuts
        ) {
            return null
        }

        val auts = eapMsg.attributes[0] as EapAkaAttributeAuts
        val autsMilenage = Milenage(
            k = ltk,
            sqn = eapSqn,
            randParam = milenage.rand,
            auts = auts.payload
        )

        val newSqnMilenage = Milenage(
            k = ltk,
            sqn = autsMilenage.synchronizationSqn,
            randParam = milenage.rand,
            auts = auts.payload,
            amf = Milenage.RESYNC_AMF
        )

        require(newSqnMilenage.macS.contentEquals(newSqnMilenage.receivedMacS)) {
            "MacS mismatch. Expected: ${newSqnMilenage.macS.toHex()}. Received: ${newSqnMilenage.receivedMacS.toHex()}"
        }
        return EapSqn(autsMilenage.synchronizationSqn)
    }

    private fun eapSuccess(): MessagePacket {
        val eapMsg = EapMessage(
            code = EapCode.SUCCESS,
            attributes = arrayOf(),
            identifier = identifier
        )
        return MessagePacket(
            type = MessageType.SESSION_ESTABLISHMENT,
            sequenceNumber = msgSeq,
            source = myId,
            destination = podId,
            payload = eapMsg.toByteArray()
        )
    }

    companion object {
        private const val IV_SIZE = 4
    }
}