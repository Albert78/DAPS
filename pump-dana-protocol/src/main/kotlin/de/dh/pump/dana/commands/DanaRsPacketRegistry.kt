package de.dh.pump.dana.commands

import de.dh.pump.commands.CommandKind
import de.dh.pump.dana.protocol.DanaRsBleEncryption

/**
 * Central registry for DanaRS packet metadata.
 *
 * Every currently supported DanaRSPacket class is represented here. Notify packets are included for
 * inbound dispatch, but they are marked as notifications rather than sendable request/response
 * commands.
 */
object DanaRsPacketRegistry {
    val APS_BASAL_SET_TEMPORARY_BASAL = ack("DanaRSPacketAPSBasalSetTemporaryBasal", "BASAL__APS_SET_TEMPORARY_BASAL", DanaRsBleEncryption.DANAR_PACKET__OPCODE_BASAL__APS_SET_TEMPORARY_BASAL, CommandKind.CONTROL)
    val APS_HISTORY_EVENTS = raw("DanaRSPacketAPSHistoryEvents", "APS_HISTORY_EVENTS", DanaRsBleEncryption.DANAR_PACKET__OPCODE__APS_HISTORY_EVENTS, CommandKind.READ)
    val APS_SET_EVENT_HISTORY = ack("DanaRSPacketAPSSetEventHistory", "APS_SET_EVENT_HISTORY", DanaRsBleEncryption.DANAR_PACKET__OPCODE__APS_SET_EVENT_HISTORY, CommandKind.WRITE)

    val BASAL_GET_BASAL_RATE = raw("DanaRSPacketBasalGetBasalRate", "BASAL__GET_BASAL_RATE", DanaRsBleEncryption.DANAR_PACKET__OPCODE_BASAL__GET_BASAL_RATE, CommandKind.READ)
    val BASAL_GET_PROFILE_BASAL_RATE = raw("DanaRSPacketBasalGetProfileBasalRate", "BASAL__GET_PROFILE_BASAL_RATE", DanaRsBleEncryption.DANAR_PACKET__OPCODE_BASAL__GET_PROFILE_BASAL_RATE, CommandKind.READ)
    val BASAL_GET_PROFILE_NUMBER = raw("DanaRSPacketBasalGetProfileNumber", "BASAL__GET_PROFILE_NUMBER", DanaRsBleEncryption.DANAR_PACKET__OPCODE_BASAL__GET_PROFILE_NUMBER, CommandKind.READ)
    val BASAL_SET_CANCEL_TEMPORARY_BASAL = ack("DanaRSPacketBasalSetCancelTemporaryBasal", "BASAL__CANCEL_TEMPORARY_BASAL", DanaRsBleEncryption.DANAR_PACKET__OPCODE_BASAL__CANCEL_TEMPORARY_BASAL, CommandKind.ABORT_CONTROL)
    val BASAL_SET_PROFILE_BASAL_RATE = ack("DanaRSPacketBasalSetProfileBasalRate", "BASAL__SET_PROFILE_BASAL_RATE", DanaRsBleEncryption.DANAR_PACKET__OPCODE_BASAL__SET_PROFILE_BASAL_RATE, CommandKind.WRITE)
    val BASAL_SET_PROFILE_NUMBER = ack("DanaRSPacketBasalSetProfileNumber", "BASAL__SET_PROFILE_NUMBER", DanaRsBleEncryption.DANAR_PACKET__OPCODE_BASAL__SET_PROFILE_NUMBER, CommandKind.WRITE)
    val BASAL_SET_SUSPEND_OFF = ack("DanaRSPacketBasalSetSuspendOff", "BASAL__SET_SUSPEND_OFF", DanaRsBleEncryption.DANAR_PACKET__OPCODE_BASAL__SET_SUSPEND_OFF, CommandKind.ABORT_CONTROL)
    val BASAL_SET_SUSPEND_ON = ack("DanaRSPacketBasalSetSuspendOn", "BASAL__SET_SUSPEND_ON", DanaRsBleEncryption.DANAR_PACKET__OPCODE_BASAL__SET_SUSPEND_ON, CommandKind.CONTROL)
    val BASAL_SET_TEMPORARY_BASAL = ack("DanaRSPacketBasalSetTemporaryBasal", "BASAL__SET_TEMPORARY_BASAL", DanaRsBleEncryption.DANAR_PACKET__OPCODE_BASAL__SET_TEMPORARY_BASAL, CommandKind.CONTROL)

    val BOLUS_GET_24_CIR_CF_ARRAY = raw("DanaRSPacketBolusGet24CIRCFArray", "BOLUS__GET_24_CIR_CF_ARRAY", DanaRsBleEncryption.DANAR_PACKET__OPCODE_BOLUS__GET_24_CIR_CF_ARRAY, CommandKind.READ)
    val BOLUS_GET_BOLUS_OPTION = raw("DanaRSPacketBolusGetBolusOption", "BOLUS__GET_BOLUS_OPTION", DanaRsBleEncryption.DANAR_PACKET__OPCODE_BOLUS__GET_BOLUS_OPTION, CommandKind.READ)
    val BOLUS_GET_BOLUS_RATE = raw("DanaRSPacketBolusGetBolusRate", "BOLUS__GET_BOLUS_RATE", DanaRsBleEncryption.DANAR_PACKET__OPCODE_BOLUS__GET_BOLUS_RATE, CommandKind.READ)
    val BOLUS_GET_CALCULATION_INFORMATION = raw("DanaRSPacketBolusGetCalculationInformation", "BOLUS__GET_CALCULATION_INFORMATION", DanaRsBleEncryption.DANAR_PACKET__OPCODE_BOLUS__GET_CALCULATION_INFORMATION, CommandKind.READ)
    val BOLUS_GET_CIR_CF_ARRAY = raw("DanaRSPacketBolusGetCIRCFArray", "BOLUS__GET_CIR_CF_ARRAY", DanaRsBleEncryption.DANAR_PACKET__OPCODE_BOLUS__GET_CIR_CF_ARRAY, CommandKind.READ)
    val BOLUS_GET_STEP_BOLUS_INFORMATION = raw("DanaRSPacketBolusGetStepBolusInformation", "BOLUS__GET_STEP_BOLUS_INFORMATION", DanaRsBleEncryption.DANAR_PACKET__OPCODE_BOLUS__GET_STEP_BOLUS_INFORMATION, CommandKind.READ)
    val BOLUS_SET_24_CIR_CF_ARRAY = ack("DanaRSPacketBolusSet24CIRCFArray", "BOLUS__SET_24_CIR_CF_ARRAY", DanaRsBleEncryption.DANAR_PACKET__OPCODE_BOLUS__SET_24_CIR_CF_ARRAY, CommandKind.WRITE)
    val BOLUS_SET_BOLUS_OPTION = ack("DanaRSPacketBolusSetBolusOption", "BOLUS__SET_BOLUS_OPTION", DanaRsBleEncryption.DANAR_PACKET__OPCODE_BOLUS__SET_BOLUS_OPTION, CommandKind.WRITE)
    val BOLUS_SET_BOLUS_RATE = ack("DanaRSPacketBolusSetBolusRate", "BOLUS__SET_BOLUS_RATE", DanaRsBleEncryption.DANAR_PACKET__OPCODE_BOLUS__SET_BOLUS_RATE, CommandKind.WRITE)
    val BOLUS_SET_EXTENDED_BOLUS = ack("DanaRSPacketBolusSetExtendedBolus", "BOLUS__SET_EXTENDED_BOLUS", DanaRsBleEncryption.DANAR_PACKET__OPCODE_BOLUS__SET_EXTENDED_BOLUS, CommandKind.CONTROL)
    val BOLUS_SET_EXTENDED_BOLUS_CANCEL = ack("DanaRSPacketBolusSetExtendedBolusCancel", "BOLUS__SET_EXTENDED_BOLUS_CANCEL", DanaRsBleEncryption.DANAR_PACKET__OPCODE_BOLUS__SET_EXTENDED_BOLUS_CANCEL, CommandKind.ABORT_CONTROL)
    val BOLUS_SET_STEP_BOLUS_START = ack("DanaRSPacketBolusSetStepBolusStart", "BOLUS__SET_STEP_BOLUS_START", DanaRsBleEncryption.DANAR_PACKET__OPCODE_BOLUS__SET_STEP_BOLUS_START, CommandKind.CONTROL)
    val BOLUS_SET_STEP_BOLUS_STOP = ack("DanaRSPacketBolusSetStepBolusStop", "BOLUS__SET_STEP_BOLUS_STOP", DanaRsBleEncryption.DANAR_PACKET__OPCODE_BOLUS__SET_STEP_BOLUS_STOP, CommandKind.ABORT_CONTROL)

    val ETC_KEEP_CONNECTION = ack("DanaRSPacketEtcKeepConnection", "ETC__KEEP_CONNECTION", DanaRsBleEncryption.DANAR_PACKET__OPCODE_ETC__KEEP_CONNECTION, CommandKind.READ)
    val ETC_SET_HISTORY_SAVE = ack("DanaRSPacketEtcSetHistorySave", "ETC__SET_HISTORY_SAVE", DanaRsBleEncryption.DANAR_PACKET__OPCODE_ETC__SET_HISTORY_SAVE, CommandKind.WRITE)

    val GENERAL_GET_SHIPPING_VERSION = raw("DanaRSPacketGeneralGetShippingVersion", "GENERAL__GET_SHIPPING_VERSION", DanaRsBleEncryption.DANAR_PACKET__OPCODE_GENERAL__GET_SHIPPING_VERSION, CommandKind.READ)
    val GENERAL_GET_PUMP_CHECK = raw("DanaRSPacketGeneralGetPumpCheck", "REVIEW__GET_PUMP_CHECK", DanaRsBleEncryption.DANAR_PACKET__OPCODE_REVIEW__GET_PUMP_CHECK, CommandKind.READ)
    val GENERAL_GET_SHIPPING_INFORMATION = raw("DanaRSPacketGeneralGetShippingInformation", "REVIEW__GET_SHIPPING_INFORMATION", DanaRsBleEncryption.DANAR_PACKET__OPCODE_REVIEW__GET_SHIPPING_INFORMATION, CommandKind.READ)
    val GENERAL_GET_USER_TIME_CHANGE_FLAG = raw("DanaRSPacketGeneralGetUserTimeChangeFlag", "REVIEW__GET_USER_TIME_CHANGE_FLAG", DanaRsBleEncryption.DANAR_PACKET__OPCODE_REVIEW__GET_USER_TIME_CHANGE_FLAG, CommandKind.READ)
    val GENERAL_INITIAL_SCREEN_INFORMATION = raw("DanaRSPacketGeneralInitialScreenInformation", "REVIEW__INITIAL_SCREEN_INFORMATION", DanaRsBleEncryption.DANAR_PACKET__OPCODE_REVIEW__INITIAL_SCREEN_INFORMATION, CommandKind.READ)
    val GENERAL_SET_HISTORY_UPLOAD_MODE = ack("DanaRSPacketGeneralSetHistoryUploadMode", "REVIEW__SET_HISTORY_UPLOAD_MODE", DanaRsBleEncryption.DANAR_PACKET__OPCODE_REVIEW__SET_HISTORY_UPLOAD_MODE, CommandKind.WRITE)
    val GENERAL_SET_USER_TIME_CHANGE_FLAG_CLEAR = ack("DanaRSPacketGeneralSetUserTimeChangeFlagClear", "REVIEW__SET_USER_TIME_CHANGE_FLAG_CLEAR", DanaRsBleEncryption.DANAR_PACKET__OPCODE_REVIEW__SET_USER_TIME_CHANGE_FLAG_CLEAR, CommandKind.WRITE)
    val REVIEW_BOLUS_AVG = raw("DanaRSPacketReviewBolusAvg", "REVIEW__BOLUS_AVG", DanaRsBleEncryption.DANAR_PACKET__OPCODE_REVIEW__BOLUS_AVG, CommandKind.READ)
    val REVIEW_GET_PUMP_DEC_RATIO = raw("DanaRSPacketReviewGetPumpDecRatio", "REVIEW__GET_PUMP_DEC_RATIO", DanaRsBleEncryption.DANAR_PACKET__OPCODE_REVIEW__GET_PUMP_DEC_RATIO, CommandKind.READ)

    val HISTORY_ALARM = raw("DanaRSPacketHistoryAlarm", "REVIEW__ALARM", DanaRsBleEncryption.DANAR_PACKET__OPCODE_REVIEW__ALARM, CommandKind.READ)
    val HISTORY_ALL_HISTORY = raw("DanaRSPacketHistoryAllHistory", "REVIEW__ALL_HISTORY", DanaRsBleEncryption.DANAR_PACKET__OPCODE_REVIEW__ALL_HISTORY, CommandKind.READ)
    val HISTORY_BASAL = raw("DanaRSPacketHistoryBasal", "REVIEW__BASAL", DanaRsBleEncryption.DANAR_PACKET__OPCODE_REVIEW__BASAL, CommandKind.READ)
    val HISTORY_BLOOD_GLUCOSE = raw("DanaRSPacketHistoryBloodGlucose", "REVIEW__BLOOD_GLUCOSE", DanaRsBleEncryption.DANAR_PACKET__OPCODE_REVIEW__BLOOD_GLUCOSE, CommandKind.READ)
    val HISTORY_BOLUS = raw("DanaRSPacketHistoryBolus", "REVIEW__BOLUS", DanaRsBleEncryption.DANAR_PACKET__OPCODE_REVIEW__BOLUS, CommandKind.READ)
    val HISTORY_CARBOHYDRATE = raw("DanaRSPacketHistoryCarbohydrate", "REVIEW__CARBOHYDRATE", DanaRsBleEncryption.DANAR_PACKET__OPCODE_REVIEW__CARBOHYDRATE, CommandKind.READ)
    val HISTORY_DAILY = raw("DanaRSPacketHistoryDaily", "REVIEW__DAILY", DanaRsBleEncryption.DANAR_PACKET__OPCODE_REVIEW__DAILY, CommandKind.READ)
    val HISTORY_PRIME = raw("DanaRSPacketHistoryPrime", "REVIEW__PRIME", DanaRsBleEncryption.DANAR_PACKET__OPCODE_REVIEW__PRIME, CommandKind.READ)
    val HISTORY_REFILL = raw("DanaRSPacketHistoryRefill", "REVIEW__REFILL", DanaRsBleEncryption.DANAR_PACKET__OPCODE_REVIEW__REFILL, CommandKind.READ)
    val HISTORY_SUSPEND = raw("DanaRSPacketHistorySuspend", "REVIEW__SUSPEND", DanaRsBleEncryption.DANAR_PACKET__OPCODE_REVIEW__SUSPEND, CommandKind.READ)
    val HISTORY_TEMPORARY = raw("DanaRSPacketHistoryTemporary", "REVIEW__TEMPORARY", DanaRsBleEncryption.DANAR_PACKET__OPCODE_REVIEW__TEMPORARY, CommandKind.READ)

    val NOTIFY_ALARM = notify("DanaRSPacketNotifyAlarm", "NOTIFY__ALARM", DanaRsBleEncryption.DANAR_PACKET__OPCODE_NOTIFY__ALARM)
    val NOTIFY_DELIVERY_COMPLETE = notify("DanaRSPacketNotifyDeliveryComplete", "NOTIFY__DELIVERY_COMPLETE", DanaRsBleEncryption.DANAR_PACKET__OPCODE_NOTIFY__DELIVERY_COMPLETE)
    val NOTIFY_DELIVERY_RATE_DISPLAY = notify("DanaRSPacketNotifyDeliveryRateDisplay", "NOTIFY__DELIVERY_RATE_DISPLAY", DanaRsBleEncryption.DANAR_PACKET__OPCODE_NOTIFY__DELIVERY_RATE_DISPLAY)
    val NOTIFY_MISSED_BOLUS_ALARM = notify("DanaRSPacketNotifyMissedBolusAlarm", "NOTIFY__MISSED_BOLUS_ALARM", DanaRsBleEncryption.DANAR_PACKET__OPCODE_NOTIFY__MISSED_BOLUS_ALARM)

    val OPTION_GET_PUMP_TIME = raw("DanaRSPacketOptionGetPumpTime", "OPTION__GET_PUMP_TIME", DanaRsBleEncryption.DANAR_PACKET__OPCODE_OPTION__GET_PUMP_TIME, CommandKind.READ)
    val OPTION_GET_PUMP_UTC_AND_TIME_ZONE = raw("DanaRSPacketOptionGetPumpUTCAndTimeZone", "OPTION__GET_PUMP_UTC_AND_TIMEZONE", DanaRsBleEncryption.DANAR_PACKET__OPCODE_OPTION__GET_PUMP_UTC_AND_TIME_ZONE, CommandKind.READ)
    val OPTION_GET_USER_OPTION = raw("DanaRSPacketOptionGetUserOption", "OPTION__GET_USER_OPTION", DanaRsBleEncryption.DANAR_PACKET__OPCODE_OPTION__GET_USER_OPTION, CommandKind.READ)
    val OPTION_SET_PUMP_TIME = ack("DanaRSPacketOptionSetPumpTime", "OPTION__SET_PUMP_TIME", DanaRsBleEncryption.DANAR_PACKET__OPCODE_OPTION__SET_PUMP_TIME, CommandKind.WRITE)
    val OPTION_SET_PUMP_UTC_AND_TIME_ZONE = ack("DanaRSPacketOptionSetPumpUTCAndTimeZone", "OPTION__SET_PUMP_UTC_AND_TIMEZONE", DanaRsBleEncryption.DANAR_PACKET__OPCODE_OPTION__SET_PUMP_UTC_AND_TIME_ZONE, CommandKind.WRITE)
    val OPTION_SET_USER_OPTION = ack("DanaRSPacketOptionSetUserOption", "OPTION__SET_USER_OPTION", DanaRsBleEncryption.DANAR_PACKET__OPCODE_OPTION__SET_USER_OPTION, CommandKind.WRITE)

    val all: List<DanaRsPacketDefinition> = listOf(
        APS_BASAL_SET_TEMPORARY_BASAL, APS_HISTORY_EVENTS, APS_SET_EVENT_HISTORY,
        BASAL_GET_BASAL_RATE, BASAL_GET_PROFILE_BASAL_RATE, BASAL_GET_PROFILE_NUMBER, BASAL_SET_CANCEL_TEMPORARY_BASAL,
        BASAL_SET_PROFILE_BASAL_RATE, BASAL_SET_PROFILE_NUMBER, BASAL_SET_SUSPEND_OFF,
        BASAL_SET_SUSPEND_ON, BASAL_SET_TEMPORARY_BASAL,
        BOLUS_GET_24_CIR_CF_ARRAY, BOLUS_GET_BOLUS_OPTION, BOLUS_GET_BOLUS_RATE, BOLUS_GET_CALCULATION_INFORMATION,
        BOLUS_GET_CIR_CF_ARRAY, BOLUS_GET_STEP_BOLUS_INFORMATION, BOLUS_SET_24_CIR_CF_ARRAY,
        BOLUS_SET_BOLUS_OPTION, BOLUS_SET_BOLUS_RATE, BOLUS_SET_EXTENDED_BOLUS, BOLUS_SET_EXTENDED_BOLUS_CANCEL,
        BOLUS_SET_STEP_BOLUS_START, BOLUS_SET_STEP_BOLUS_STOP, ETC_KEEP_CONNECTION,
        ETC_SET_HISTORY_SAVE, GENERAL_GET_SHIPPING_VERSION, GENERAL_GET_PUMP_CHECK,
        GENERAL_GET_SHIPPING_INFORMATION, GENERAL_GET_USER_TIME_CHANGE_FLAG,
        GENERAL_INITIAL_SCREEN_INFORMATION, GENERAL_SET_HISTORY_UPLOAD_MODE,
        GENERAL_SET_USER_TIME_CHANGE_FLAG_CLEAR, REVIEW_BOLUS_AVG, REVIEW_GET_PUMP_DEC_RATIO,
        HISTORY_ALARM, HISTORY_ALL_HISTORY, HISTORY_BASAL, HISTORY_BLOOD_GLUCOSE,
        HISTORY_BOLUS, HISTORY_CARBOHYDRATE, HISTORY_DAILY, HISTORY_PRIME, HISTORY_REFILL,
        HISTORY_SUSPEND, HISTORY_TEMPORARY, NOTIFY_ALARM, NOTIFY_DELIVERY_COMPLETE, NOTIFY_DELIVERY_RATE_DISPLAY,
        NOTIFY_MISSED_BOLUS_ALARM, OPTION_GET_PUMP_TIME, OPTION_GET_PUMP_UTC_AND_TIME_ZONE,
        OPTION_GET_USER_OPTION, OPTION_SET_PUMP_TIME, OPTION_SET_PUMP_UTC_AND_TIME_ZONE,
        OPTION_SET_USER_OPTION,
    )

    private val byResponseCommand = all.associateBy { (it.responseType shl 8) or it.opcode }

    fun findByResponse(responseType: Int, opcode: Int): DanaRsPacketDefinition? {
        return byResponseCommand[((responseType and 0xff) shl 8) or (opcode and 0xff)]
    }

    private fun raw(sourceClassName: String, friendlyName: String, opcode: Int, kind: CommandKind): DanaRsPacketDefinition {
        return DanaRsPacketDefinition(sourceClassName, friendlyName, opcode, kind)
    }

    private fun ack(sourceClassName: String, friendlyName: String, opcode: Int, kind: CommandKind): DanaRsPacketDefinition {
        return DanaRsPacketDefinition(sourceClassName, friendlyName, opcode, kind, responseShape = DanaRsResponseShape.ACK_RESULT)
    }

    private fun notify(sourceClassName: String, friendlyName: String, opcode: Int): DanaRsPacketDefinition {
        return DanaRsPacketDefinition(
            sourceClassName = sourceClassName,
            friendlyName = friendlyName,
            opcode = opcode,
            kind = CommandKind.READ,
            direction = DanaRsPacketDirection.NOTIFY,
            responseShape = DanaRsResponseShape.NOTIFY,
            responseType = DanaRsBleEncryption.DANAR_PACKET__TYPE_NOTIFY,
        )
    }
}