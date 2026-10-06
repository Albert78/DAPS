package de.dh.pump.dana.commands

import de.dh.pump.client.singleframe.CommandId
import de.dh.pump.client.singleframe.PumpCommand
import de.dh.pump.dana.DanaPumpStatus
import de.dh.pump.protocol.ByteReader
import de.dh.pump.protocol.ByteWriter

/**
 * Base class for one concrete DanaRS packet command.
 *
 * Each subclass owns the request layout and the response parser for one opcode. The shared base only
 * maps packet metadata into the generic PumpCommon command contract.
 */
abstract class DanaRsPacketCommand<R : DanaRsResponse>(
    val definition: DanaRsPacketDefinition,
) : PumpCommand<R> {
    override val commandId: CommandId = CommandId(definition.opcode)
    override val name: String = definition.friendlyName
    override val kind = definition.kind

    override fun encodePayload(writer: ByteWriter) {
        // no parameters
    }
}

/**
 * Base class for commands whose response has not yet been promoted to a richer domain model.
 */
abstract class DanaRsRawPacketCommand(
    definition: DanaRsPacketDefinition,
) : DanaRsPacketCommand<DanaRsRawResponse>(definition) {
    override fun decodePayload(reader: ByteReader): DanaRsRawResponse {
        return DanaRsRawResponse.read(definition, reader.readBytes(reader.remaining))
    }
}

/**
 * Base class for commands that return the standard one-byte DanaRS result code.
 */
abstract class DanaRsAckPacketCommand(
    definition: DanaRsPacketDefinition,
) : DanaRsPacketCommand<DanaRsAckResponse>(definition) {
    override fun decodePayload(reader: ByteReader): DanaRsAckResponse {
        val resultCode = reader.readUInt8()
        return DanaRsAckResponse(
            status = DanaPumpStatus.fromCode(resultCode),
            resultCode = resultCode,
        )
    }
}