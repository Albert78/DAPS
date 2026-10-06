package de.dh.pump.dana.commands

import de.dh.pump.client.singleframe.CommandKind
import de.dh.pump.dana.protocol.DanaRsBleEncryption

/**
 * Describes whether a packet is initiated by the app or delivered asynchronously by the pump.
 */
enum class DanaRsPacketDirection {
    REQUEST_RESPONSE,
    NOTIFY,
}

/**
 * Captures the coarse response layout used by the generic DanaRS command wrapper.
 *
 * Many read commands expose command-specific data that is intentionally kept raw at this layer.
 * Write/control commands usually return a one-byte result code, while notify packets are matched by
 * packet type and opcode but are not expected as direct command responses.
 */
enum class DanaRsResponseShape {
    RAW,
    ACK_RESULT,
    NOTIFY,
}

/**
 * Metadata for one DanaRS packet implementation.
 *
 * The source class name is kept deliberately: it gives future protocol work a direct breadcrumb back
 * to the simplified reference copy when a payload parser needs to be implemented or verified.
 */
data class DanaRsPacketDefinition(
    val sourceClassName: String,
    val friendlyName: String,
    val opcode: Int,
    val kind: CommandKind,
    val direction: DanaRsPacketDirection = DanaRsPacketDirection.REQUEST_RESPONSE,
    val responseShape: DanaRsResponseShape = DanaRsResponseShape.RAW,
    val responseType: Int = DanaRsBleEncryption.DANAR_PACKET__TYPE_RESPONSE,
)