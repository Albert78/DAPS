package de.dh.pump.client.singleframe

import de.dh.pump.PumpStatus
import de.dh.pump.protocol.ByteReader
import de.dh.pump.protocol.ByteWriter

/**
 * Describes the operational risk class of a command.
 *
 * UI code and policy layers can use this value to distinguish passive reads from writes and active
 * control commands. The protocol layer still treats all commands uniformly.
 */
enum class CommandKind {
    READ,
    WRITE,
    CONTROL,
    ABORT_CONTROL,
}

interface PumpResponse {
    val status: PumpStatus
}

data class AckResponse(
    override val status: PumpStatus,
) : PumpResponse

/**
 * A typed command owns both directions of its wire payload.
 *
 * Keeping encode and decode logic together avoids a central parser that must know every pump-specific
 * message layout. It also makes each command easy to verify with golden request/response byte streams.
 */
interface PumpCommand<R : PumpResponse> {
    val commandId: CommandId
    val name: String
    val kind: CommandKind

    fun encodePayload(writer: ByteWriter)
    fun decodePayload(reader: ByteReader): R
}

/**
 * A command whose response is delivered as several notifications.
 *
 * History transfers are the typical example: the pump sends zero or more record chunks followed by a
 * terminal marker. The stream command keeps the protocol-specific aggregation state, while
 * PumpCommon only handles request serialization and frame correlation.
 */
interface PumpStreamCommand<C : PumpResponse, R> {
    val commandId: CommandId
    val name: String
    val kind: CommandKind

    fun encodePayload(writer: ByteWriter)
    fun decodeChunk(reader: ByteReader): C
    fun onChunk(chunk: C)
    fun isComplete(chunk: C): Boolean
    fun result(): R
}