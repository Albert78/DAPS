package de.dh.pump.omnipod.protocol.security

import de.dh.pump.omnipod.protocol.transport.Id
import de.dh.pump.omnipod.protocol.transport.MessagePacket
import de.dh.pump.omnipod.protocol.transport.MessageType
import de.dh.pump.omnipod.protocol.transport.StringLengthPrefixEncoding

class PairMessage(
    val sequenceNumber: Byte,
    val source: Id,
    val destination: Id,
    keys: Array<String>,
    payloads: Array<ByteArray>,
    val messagePacket: MessagePacket = MessagePacket(
        type = MessageType.PAIRING,
        source = source,
        destination = destination,
        payload = StringLengthPrefixEncoding.formatKeys(keys, payloads),
        sequenceNumber = sequenceNumber,
        sas = true
    )
)