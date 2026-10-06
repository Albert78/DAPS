package de.dh.pump.dana

import de.dh.pump.client.singleframe.PumpProtocolCodec

/**
 * Marker interface for the real DanaRS/Dana-i packet codec.
 *
 * Implementations should cover the complete Dana packet envelope, including framing, checksum,
 * sequence/session fields, and encryption or authentication steps required by the pump generation.
 */
interface DanaPacketCodec : PumpProtocolCodec