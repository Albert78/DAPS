package de.dh.pump.dana.commands

import de.dh.daps.common.model.data.PumpTimestamp
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

internal fun encodeDanaDateTime(date: ZonedDateTime): ByteArray {
    return byteArrayOf(
        (date.year - 2000 and 0xff).toByte(),
        (date.monthValue and 0xff).toByte(),
        (date.dayOfMonth and 0xff).toByte(),
        (date.hour and 0xff).toByte(),
        (date.minute and 0xff).toByte(),
        (date.second and 0xff).toByte(),
    )
}

internal fun encodeDanaDateTime(time: PumpTimestamp, zoneId: ZoneId = ZoneId.systemDefault()): ByteArray {
    return encodeDanaDateTime(Instant.ofEpochMilli(time.rawMs).atZone(zoneId))
}

internal fun encodeDanaUtcDateTime(time: PumpTimestamp): ByteArray {
    return encodeDanaDateTime(Instant.ofEpochMilli(time.rawMs).atZone(ZoneOffset.UTC))
}

internal fun encodeDanaHistoryStart(fromTime: PumpTimestamp, zoneId: ZoneId = ZoneId.systemDefault()): ByteArray {
    return if (fromTime.rawMs == 0L) {
        byteArrayOf(0, 1, 1, 0, 0, 0)
    } else {
        encodeDanaDateTime(fromTime, zoneId)
    }
}

internal fun le16(value: Int): ByteArray {
    return byteArrayOf((value and 0xff).toByte(), (value ushr 8 and 0xff).toByte())
}