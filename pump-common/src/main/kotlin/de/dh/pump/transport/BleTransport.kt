package de.dh.pump.transport

import kotlinx.coroutines.channels.Channel
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Minimal transport contract required by [de.dh.pump.PumpClient].
 *
 * Implementations expose raw notification payloads and provide serialized writes to the device. The
 * transport does not parse pump packets; that responsibility belongs to a [de.dh.pump.protocol.PumpProtocolCodec].
 */
interface BleTransport {
    val notifications: Channel<ByteArray>

    suspend fun write(bytes: ByteArray)
    suspend fun close(timeout: Duration = 10.seconds)
}