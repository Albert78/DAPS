package de.dh.pump.dana.protocol

import de.dh.pump.protocol.ProtocolFrame
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class DanaRsPacketCodecTest {
    @Test
    fun codecReassemblesFragmentedPackets() {
        val codec = DanaRsPacketCodec()
        val bytes = codec.encodePumpCheck("ABCDEFGHIJ")

        val first = codec.decodeFrames(bytes.copyOfRange(0, 5))
        val second = codec.decodeFrames(bytes.copyOfRange(5, bytes.size))

        assertEquals(emptyList<ProtocolFrame>(), first)
        assertEquals(1, second.size)
        assertEquals(DanaRsBleEncryption.DANAR_PACKET__TYPE_ENCRYPTION_REQUEST.toInt(), second.single().flags)
        assertEquals(DanaRsBleEncryption.DANAR_PACKET__OPCODE_ENCRYPTION__PUMP_CHECK, second.single().commandId.value)
        assertArrayEquals("ABCDEFGHIJ".toByteArray(), second.single().payload)
    }
}