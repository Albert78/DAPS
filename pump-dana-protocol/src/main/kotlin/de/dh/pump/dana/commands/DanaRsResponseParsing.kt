package de.dh.pump.dana.commands

import de.dh.pump.protocol.ByteReader
import de.dh.pump.protocol.ProtocolException
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.LocalDateTime

internal fun ByteReader.requireRemainingAtLeast(count: Int, context: String) {
    if (remaining < count) {
        throw ProtocolException("$context requires at least $count byte(s), only $remaining available")
    }
}

internal fun ByteReader.discardRemaining() {
    if (remaining > 0) {
        readBytes(remaining)
    }
}

internal fun ByteReader.readAscii(length: Int): String {
    return String(readBytes(length), StandardCharsets.US_ASCII).trimEnd('\u0000', ' ')
}

internal fun ByteReader.readDanaLocalDate(): LocalDate {
    val year = 2000 + readUInt8()
    val month = readUInt8()
    val day = readUInt8()
    return LocalDate.of(year, month, day)
}

internal fun ByteReader.readDanaLocalDateTime(): LocalDateTime {
    val year = 2000 + readUInt8()
    val month = readUInt8()
    val day = readUInt8()
    val hour = readUInt8()
    val minute = readUInt8()
    val second = readUInt8()
    return LocalDateTime.of(year, month, day, hour, minute, second)
}

internal fun ByteReader.readSignedInt8(): Int = readBytes(1)[0].toInt()

internal fun ByteReader.readUInt16Be(): Int {
    val hi = readUInt8()
    val lo = readUInt8()
    return (hi shl 8) or lo
}