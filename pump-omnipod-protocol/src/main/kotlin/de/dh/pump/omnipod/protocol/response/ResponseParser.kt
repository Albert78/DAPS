package de.dh.pump.omnipod.protocol.response

import de.dh.pump.omnipod.protocol.definition.byValue

class CouldNotParseResponseException(message: String) : Exception(message)

object ResponseParser {

    @Throws(CouldNotParseResponseException::class)
    fun parseResponse(payload: ByteArray): Response {
        if (payload.isEmpty()) {
            throw CouldNotParseResponseException("Empty payload")
        }
        return when (byValue(payload[0], ResponseType.UNKNOWN)) {
            ResponseType.ACTIVATION_RESPONSE -> parseActivationResponse(payload)
            ResponseType.DEFAULT_STATUS_RESPONSE -> DefaultStatusResponse(payload)
            ResponseType.ADDITIONAL_STATUS_RESPONSE -> parseAdditionalStatusResponse(payload)
            ResponseType.NAK_RESPONSE -> NakResponse(payload)
            ResponseType.UNKNOWN -> throw CouldNotParseResponseException("Unrecognized message type: ${payload[0]}")
        }
    }

    private fun parseActivationResponse(payload: ByteArray): Response {
        if (payload.size < 2) throw CouldNotParseResponseException("Payload too short for activation response")
        return when (byValue(payload[1], ResponseType.ActivationResponseType.UNKNOWN)) {
            ResponseType.ActivationResponseType.GET_VERSION_RESPONSE -> VersionResponse(payload)
            ResponseType.ActivationResponseType.SET_UNIQUE_ID_RESPONSE -> SetUniqueIdResponse(payload)
            ResponseType.ActivationResponseType.UNKNOWN -> throw CouldNotParseResponseException("Unrecognized activation response type: ${payload[1]}")
        }
    }

    private fun parseAdditionalStatusResponse(payload: ByteArray): Response {
        if (payload.size < 3) throw CouldNotParseResponseException("Payload too short for additional status response")
        return when (byValue(payload[2], ResponseType.StatusResponseType.UNKNOWN)) {
            ResponseType.StatusResponseType.DEFAULT_STATUS_RESPONSE -> DefaultStatusResponse(payload)
            ResponseType.StatusResponseType.ALARM_STATUS -> AlarmStatusResponse(payload)
            ResponseType.StatusResponseType.STATUS_RESPONSE_PAGE_1,
            ResponseType.StatusResponseType.UNKNOWN -> throw CouldNotParseResponseException("Unrecognized additional status response type: ${payload[2]}")
        }
    }
}