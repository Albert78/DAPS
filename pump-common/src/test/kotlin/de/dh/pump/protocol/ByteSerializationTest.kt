package de.dh.pump.protocol

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for the low-level byte manipulation utilities.
 *
 * These utilities ensure that insulin units and other multi-byte values are
 * correctly encoded/decoded regardless of the Android device's endianness.
 */
class ByteSerializationTest {

    @Test
    fun writerEncodesCorrectEndianness() {
        val writer = ByteWriter()
        writer.writeUInt8(0x01)
        writer.writeUInt16Le(0x1234) // 0x34 0x12
        writer.writeUInt16Be(0x5678) // 0x56 0x78
        
        val expected = byteArrayOf(0x01, 0x34, 0x12, 0x56, 0x78)
        assertArrayEquals(expected, writer.toByteArray())
    }

    @Test
    fun readerDecodesCorrectEndianness() {
        val bytes = byteArrayOf(0x01, 0x34, 0x12, 0x56, 0x78)
        val reader = ByteReader(bytes)
        
        assertEquals(0x01, reader.readUInt8())
        assertEquals(0x1234, reader.readUInt16Le())
        assertEquals(0x5678, reader.readUInt16Be())
        assertEquals(0, reader.remaining)
    }

    @Test(expected = ProtocolException::class)
    fun readerThrowsOnUnderflow() {
        val reader = ByteReader(byteArrayOf(0x01))
        reader.readUInt16Le() // Should throw
    }
}