package de.dh.pump.dana.commands

import de.dh.pump.client.singleframe.PumpResponse
import de.dh.pump.PumpStatus

interface DanaRsResponse : PumpResponse

/**
 * Result returned by DanaRS write/control packets that acknowledge success with a one-byte code.
 */
data class DanaRsAckResponse(
    override val status: PumpStatus,
    val resultCode: Int,
) : DanaRsResponse

/**
 * Raw decoded DanaRS response.
 *
 * This keeps the transport/protocol layer useful before every command-specific parser exists. Callers
 * can still inspect the packet definition, result code, and payload bytes, while later iterations can
 * replace selected raw responses with richer typed models command by command.
 */
data class DanaRsRawResponse(
    override val status: PumpStatus,
    val definition: DanaRsPacketDefinition,
    val resultCode: Int?,
    val payload: ByteArray,
) : DanaRsResponse {
    override fun equals(other: Any?): Boolean {
        return other is DanaRsRawResponse &&
            status == other.status &&
            definition == other.definition &&
            resultCode == other.resultCode &&
            payload.contentEquals(other.payload)
    }

    override fun hashCode(): Int {
        var result = status.hashCode()
        result = 31 * result + definition.hashCode()
        result = 31 * result + (resultCode ?: 0)
        result = 31 * result + payload.contentHashCode()
        return result
    }

    companion object {
        fun read(definition: DanaRsPacketDefinition, payload: ByteArray): DanaRsRawResponse {
            return DanaRsRawResponse(
                status = PumpStatus.OK,
                definition = definition,
                resultCode = null,
                payload = payload,
            )
        }
    }
}