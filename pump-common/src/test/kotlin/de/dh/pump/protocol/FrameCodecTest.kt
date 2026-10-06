package de.dh.pump.protocol

import org.junit.Assert
import org.junit.Test

class FrameCodecTest {
    @Test
    fun frameRoundTripPreservesFields() {
        val frame = ProtocolFrame(
            sequence = 42,
            commandId = CommandId(0x0101),
            flags = 0,
            payload = byteArrayOf(0x34, 0x12),
        )

        val decoded = FrameCodec.decode(FrameCodec.encode(frame))

        Assert.assertEquals(frame.sequence, decoded.sequence)
        Assert.assertEquals(frame.commandId, decoded.commandId)
        Assert.assertEquals(frame.flags, decoded.flags)
        Assert.assertArrayEquals(frame.payload, decoded.payload)
    }

    @Test(expected = ProtocolException::class)
    fun corruptedFrameFailsCrcCheck() {
        val bytes = FrameCodec.encode(
            ProtocolFrame(
                sequence = 1,
                commandId = CommandId(0x0001),
                flags = 0,
                payload = byteArrayOf(0x00),
            ),
        )
        bytes[bytes.lastIndex - 1] = (bytes[bytes.lastIndex - 1].toInt() xor 0x01).toByte()

        FrameCodec.decode(bytes)
    }
}