package de.dh.pump.dana.commands

import de.dh.pump.dana.commands.aps.ApsBasalSetTemporaryBasalCommand
import de.dh.pump.dana.commands.aps.ApsHistoryEventsCommand
import de.dh.pump.dana.commands.aps.ApsSetEventHistoryCommand
import de.dh.pump.dana.commands.basal.BasalGetBasalRateCommand
import de.dh.pump.dana.commands.basal.BasalGetProfileBasalRateCommand
import de.dh.pump.dana.commands.basal.BasalGetProfileNumberCommand
import de.dh.pump.dana.commands.basal.BasalSetCancelTemporaryBasalCommand
import de.dh.pump.dana.commands.basal.BasalSetProfileBasalRateCommand
import de.dh.pump.dana.commands.basal.BasalSetProfileNumberCommand
import de.dh.pump.dana.commands.basal.BasalSetSuspendOffCommand
import de.dh.pump.dana.commands.basal.BasalSetSuspendOnCommand
import de.dh.pump.dana.commands.basal.BasalSetTemporaryBasalCommand
import de.dh.pump.dana.commands.bolus.BolusGet24CIRCFArrayCommand
import de.dh.pump.dana.commands.bolus.BolusGetBolusOptionCommand
import de.dh.pump.dana.commands.bolus.BolusGetBolusRateCommand
import de.dh.pump.dana.commands.bolus.BolusGetCIRCFArrayCommand
import de.dh.pump.dana.commands.bolus.BolusGetCalculationInformationCommand
import de.dh.pump.dana.commands.bolus.BolusGetStepBolusInformationCommand
import de.dh.pump.dana.commands.bolus.BolusSet24CIRCFArrayCommand
import de.dh.pump.dana.commands.bolus.BolusSetBolusOptionCommand
import de.dh.pump.dana.commands.bolus.BolusSetBolusRateCommand
import de.dh.pump.dana.commands.bolus.BolusSetExtendedBolusCancelCommand
import de.dh.pump.dana.commands.bolus.BolusSetExtendedBolusCommand
import de.dh.pump.dana.commands.bolus.BolusSetStepBolusStartCommand
import de.dh.pump.dana.commands.bolus.BolusSetStepBolusStopCommand
import de.dh.pump.dana.commands.bolus.MissedBolusWindow
import de.dh.pump.dana.commands.etc.EtcKeepConnectionCommand
import de.dh.pump.dana.commands.etc.EtcSetHistorySaveCommand
import de.dh.pump.dana.commands.general.GeneralGetPumpCheckCommand
import de.dh.pump.dana.commands.general.GeneralGetShippingInformationCommand
import de.dh.pump.dana.commands.general.GeneralGetShippingVersionCommand
import de.dh.pump.dana.commands.general.GeneralGetUserTimeChangeFlagCommand
import de.dh.pump.dana.commands.general.GeneralInitialScreenInformationCommand
import de.dh.pump.dana.commands.general.GeneralSetHistoryUploadModeCommand
import de.dh.pump.dana.commands.general.GeneralSetUserTimeChangeFlagClearCommand
import de.dh.pump.dana.commands.general.ReviewBolusAverageCommand
import de.dh.pump.dana.commands.general.ReviewGetPumpDecRatioCommand
import de.dh.pump.dana.commands.history.HistoryAlarmCommand
import de.dh.pump.dana.commands.history.HistoryAllHistoryCommand
import de.dh.pump.dana.commands.history.HistoryBasalCommand
import de.dh.pump.dana.commands.history.HistoryBloodGlucoseCommand
import de.dh.pump.dana.commands.history.HistoryBolusCommand
import de.dh.pump.dana.commands.history.HistoryCarbohydrateCommand
import de.dh.pump.dana.commands.history.HistoryDailyCommand
import de.dh.pump.dana.commands.history.HistoryPrimeCommand
import de.dh.pump.dana.commands.history.HistoryRefillCommand
import de.dh.pump.dana.commands.history.HistorySuspendCommand
import de.dh.pump.dana.commands.history.HistoryTemporaryCommand
import de.dh.pump.dana.commands.options.DanaRsUserOptions
import de.dh.pump.dana.commands.options.OptionGetPumpTimeCommand
import de.dh.pump.dana.commands.options.OptionGetPumpUtcAndTimeZoneCommand
import de.dh.pump.dana.commands.options.OptionGetUserOptionCommand
import de.dh.pump.dana.commands.options.OptionSetPumpTimeCommand
import de.dh.pump.dana.commands.options.OptionSetPumpUtcAndTimeZoneCommand
import de.dh.pump.dana.commands.options.OptionSetUserOptionCommand
import de.dh.daps.common.model.data.PumpTimestamp
import java.time.ZoneId

/**
 * Public command factory for DanaRS/Dana-i packets.
 *
 * The individual command classes own request encoding and response decoding. This facade keeps the
 * call site compact and gives UI/application code one stable entry point.
 */
class DanaRsCommands {
    val definitions: List<DanaRsPacketDefinition> = DanaRsPacketRegistry.all

    fun basalGetBasalRate() = BasalGetBasalRateCommand()
    fun basalGetProfileBasalRate(profileNumber: Int) = BasalGetProfileBasalRateCommand(profileNumber)
    fun basalGetProfileNumber() = BasalGetProfileNumberCommand()
    fun basalSetCancelTemporaryBasal() = BasalSetCancelTemporaryBasalCommand()
    fun basalSetTemporaryBasal(ratioPercent: Int, durationHours: Int) =
        BasalSetTemporaryBasalCommand(ratioPercent, durationHours)

    fun basalSetProfileNumber(profileNumber: Int) = BasalSetProfileNumberCommand(profileNumber)
    fun basalSetSuspendOff() = BasalSetSuspendOffCommand()
    fun basalSetSuspendOn() = BasalSetSuspendOnCommand()
    fun basalSetProfileBasalRate(profileNumber: Int, hourlyRatesUnits: List<Double>) =
        BasalSetProfileBasalRateCommand(profileNumber, hourlyRatesUnits)

    fun apsBasalSetTemporaryBasal(percent: Int) = ApsBasalSetTemporaryBasalCommand.create(percent)
    fun apsHistoryEvents(
        fromTime: PumpTimestamp,
        zoneId: ZoneId = ZoneId.systemDefault(),
        useUtcLayout: Boolean = true,
    ) = ApsHistoryEventsCommand(fromTime, zoneId, useUtcLayout)

    fun apsHistoryEvents(
        fromMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
        useUtcLayout: Boolean = true,
    ) = apsHistoryEvents(PumpTimestamp(fromMillis), zoneId, useUtcLayout)

    fun apsSetEventHistory(
        packetType: Int,
        time: PumpTimestamp,
        param1: Int,
        param2: Int,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ) = ApsSetEventHistoryCommand(packetType, time, param1, param2, zoneId)

    fun apsSetEventHistory(
        packetType: Int,
        timeMillis: Long,
        param1: Int,
        param2: Int,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ) = apsSetEventHistory(packetType, PumpTimestamp(timeMillis), param1, param2, zoneId)

    fun bolusGet24CIRCFArray() = BolusGet24CIRCFArrayCommand()
    fun bolusGetBolusOption() = BolusGetBolusOptionCommand()

    /**
     * Doesn't work on Dana-i.
     */
    fun bolusGetBolusRate() = BolusGetBolusRateCommand()
    fun bolusGetCalculationInformation() = BolusGetCalculationInformationCommand()
    fun bolusGetCIRCFArray() = BolusGetCIRCFArrayCommand()
    fun bolusGetStepBolusInformation() = BolusGetStepBolusInformationCommand()
    fun bolusSetStepBolusStart(amountUnits: Double, speed: DanaRsBolusSpeed) =
        BolusSetStepBolusStartCommand(amountUnits, speed)

    fun bolusSetStepBolusStop() = BolusSetStepBolusStopCommand()
    fun bolusSetExtendedBolus(amountUnits: Double, durationHalfHours: Int) =
        BolusSetExtendedBolusCommand(amountUnits, durationHalfHours)

    fun bolusSetExtendedBolusCancel() = BolusSetExtendedBolusCancelCommand()
    fun bolusSet24CIRCFArray(ic: IntArray, cf: IntArray) = BolusSet24CIRCFArrayCommand(ic, cf)
    fun bolusSetBolusOption(
        extendedBolusEnabled: Boolean,
        bolusCalculationOption: Int,
        missedBolusConfig: Int,
        missedBolusWindows: List<MissedBolusWindow>,
    ) = BolusSetBolusOptionCommand(
        extendedBolusEnabled = extendedBolusEnabled,
        bolusCalculationOption = bolusCalculationOption,
        missedBolusConfig = missedBolusConfig,
        missedBolusWindows = missedBolusWindows,
    )

    /**
     * Doesn't work on Dana-i.
     */
    fun bolusSetBolusRate(
        maxBolusUnits: Double,
        bolusStepUnits: Double,
        speed: DanaRsBolusSpeed,
    ) = BolusSetBolusRateCommand(
        maxBolusUnits = maxBolusUnits,
        bolusStepUnits = bolusStepUnits,
        speed = speed,
    )

    fun etcKeepConnection() = EtcKeepConnectionCommand()
    fun etcSetHistorySave(
        historyType: Int,
        historyYear: Int,
        historyMonth: Int,
        historyDate: Int,
        historyHour: Int,
        historyMinute: Int,
        historySecond: Int,
        historyCode: Int,
        historyValue: Int,
    ) = EtcSetHistorySaveCommand(
        historyType = historyType,
        historyYear = historyYear,
        historyMonth = historyMonth,
        historyDate = historyDate,
        historyHour = historyHour,
        historyMinute = historyMinute,
        historySecond = historySecond,
        historyCode = historyCode,
        historyValue = historyValue,
    )

    fun generalGetShippingVersion() = GeneralGetShippingVersionCommand()
    fun generalGetPumpCheck() = GeneralGetPumpCheckCommand()
    fun generalGetShippingInformation() = GeneralGetShippingInformationCommand()
    fun generalGetUserTimeChangeFlag() = GeneralGetUserTimeChangeFlagCommand()
    fun generalInitialScreenInformation() = GeneralInitialScreenInformationCommand()
    fun generalSetHistoryUploadMode(mode: Int) = GeneralSetHistoryUploadModeCommand(mode)
    fun generalSetUserTimeChangeFlagClear() = GeneralSetUserTimeChangeFlagClearCommand()
    fun reviewBolusAverage() = ReviewBolusAverageCommand()
    fun reviewGetPumpDecRatio() = ReviewGetPumpDecRatioCommand()

    fun historyAlarm(fromTime: PumpTimestamp, zoneId: ZoneId = ZoneId.systemDefault()) = HistoryAlarmCommand(fromTime, zoneId)
    fun historyAlarm(fromMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()) = historyAlarm(PumpTimestamp(fromMillis), zoneId)

    fun historyAllHistory(fromTime: PumpTimestamp, zoneId: ZoneId = ZoneId.systemDefault()) = HistoryAllHistoryCommand(fromTime, zoneId)
    fun historyAllHistory(fromMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()) = historyAllHistory(PumpTimestamp(fromMillis), zoneId)

    fun historyBasal(fromTime: PumpTimestamp, zoneId: ZoneId = ZoneId.systemDefault()) = HistoryBasalCommand(fromTime, zoneId)
    fun historyBasal(fromMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()) = historyBasal(PumpTimestamp(fromMillis), zoneId)

    fun historyBloodGlucose(fromTime: PumpTimestamp, zoneId: ZoneId = ZoneId.systemDefault()) = HistoryBloodGlucoseCommand(fromTime, zoneId)
    fun historyBloodGlucose(fromMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()) = historyBloodGlucose(PumpTimestamp(fromMillis), zoneId)

    fun historyBolus(fromTime: PumpTimestamp, zoneId: ZoneId = ZoneId.systemDefault()) = HistoryBolusCommand(fromTime, zoneId)
    fun historyBolus(fromMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()) = historyBolus(PumpTimestamp(fromMillis), zoneId)

    fun historyCarbohydrate(fromTime: PumpTimestamp, zoneId: ZoneId = ZoneId.systemDefault()) = HistoryCarbohydrateCommand(fromTime, zoneId)
    fun historyCarbohydrate(fromMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()) = historyCarbohydrate(PumpTimestamp(fromMillis), zoneId)

    fun historyDaily(fromTime: PumpTimestamp, zoneId: ZoneId = ZoneId.systemDefault()) = HistoryDailyCommand(fromTime, zoneId)
    fun historyDaily(fromMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()) = historyDaily(PumpTimestamp(fromMillis), zoneId)

    fun historyPrime(fromTime: PumpTimestamp, zoneId: ZoneId = ZoneId.systemDefault()) = HistoryPrimeCommand(fromTime, zoneId)
    fun historyPrime(fromMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()) = historyPrime(PumpTimestamp(fromMillis), zoneId)

    fun historyRefill(fromTime: PumpTimestamp, zoneId: ZoneId = ZoneId.systemDefault()) = HistoryRefillCommand(fromTime, zoneId)
    fun historyRefill(fromMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()) = historyRefill(PumpTimestamp(fromMillis), zoneId)

    fun historySuspend(fromTime: PumpTimestamp, zoneId: ZoneId = ZoneId.systemDefault()) = HistorySuspendCommand(fromTime, zoneId)
    fun historySuspend(fromMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()) = historySuspend(PumpTimestamp(fromMillis), zoneId)

    fun historyTemporary(fromTime: PumpTimestamp, zoneId: ZoneId = ZoneId.systemDefault()) = HistoryTemporaryCommand(fromTime, zoneId)
    fun historyTemporary(fromMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()) = historyTemporary(PumpTimestamp(fromMillis), zoneId)

    /**
     * Doesn't work on Dana-i.
     */
    fun optionGetPumpTime() = OptionGetPumpTimeCommand()
    fun optionGetPumpUtcAndTimeZone() = OptionGetPumpUtcAndTimeZoneCommand()
    fun optionGetUserOption() = OptionGetUserOptionCommand()

    /**
     * Doesn't work on Dana-i.
     */
    fun optionSetPumpTime(time: PumpTimestamp, zoneId: ZoneId = ZoneId.systemDefault()) =
        OptionSetPumpTimeCommand(time, zoneId)

    fun optionSetPumpTime(timeMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()) =
        optionSetPumpTime(PumpTimestamp(timeMillis), zoneId)

    fun optionSetPumpUtcAndTimeZone(time: PumpTimestamp, zoneOffset: Int) =
        OptionSetPumpUtcAndTimeZoneCommand(time, zoneOffset)

    fun optionSetPumpUtcAndTimeZone(timeMillis: Long, zoneOffset: Int) =
        optionSetPumpUtcAndTimeZone(PumpTimestamp(timeMillis), zoneOffset)

    fun optionSetUserOption(options: DanaRsUserOptions) = OptionSetUserOptionCommand(options)
}