package de.dh.pump.omnipod.protocol.security

import de.dh.pump.protocol.toHex
import java.nio.ByteBuffer

enum class EapCode(val code: Byte) {
    REQUEST(1),
    RESPONSE(2),
    SUCCESS(3),
    FAILURE(4);

    companion object {
        fun byValue(value: Byte): EapCode =
            entries.firstOrNull { it.code == value }
                ?: throw IllegalArgumentException("Unknown EAP code: $value")
    }
}

class EapMessage(
    val code: EapCode,
    val identifier: Byte,
    val subType: Byte = 0,
    val attributes: Array<EapAkaAttribute>
) {

    fun toByteArray(): ByteArray {
        val serializedAttributes = attributes.flatMap { it.toByteArray().asIterable() }
        val joinedAttributes = serializedAttributes.toByteArray()

        val attrSize = joinedAttributes.size
        if (attrSize == 0) {
            return byteArrayOf(code.code, identifier, 0, 4)
        }
        val totalSize = HEADER_SIZE + attrSize

        val bb = ByteBuffer
            .allocate(totalSize)
            .put(code.code)
            .put(identifier)
            .put(((totalSize ushr 8) and 0xFF).toByte())
            .put((totalSize and 0xFF).toByte())
            .put(AKA_PACKET_TYPE)
            .put(SUBTYPE_AKA_CHALLENGE)
            .put(byteArrayOf(0, 0))
            .put(joinedAttributes)

        return bb.array()
    }

    companion object {
        private const val HEADER_SIZE = 8
        private const val SUBTYPE_AKA_CHALLENGE = 1.toByte()
        const val SUBTYPE_SYNCHRONIZATION_FAILURE = 4.toByte()
        private const val AKA_PACKET_TYPE = 0x17.toByte()

        fun parse(payload: ByteArray): EapMessage {
            require(payload.size >= 4) { "Payload too short: ${payload.toHex()}" }

            val totalSize = (payload[2].toInt() and 0xFF shl 8) or (payload[3].toInt() and 0xFF)
            require(payload.size >= totalSize) { "Payload truncated: ${payload.toHex()}" }

            if (payload.size == 4) {
                return EapMessage(
                    code = EapCode.byValue(payload[0]),
                    identifier = payload[1],
                    attributes = arrayOf()
                )
            }
            if (totalSize > 0 && payload[4] != AKA_PACKET_TYPE) {
                throw IllegalArgumentException("Invalid EAP payload. Expected AKA packet type: ${payload.toHex()}")
            }
            val attributesPayload = payload.copyOfRange(8, totalSize)
            return EapMessage(
                code = EapCode.byValue(payload[0]),
                identifier = payload[1],
                attributes = EapAkaAttribute.parseAttributes(attributesPayload).toTypedArray(),
                subType = payload[5]
            )
        }
    }
}