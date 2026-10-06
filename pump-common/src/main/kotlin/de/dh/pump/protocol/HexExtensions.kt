package de.dh.pump.protocol

fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

fun String.hexToByteArray(): ByteArray {
    val result = ByteArray(length / 2)
    for (i in 0 until length step 2) {
        val firstDigit = Character.digit(this[i], 16)
        val secondDigit = Character.digit(this[i + 1], 16)
        result[i / 2] = ((firstDigit shl 4) + secondDigit).toByte()
    }
    return result
}