package de.dh.pump.protocol

object Crc16 {
    fun ccittFalse(bytes: ByteArray): Int {
        var crc = 0xffff
        for (byte in bytes) {
            crc = crc xor ((byte.toInt() and 0xff) shl 8)
            repeat(8) {
                crc = if ((crc and 0x8000) != 0) {
                    (crc shl 1) xor 0x1021
                } else {
                    crc shl 1
                }
                crc = crc and 0xffff
            }
        }
        return crc
    }
}