package de.dh.pump.dana.commands.history

import de.dh.pump.PumpStatus
import de.dh.pump.dana.DanaPumpStatus
import de.dh.pump.client.singleframe.PumpStreamCommand
import de.dh.pump.dana.commands.DanaRsPacketCommand
import de.dh.pump.dana.commands.DanaRsPacketDefinition
import de.dh.pump.dana.commands.DanaRsPacketRegistry
import de.dh.pump.dana.commands.discardRemaining
import de.dh.pump.dana.commands.encodeDanaHistoryStart
import de.dh.pump.dana.commands.requireRemainingAtLeast
import de.dh.pump.protocol.ByteReader
import de.dh.pump.protocol.ByteWriter
import de.dh.pump.protocol.ProtocolException
import de.dh.daps.common.model.data.PumpTimestamp
import java.time.DateTimeException
import java.time.LocalDateTime
import java.time.ZoneId

abstract class DanaRsHistoryCommand(
    definition: DanaRsPacketDefinition,
    private val fromTime: PumpTimestamp = PumpTimestamp.ZERO,
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) : DanaRsPacketCommand<DanaRsHistoryResponse>(definition),
    PumpStreamCommand<DanaRsHistoryResponse, DanaRsHistoryResult> {
    private val records = mutableListOf<DanaRsHistoryRecord>()
    private var end: HistoryEndResponse? = null

    override fun encodePayload(writer: ByteWriter) {
        writer.writeBytes(
            encodeDanaHistoryStart(
                fromTime,
                zoneId
            )
        )
    }

    override fun decodePayload(reader: ByteReader): DanaRsHistoryResponse {
        return decodeChunk(reader)
    }

    override fun decodeChunk(reader: ByteReader): DanaRsHistoryResponse {
        return when (reader.remaining) {
            1 -> decodeEnd(reader)
            3 -> decodeEndWithCount(reader)
            else -> decodeRecord(reader)
        }
    }

    override fun onChunk(chunk: DanaRsHistoryResponse) {
        when (chunk) {
            is HistoryRecordResponse -> records += chunk.record
            is HistoryEndResponse -> end = chunk
        }
    }

    override fun isComplete(chunk: DanaRsHistoryResponse): Boolean = chunk is HistoryEndResponse

    override fun result(): DanaRsHistoryResult {
        return DanaRsHistoryResult(
            records = records.toList(),
            end = end ?: HistoryEndResponse(
                status = PumpStatus.UNKNOWN,
                errorCode = DanaPumpStatus.UNKNOWN_CODE,
                totalCount = null,
            ),
        )
    }

    private fun decodeEnd(reader: ByteReader): HistoryEndResponse {
        val errorCode = reader.readUInt8()
        reader.discardRemaining()
        return HistoryEndResponse(
            status = DanaPumpStatus.fromCode(errorCode),
            errorCode = errorCode,
            totalCount = null,
        )
    }

    private fun decodeEndWithCount(reader: ByteReader): HistoryEndResponse {
        val errorCode = reader.readUInt8()
        val totalCount = reader.readUInt16Le()
        reader.discardRemaining()
        return HistoryEndResponse(
            status = DanaPumpStatus.fromCode(errorCode),
            errorCode = errorCode,
            totalCount = totalCount,
        )
    }

    private fun decodeRecord(reader: ByteReader): HistoryRecordResponse {
        reader.requireRemainingAtLeast(10, name)
        val recordCode = reader.readUInt8()
        val year = reader.readUInt8()
        val month = reader.readUInt8()
        val day = reader.readUInt8()
        val hourOrDailyBasalHi = reader.readUInt8()
        val minuteOrDailyBasalLo = reader.readUInt8()
        val secondOrDailyBolusHi = reader.readUInt8()
        val historyCodeOrDailyBolusLo = reader.readUInt8()
        val rawValue = reader.readUInt16Be()
        reader.discardRemaining()

        val kind = DanaRsHistoryRecordKind.fromWireValue(recordCode)
        val dailyBasal = ((hourOrDailyBasalHi shl 8) or minuteOrDailyBasalLo) * 0.01
        val dailyBolus = ((secondOrDailyBolusHi shl 8) or historyCodeOrDailyBolusLo) * 0.01
        val timestamp = historyTimestamp(
            kind = kind,
            year = year,
            month = month,
            day = day,
            hour = hourOrDailyBasalHi,
            minute = minuteOrDailyBasalLo,
            second = secondOrDailyBolusHi,
        )
        val state = when (kind) {
            DanaRsHistoryRecordKind.SUSPEND,
            DanaRsHistoryRecordKind.TEMP_BASAL -> {
                // Start event = 'O', stop event = 'F'
                if (historyCodeOrDailyBolusLo == 'O'.code) DanaRsHistoryOnOffState.ON else DanaRsHistoryOnOffState.OFF
            }
            else -> null
        }
        val durationMinutes = when (kind) {
            DanaRsHistoryRecordKind.BOLUS -> (historyCodeOrDailyBolusLo and 0x0f) * 60 + secondOrDailyBolusHi
            else -> null
        }
        val bolusType = if (kind == DanaRsHistoryRecordKind.BOLUS) {
            when (historyCodeOrDailyBolusLo and 0xf0) {
                0xa0 -> DanaRsHistoryBolusType.DUAL_STEP
                0xc0 -> DanaRsHistoryBolusType.EXTENDED
                0x80 -> DanaRsHistoryBolusType.STEP
                0x90 -> DanaRsHistoryBolusType.DUAL_EXTENDED
                else -> DanaRsHistoryBolusType.NONE
            }
        } else {
            null
        }
        val alarm = if (kind == DanaRsHistoryRecordKind.ALARM) alarmType(historyCodeOrDailyBolusLo) else null
        val value = when (kind) {
            DanaRsHistoryRecordKind.DAILY,
            DanaRsHistoryRecordKind.SUSPEND -> null

            DanaRsHistoryRecordKind.GLUCOSE,
            DanaRsHistoryRecordKind.CARBOHYDRATE -> rawValue.toDouble()

            DanaRsHistoryRecordKind.TEMP_BASAL -> {
                if (state == DanaRsHistoryOnOffState.ON) {
                    // Start event delivers percentage directly
                    rawValue.toDouble()
                } else {
                    // Stop event ('F') delivers reservoir level in 0.01 U
                    rawValue * 0.01
                }
            }

            DanaRsHistoryRecordKind.BOLUS,
            DanaRsHistoryRecordKind.PRIME,
            DanaRsHistoryRecordKind.REFILL,
            DanaRsHistoryRecordKind.ALARM,
            DanaRsHistoryRecordKind.BASAL_HOUR,
            DanaRsHistoryRecordKind.TEMP_BASAL_APS -> rawValue * 0.01

            DanaRsHistoryRecordKind.UNKNOWN -> null
        }
        return HistoryRecordResponse(
            status = PumpStatus.OK,
            record = DanaRsHistoryRecord(
                recordCode = recordCode,
                kind = kind,
                timestamp = PumpTimestamp(timestamp),
                value = value,
                durationMinutes = durationMinutes,
                bolusType = bolusType,
                alarm = alarm,
                state = state,
                dailyBasalUnits = if (kind == DanaRsHistoryRecordKind.DAILY) dailyBasal else null,
                dailyBolusUnits = if (kind == DanaRsHistoryRecordKind.DAILY) dailyBolus else null,
                rawHistoryCode = historyCodeOrDailyBolusLo,
                rawValue = rawValue,
            ),
        )
    }

    private fun historyTimestamp(
        kind: DanaRsHistoryRecordKind,
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
        second: Int,
    ): Long {
        return try {
            val local = when (kind) {
                DanaRsHistoryRecordKind.DAILY -> LocalDateTime.of(2000 + year, month, day, 0, 0)
                DanaRsHistoryRecordKind.BOLUS -> LocalDateTime.of(2000 + year, month, day, hour, minute)
                DanaRsHistoryRecordKind.PRIME,
                DanaRsHistoryRecordKind.REFILL,
                DanaRsHistoryRecordKind.GLUCOSE,
                DanaRsHistoryRecordKind.CARBOHYDRATE,
                DanaRsHistoryRecordKind.SUSPEND,
                DanaRsHistoryRecordKind.ALARM,
                DanaRsHistoryRecordKind.BASAL_HOUR,
                DanaRsHistoryRecordKind.TEMP_BASAL,
                DanaRsHistoryRecordKind.TEMP_BASAL_APS,
                DanaRsHistoryRecordKind.UNKNOWN -> LocalDateTime.of(2000 + year, month, day, hour, minute, second)
            }
            local.atZone(zoneId).toInstant().toEpochMilli()
        } catch (error: DateTimeException) {
            throw ProtocolException("$name contains an invalid history timestamp: ${error.message}")
        }
    }

    private fun alarmType(code: Int): DanaRsHistoryAlarmType =
        when (code) {
            'P'.code -> DanaRsHistoryAlarmType.BASAL_COMPARE
            'R'.code -> DanaRsHistoryAlarmType.EMPTY_RESERVOIR
            'C'.code -> DanaRsHistoryAlarmType.CHECK
            'O'.code -> DanaRsHistoryAlarmType.OCCLUSION
            'M'.code -> DanaRsHistoryAlarmType.BASAL_MAX
            'D'.code -> DanaRsHistoryAlarmType.DAILY_MAX
            'B'.code -> DanaRsHistoryAlarmType.LOW_BATTERY
            'S'.code -> DanaRsHistoryAlarmType.SHUTDOWN
            else -> DanaRsHistoryAlarmType.NONE
        }
}

class HistoryAlarmCommand(fromTime: PumpTimestamp = PumpTimestamp.ZERO, zoneId: ZoneId = ZoneId.systemDefault()) :
    DanaRsHistoryCommand(DanaRsPacketRegistry.HISTORY_ALARM, fromTime, zoneId)

class HistoryAllHistoryCommand(fromTime: PumpTimestamp = PumpTimestamp.ZERO, zoneId: ZoneId = ZoneId.systemDefault()) :
    DanaRsHistoryCommand(DanaRsPacketRegistry.HISTORY_ALL_HISTORY, fromTime, zoneId)

class HistoryBasalCommand(fromTime: PumpTimestamp = PumpTimestamp.ZERO, zoneId: ZoneId = ZoneId.systemDefault()) :
    DanaRsHistoryCommand(DanaRsPacketRegistry.HISTORY_BASAL, fromTime, zoneId)

class HistoryBloodGlucoseCommand(fromTime: PumpTimestamp = PumpTimestamp.ZERO, zoneId: ZoneId = ZoneId.systemDefault()) :
    DanaRsHistoryCommand(DanaRsPacketRegistry.HISTORY_BLOOD_GLUCOSE, fromTime, zoneId)

class HistoryBolusCommand(fromTime: PumpTimestamp = PumpTimestamp.ZERO, zoneId: ZoneId = ZoneId.systemDefault()) :
    DanaRsHistoryCommand(DanaRsPacketRegistry.HISTORY_BOLUS, fromTime, zoneId)

class HistoryCarbohydrateCommand(fromTime: PumpTimestamp = PumpTimestamp.ZERO, zoneId: ZoneId = ZoneId.systemDefault()) :
    DanaRsHistoryCommand(DanaRsPacketRegistry.HISTORY_CARBOHYDRATE, fromTime, zoneId)

class HistoryDailyCommand(fromTime: PumpTimestamp = PumpTimestamp.ZERO, zoneId: ZoneId = ZoneId.systemDefault()) :
    DanaRsHistoryCommand(DanaRsPacketRegistry.HISTORY_DAILY, fromTime, zoneId)

class HistoryPrimeCommand(fromTime: PumpTimestamp = PumpTimestamp.ZERO, zoneId: ZoneId = ZoneId.systemDefault()) :
    DanaRsHistoryCommand(DanaRsPacketRegistry.HISTORY_PRIME, fromTime, zoneId)

class HistoryRefillCommand(fromTime: PumpTimestamp = PumpTimestamp.ZERO, zoneId: ZoneId = ZoneId.systemDefault()) :
    DanaRsHistoryCommand(DanaRsPacketRegistry.HISTORY_REFILL, fromTime, zoneId)

class HistorySuspendCommand(fromTime: PumpTimestamp = PumpTimestamp.ZERO, zoneId: ZoneId = ZoneId.systemDefault()) :
    DanaRsHistoryCommand(DanaRsPacketRegistry.HISTORY_SUSPEND, fromTime, zoneId)

class HistoryTemporaryCommand(fromTime: PumpTimestamp = PumpTimestamp.ZERO, zoneId: ZoneId = ZoneId.systemDefault()) :
    DanaRsHistoryCommand(DanaRsPacketRegistry.HISTORY_TEMPORARY, fromTime, zoneId)