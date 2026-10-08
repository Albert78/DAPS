package de.dh.pump.omnipod.protocol.transport

import java.nio.ByteBuffer
import java.util.LinkedList

class PayloadJoiner(firstPacket: ByteArray) {

    var oneExtraPacket: Boolean
    val fullFragments: Int
    var crc: Long = 0
    private var expectedIndex = 0
    private val fragments: MutableList<BlePacket> = LinkedList()

    init {
        val parsedFirstPacket = FirstBlePacket.parse(firstPacket)
        fragments.add(parsedFirstPacket)
        fullFragments = parsedFirstPacket.fullFragments
        crc = parsedFirstPacket.crc32 ?: 0
        oneExtraPacket = parsedFirstPacket.oneExtraPacket
    }

    fun accumulate(packet: ByteArray) {
        require(packet.size >= 3) { "Packet too short" }
        val idx = packet[0].toInt()
        require(idx == expectedIndex + 1) { "Unexpected packet index: $idx, expected: ${expectedIndex + 1}" }

        expectedIndex++
        when {
            idx < fullFragments -> {
                fragments.add(MiddleBlePacket.parse(packet))
            }
            idx == fullFragments -> {
                val lastPacket = LastBlePacket.parse(packet)
                fragments.add(lastPacket)
                crc = lastPacket.crc32
                oneExtraPacket = lastPacket.oneExtraPacket
            }
            idx == fullFragments + 1 && oneExtraPacket -> {
                fragments.add(LastOptionalPlusOneBlePacket.parse(packet))
            }
            else -> {
                throw IllegalArgumentException("Packet index out of bounds: $idx")
            }
        }
    }

    fun finalize(): ByteArray {
        val payloads = fragments.map { it.payload }
        val totalLen = payloads.fold(0) { acc, elem -> acc + elem.size }
        val bb = ByteBuffer.allocate(totalLen)
        payloads.forEach { p -> bb.put(p) }
        bb.flip()
        val bytes = bb.array()
        val computedCrc = bytes.crc32()
        require(computedCrc == crc) { "CRC mismatch. Computed: $computedCrc, expected: $crc" }
        return bytes
    }
}