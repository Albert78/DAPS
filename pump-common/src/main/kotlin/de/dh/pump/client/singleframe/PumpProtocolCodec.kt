package de.dh.pump.client.singleframe

/**
 * Converts between the common single-frame model and the raw bytes exchanged over BLE.
 *
 * This interface is the protocol boundary between single-frame client orchestration and
 * pump-specific packet codecs.
 */
interface PumpProtocolCodec {
    fun encode(frame: ProtocolFrame): ByteArray
    fun decode(bytes: ByteArray): ProtocolFrame

    /**
     * Decodes all complete frames produced by this input chunk.
     *
     * Most simple protocols produce exactly one frame per notification. BLE pump protocols can be
     * fragmented across several notifications or coalesce more than one packet into a single
     * notification, so stateful codecs can override this method and emit zero, one, or many frames.
     */
    fun decodeFrames(bytes: ByteArray): List<ProtocolFrame> = listOf(decode(bytes))
}