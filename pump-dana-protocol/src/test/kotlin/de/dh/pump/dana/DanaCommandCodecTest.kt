package de.dh.pump.dana

import de.dh.pump.PumpStatus
import de.dh.pump.dana.commands.DanaRsBolusSpeed
import de.dh.pump.dana.commands.DanaRsCommands
import de.dh.pump.dana.commands.aps.ApsHistoryEndChunk
import de.dh.pump.dana.commands.aps.ApsHistoryEventChunk
import de.dh.pump.dana.commands.aps.ApsHistoryEventKind
import de.dh.pump.dana.commands.bolus.MissedBolusWindow
import de.dh.pump.dana.commands.general.DanaRsPumpErrorState
import de.dh.pump.dana.commands.history.DanaRsHistoryBolusType
import de.dh.pump.dana.commands.history.DanaRsHistoryRecordKind
import de.dh.pump.dana.commands.history.HistoryEndResponse
import de.dh.pump.dana.commands.history.HistoryRecordResponse
import de.dh.pump.protocol.ByteReader
import de.dh.pump.protocol.ByteWriter
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class DanaCommandCodecTest {
    private val commands = DanaRsCommands()

    @Test
    fun apsTemporaryBasalAboveOneHundredUsesAapsFifteenMinutePayloadLayout() {
        val command = commands.apsBasalSetTemporaryBasal(percent = 125)

        val payload = ByteWriter().also(command::encodePayload).toByteArray()

        assertArrayEquals(byteArrayOf(0x7d, 0x00, 150.toByte()), payload)
    }

    @Test
    fun apsTemporaryBasalBelowOneHundredUsesAapsThirtyMinutePayloadLayout() {
        val command = commands.apsBasalSetTemporaryBasal(percent = 80)

        val payload = ByteWriter().also(command::encodePayload).toByteArray()

        assertArrayEquals(byteArrayOf(0x50, 0x00, 160.toByte()), payload)
    }

    @Test
    fun bolusStartUsesCentiUnitPayloadLayout() {
        val command = commands.bolusSetStepBolusStart(
            amountUnits = 1.25,
            speed = DanaRsBolusSpeed.U12_SECONDS,
        )

        val payload = ByteWriter().also(command::encodePayload).toByteArray()

        assertArrayEquals(byteArrayOf(0x7d, 0x00, 0x00), payload)
    }

    @Test
    fun historyCommandsUseReferenceStartDateWhenNoCursorIsKnown() {
        val command = commands.historyBolus(fromMillis = 0L)

        val payload = ByteWriter().also(command::encodePayload).toByteArray()

        assertArrayEquals(byteArrayOf(0, 1, 1, 0, 0, 0), payload)
    }

    @Test
    fun ackCommandsDecodeOneByteResultCode() {
        val command = commands.basalSetCancelTemporaryBasal()

        val response = command.decodePayload(ByteReader(byteArrayOf(0x00)))

        assertEquals(PumpStatus.OK, response.status)
        assertEquals(0x00, response.resultCode)
    }

    @Test
    fun bolusSetBolusOptionUsesReferencePayloadLayout() {
        val command = commands.bolusSetBolusOption(
            extendedBolusEnabled = true,
            bolusCalculationOption = 2,
            missedBolusConfig = 3,
            missedBolusWindows = listOf(
                MissedBolusWindow(1, 2, 3, 4),
                MissedBolusWindow(5, 6, 7, 8),
                MissedBolusWindow(9, 10, 11, 12),
                MissedBolusWindow(13, 14, 15, 16),
            ),
        )

        val payload = ByteWriter().also(command::encodePayload).toByteArray()

        assertArrayEquals(byteArrayOf(1, 2, 3, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16), payload)
    }

    @Test
    fun etcSetHistorySaveUsesReferencePayloadLayout() {
        val command = commands.etcSetHistorySave(
            historyType = 2,
            historyYear = 26,
            historyMonth = 5,
            historyDate = 15,
            historyHour = 14,
            historyMinute = 30,
            historySecond = 10,
            historyCode = 0x82,
            historyValue = 125,
        )

        val payload = ByteWriter().also(command::encodePayload).toByteArray()

        assertArrayEquals(byteArrayOf(2, 26, 5, 15, 14, 30, 10, 0x82.toByte(), 0x7d, 0x00), payload)
    }

    @Test
    fun unusedReviewCommandsDecodeResponses() {
        val shippingVersion = commands.generalGetShippingVersion().decodePayload(ByteReader("BLE-1".toByteArray()))
        val timeFlag = commands.generalGetUserTimeChangeFlag().decodePayload(ByteReader(byteArrayOf(1)))
        val bolusAverage = commands.reviewBolusAverage().decodePayload(
            ByteReader(byteArrayOf(100, 0, 110, 0, 120, 0, 130.toByte(), 0, 140.toByte(), 0)),
        )
        val decRatio = commands.reviewGetPumpDecRatio().decodePayload(ByteReader(byteArrayOf(4)))

        assertEquals("BLE-1", shippingVersion.bleModel)
        assertEquals(true, timeFlag.changedByUser)
        assertEquals(1.0, bolusAverage.threeDayAverageUnits, 0.0001)
        assertEquals(1.4, bolusAverage.twentyEightDayAverageUnits, 0.0001)
        assertEquals(20, decRatio.ratioPercent)
    }

    @Test
    fun apsHistoryEventsDecodeRecordAndEndChunks() {
        val command = commands.apsHistoryEvents(fromMillis = 0L)

        val record = command.decodePayload(
            ByteReader(byteArrayOf(0x05, 26, 5, 15, 14, 30, 10, 0x00, 0x7d, 0x00, 0x00)),
        ) as ApsHistoryEventChunk
        val end = command.decodePayload(ByteReader(byteArrayOf(0xff.toByte())))

        assertEquals(record.events.size, 1)
        assertEquals(PumpStatus.OK, record.status)
        val event = record.events[0]
        assertEquals(ApsHistoryEventKind.BOLUS, event.kind)
        assertEquals(1.25, event.insulinUnits ?: 0.0, 0.0001)
        assertEquals(PumpStatus.OK, end.status)
        assertEquals(true, end is ApsHistoryEndChunk)
    }

    @Test
    fun initialScreenInformationDecodesReferenceLayout() {
        val command = commands.generalInitialScreenInformation()
        val response = command.decodePayload(
            ByteReader(
                byteArrayOf(
                    0x1d,
                    0x7b, 0x00,
                    0xc4.toByte(), 0x09,
                    0x10, 0x27,
                    0x50, 0x00,
                    150.toByte(),
                    88,
                    0x2c, 0x01,
                    0x19, 0x00,
                    0x02,
                ),
            ),
        )

        assertEquals(PumpStatus.OK, response.status)
        assertEquals(true, response.pumpSuspended)
        assertEquals(true, response.tempBasalInProgress)
        assertEquals(true, response.extendedBolusInProgress)
        assertEquals(true, response.dualBolusInProgress)
        assertEquals(1.23, response.dailyTotalUnits, 0.0001)
        assertEquals(25, response.maxDailyTotalUnits)
        assertEquals(100.0, response.reservoirRemainingUnits, 0.0001)
        assertEquals(0.8, response.currentBasalUnitsPerHour, 0.0001)
        assertEquals(150, response.tempBasalPercent)
        assertEquals(88, response.batteryRemainingPercent)
        assertEquals(3.0, response.extendedBolusAbsoluteRate, 0.0001)
        assertEquals(0.25, response.insulinOnBoardUnits, 0.0001)
        assertEquals(setOf(DanaRsPumpErrorState.DAILY_MAX), response.errorStates)
    }

    @Test
    fun basalRatesDecodeTwentyFourHourlyValues() {
        val command = commands.basalGetBasalRate()
        val payload = ByteWriter()
            .writeUInt16Le(300)
            .writeUInt8(1)
            .also { writer ->
                repeat(24) { hour -> writer.writeUInt16Le(100 + hour) }
            }
            .toByteArray()

        val response = command.decodePayload(ByteReader(payload))

        assertEquals(3.0, response.maxBasalUnitsPerHour, 0.0001)
        assertEquals(0.01, response.basalStepUnits, 0.0001)
        assertEquals(true, response.basalStepSupported)
        assertEquals(24, response.hourlyRatesUnits.size)
        assertEquals(1.0, response.hourlyRatesUnits.first(), 0.0001)
        assertEquals(1.23, response.hourlyRatesUnits.last(), 0.0001)
    }

    @Test
    fun stepBolusInformationDecodesAmountsAndClockTime() {
        val command = commands.bolusGetStepBolusInformation()
        val response = command.decodePayload(
            ByteReader(
                byteArrayOf(
                    0x00,
                    0x02,
                    0x7d, 0x00,
                    14,
                    30,
                    0x32, 0x00,
                    0xf4.toByte(), 0x01,
                    5,
                ),
            ),
        )

        assertEquals(PumpStatus.OK, response.status)
        assertEquals(2, response.bolusType)
        assertEquals(1.25, response.initialBolusAmountUnits, 0.0001)
        assertEquals(LocalTime.of(14, 30), response.lastBolusTimeOfDayUTC)
        assertEquals(0.5, response.lastBolusAmountUnits, 0.0001)
        assertEquals(5.0, response.maxBolusUnits, 0.0001)
        assertEquals(0.05, response.bolusStepUnits, 0.0001)
    }

    @Test
    fun userOptionsDecodeSelectableLanguagesAndTarget() {
        val command = commands.optionGetUserOption()
        val response = command.decodePayload(
            ByteReader(
                byteArrayOf(
                    0,
                    1,
                    2,
                    5,
                    15,
                    3,
                    0,
                    12,
                    20,
                    0x2c, 0x01,
                    0xf4.toByte(), 0x01,
                    1,
                    2,
                    3,
                    4,
                    5,
                    100,
                    0,
                ),
            ),
        )

        assertEquals(PumpStatus.OK, response.status)
        assertEquals(true, response.timeDisplayType24)
        assertEquals(true, response.buttonScrollOnOff)
        assertEquals(300, response.cannulaVolume)
        assertEquals(500, response.refillAmount)
        assertEquals(listOf(1, 2, 3, 4, 5), response.selectableLanguages)
        assertEquals(100, response.target)
    }

    @Test
    fun historyEndPacketDecodesResultAndTotalCount() {
        val command = commands.historyBolus(fromMillis = 0L)

        val response = command.decodePayload(ByteReader(byteArrayOf(0x00, 0x02, 0x00)))

        val end = response as HistoryEndResponse
        assertEquals(PumpStatus.OK, end.status)
        assertEquals(0, end.errorCode)
        assertEquals(2, end.totalCount)
    }

    @Test
    fun historyRecordPacketDecodesBolusRecord() {
        val command = commands.historyBolus(fromMillis = 0L)

        val response = command.decodePayload(
            ByteReader(byteArrayOf(0x02, 26, 5, 15, 14, 30, 10, 0x82.toByte(), 0x00, 0x7d)),
        )

        val record = (response as HistoryRecordResponse).record
        assertEquals(DanaRsHistoryRecordKind.BOLUS, record.kind)
        val expectedMillis = LocalDateTime.of(2026, 5, 15, 14, 30).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        assertEquals(expectedMillis, record.timestamp.rawMs)
        assertEquals(DanaRsHistoryBolusType.STEP, record.bolusType)
        assertEquals(130, record.durationMinutes)
        assertEquals(1.25, record.value ?: 0.0, 0.0001)
    }
}