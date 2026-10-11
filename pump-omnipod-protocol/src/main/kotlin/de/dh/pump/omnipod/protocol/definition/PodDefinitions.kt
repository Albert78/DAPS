package de.dh.pump.omnipod.protocol.definition

import java.io.Serializable
import java.nio.ByteBuffer
import java.time.Duration
import java.util.Calendar
import java.util.Collections
import kotlin.experimental.and
import kotlin.experimental.or

interface HasValue {
    val value: Byte
}

inline fun <reified T> byValue(value: Byte, default: T): T where T : Enum<T>, T : HasValue {
    return enumValues<T>().firstOrNull { it.value == value } ?: default
}

interface Encodable {
    val encoded: ByteArray
}

enum class ActivationProgress {
    NOT_STARTED,
    GOT_POD_VERSION,
    SET_UNIQUE_ID,
    PROGRAMMED_LOW_RESERVOIR_ALERTS,
    REPROGRAMMED_LUMP_OF_COAL_ALERT,
    PRIMING,
    PRIME_COMPLETED,
    PHASE_1_COMPLETED,
    PROGRAMMED_BASAL,
    UPDATED_EXPIRATION_ALERTS,
    INSERTING_CANNULA,
    CANNULA_INSERTED,
    COMPLETED;

    fun isBefore(other: ActivationProgress): Boolean = ordinal < other.ordinal
    fun isAtLeast(other: ActivationProgress): Boolean = ordinal >= other.ordinal
}

object PodConstants {
    val MAX_POD_LIFETIME: Duration = Duration.ofHours(80)
    const val POD_EXPIRATION_ALERT_HOURS_REMAINING_DEFAULT = 7L
    const val POD_EXPIRATION_IMMINENT_ALERT_HOURS_REMAINING = 1L
    const val POD_PULSE_BOLUS_UNITS = 0.05
    const val DEFAULT_MAX_RESERVOIR_ALERT_THRESHOLD: Short = 20
}

enum class PodStatus(override val value: Byte) : HasValue {
    UNINITIALIZED(0x00.toByte()),
    MFG_TEST(0x01.toByte()),
    FILLED(0x02.toByte()),
    UID_SET(0x03.toByte()),
    ENGAGING_CLUTCH_DRIVE(0x04.toByte()),
    CLUTCH_DRIVE_ENGAGED(0x05.toByte()),
    BASAL_PROGRAM_SET(0x06.toByte()),
    PRIMING(0x07.toByte()),
    RUNNING_ABOVE_MIN_VOLUME(0x08.toByte()),
    RUNNING_BELOW_MIN_VOLUME(0x09.toByte()),
    UNUSED_10(0x0a.toByte()),
    UNUSED_11(0x0b.toByte()),
    UNUSED_12(0x0c.toByte()),
    ALARM(0x0d.toByte()),
    LUMP_OF_COAL(0x0e.toByte()),
    DEACTIVATED(0x0f.toByte()),
    UNKNOWN(0xff.toByte());

    fun isRunning(): Boolean = this == RUNNING_ABOVE_MIN_VOLUME || this == RUNNING_BELOW_MIN_VOLUME
}

enum class DeliveryStatus(override val value: Byte) : HasValue {
    SUSPENDED(0x00.toByte()),
    BASAL_ACTIVE(0x01.toByte()),
    TEMP_BASAL_ACTIVE(0x02.toByte()),
    PRIMING(0x04.toByte()),
    BOLUS_AND_BASAL_ACTIVE(0x05.toByte()),
    BOLUS_AND_TEMP_BASAL_ACTIVE(0x06.toByte()),
    UNKNOWN(0xff.toByte());

    fun bolusDeliveringActive(): Boolean {
        return value in arrayOf(BOLUS_AND_BASAL_ACTIVE.value, BOLUS_AND_TEMP_BASAL_ACTIVE.value)
    }

    fun basalActive(): Boolean {
        return value in arrayOf(BOLUS_AND_BASAL_ACTIVE.value, BASAL_ACTIVE.value)
    }

    fun tempBasalActive(): Boolean {
        return value in arrayOf(BOLUS_AND_TEMP_BASAL_ACTIVE.value, TEMP_BASAL_ACTIVE.value)
    }

    fun suspended(): Boolean {
        return value == SUSPENDED.value
    }
}

enum class AlertType(val index: Byte) : HasValue {
    AUTO_OFF(0x00.toByte()),
    MULTI_COMMAND(0x01.toByte()),
    EXPIRATION_IMMINENT(0x02.toByte()),
    USER_SET_EXPIRATION(0x03.toByte()),
    LOW_RESERVOIR(0x04.toByte()),
    SUSPEND_IN_PROGRESS(0x05.toByte()),
    SUSPEND_ENDED(0x06.toByte()),
    EXPIRATION(0x07.toByte()),
    UNKNOWN(0xff.toByte());

    override val value: Byte
        get() = if (this == UNKNOWN) {
            0xff.toByte()
        } else {
            (1 shl index.toInt()).toByte()
        }
}

sealed class AlertTrigger {
    class TimerTrigger(val offsetInMinutes: Short) : AlertTrigger()
    class ReservoirVolumeTrigger(val thresholdInMicroLiters: Short) : AlertTrigger()
}

enum class BeepType(val value: Byte) {
    SILENT(0x00.toByte()),
    FOUR_TIMES_BIP_BEEP(0x02.toByte()),
    SUSPEND_BEEP(0x04.toByte()),
    LONG_SINGLE_BEEP(0x06.toByte())
}

enum class BeepRepetitionType(val value: Byte) {
    ONCE(0x01.toByte()),
    EVERY_MINUTE_AND_EVERY_15_MIN(0x03.toByte()),
    REPEAT_3(0x05.toByte()),
    REPEAT_4(0x06.toByte()),
    REPEAT_5(0x08.toByte())
}

enum class BolusType {
    DEFAULT, SMB, BASAL_CORRECTION, PRIMING
}

class AlertConfiguration(
    val type: AlertType,
    val enabled: Boolean,
    val durationInMinutes: Short,
    val autoOff: Boolean,
    val trigger: AlertTrigger,
    val beepType: BeepType,
    val beepRepetition: BeepRepetitionType
) : Encodable {

    override val encoded: ByteArray
        get() {
            var firstByte = (type.index.toInt() shl 4).toByte()
            if (enabled) {
                firstByte = (firstByte.toInt() or (1 shl 3)).toByte()
            }
            if (trigger is AlertTrigger.ReservoirVolumeTrigger) {
                firstByte = (firstByte.toInt() or (1 shl 2)).toByte()
            }
            if (autoOff) {
                firstByte = (firstByte.toInt() or (1 shl 1)).toByte()
            }
            firstByte = firstByte or ((durationInMinutes.toInt() shr 8 and 0x01).toByte())
            return ByteBuffer.allocate(6)
                .put(firstByte)
                .put(durationInMinutes.toByte())
                .putShort(
                    when (trigger) {
                        is AlertTrigger.ReservoirVolumeTrigger -> trigger.thresholdInMicroLiters
                        is AlertTrigger.TimerTrigger -> trigger.offsetInMinutes
                    }
                )
                .put(beepRepetition.value)
                .put(beepType.value)
                .array()
        }
}

class ProgramReminder(
    val atStart: Boolean,
    val atEnd: Boolean,
    val atInterval: Byte
) : Encodable, Serializable {

    override val encoded: ByteArray
        get() = byteArrayOf(
            (
                (if (atStart) 1 else 0) shl 7
                    or ((if (atEnd) 1 else 0) shl 6)
                    or ((atInterval and 0x3f).toInt())
            ).toByte()
        )
}

class BasalProgram(
    segments: List<Segment>
) {
    val segments: MutableList<Segment> = segments.toMutableList()
        get() = Collections.unmodifiableList(field)

    fun addSegment(segment: Segment) {
        segments.add(segment)
    }

    fun hasZeroUnitSegments() = segments.any { it.basalRateInHundredthUnitsPerHour == 0 }

    fun rateAt(date: Long): Double {
        val instance = Calendar.getInstance()
        instance.timeInMillis = date
        val hourOfDay = instance[Calendar.HOUR_OF_DAY]
        val minuteOfHour = instance[Calendar.MINUTE]
        val slotIndex = hourOfDay * 2 + minuteOfHour / 30
        val slot = segments.find { it.startSlotIndex <= slotIndex && slotIndex < it.endSlotIndex }
        return (slot?.basalRateInHundredthUnitsPerHour ?: 0).toDouble() / 100
    }

    class Segment(
        val startSlotIndex: Short,
        val endSlotIndex: Short,
        val basalRateInHundredthUnitsPerHour: Int
    ) {
        fun getPulsesPerHour(): Short {
            return (basalRateInHundredthUnitsPerHour * PULSES_PER_UNIT / 100).toShort()
        }

        fun getNumberOfSlots(): Short {
            return (endSlotIndex - startSlotIndex).toShort()
        }

        companion object {
            private const val PULSES_PER_UNIT: Byte = 20
        }
    }
}

enum class NakErrorType(override val value: Byte) : HasValue {
    FLASH_WRITE(0x01.toByte()),
    FLASH_ERASE(0x02.toByte()),
    FLASH_OPERATION(0x03.toByte()),
    FLASH_ADDR(0x04.toByte()),
    POD_STATE(0x05.toByte()),
    CRITICAL_VARIABLE(0x06.toByte()),
    ILLEGAL_PARAM(0x07.toByte()),
    BOLUS_CRITICAL_VAR(0x08.toByte()),
    INT_ILLEGAL_PARAM(0x09.toByte()),
    ILLEGAL_CHECKSUM(0x0a.toByte()),
    INVALID_MSG_LEN(0x0b.toByte()),
    PUMP_STATE(0x0c.toByte()),
    ILLEGAL_COMMAND(0x0d.toByte()),
    ILLEGAL_FILL_STATE(0x0e.toByte()),
    MAX_READWRITE_SIZE(0x0f.toByte()),
    ILLEGAL_READ_ADDRESS(0x10.toByte()),
    ILLEGAL_READ_MEM_TYPE(0x11.toByte()),
    INIT_POD(0x12.toByte()),
    ILLEGAL_CMD_STATE(0x13.toByte()),
    ILLEGAL_SECURITY_CODE(0x14.toByte()),
    POD_IN_ALARM(0x15.toByte()),
    COMD_NOT_SET(0x16.toByte()),
    ILLEGAL_RX_SENS_VALUE(0x17.toByte()),
    ILLEGAL_TX_PKT_SIZE(0x18.toByte()),
    OCCL_PARAMS_ALREADY_SET(0x19.toByte()),
    OCCL_PARAM(0x1a.toByte()),
    ILLEGAL_CDTHR_VALUE(0x1b.toByte()),
    IGNORE_COMMAND(0x1c.toByte()),
    INVALID_CRC(0x1d.toByte()),
    UNKNOWN(0xff.toByte())
}

enum class PdmFaultCategory(val tt: Int) {
    RESERVOIR_EMPTY(14),
    AUTO_OFF(15),
    POD_EXPIRED(16),
    OCCLUDED(17),
    POD_ERROR(19)
}

enum class AlarmType(override val value: Byte) : HasValue {
    NONE(0x00.toByte()),
    ALARM_PW_FLASH_ERASE(0x01.toByte()),
    ALARM_PW_FLASH_WRITE(0x02.toByte()),
    ALARM_BASAL_CKSUM(0x03.toByte()),
    ALARM_BASAL_PPULSE(0x04.toByte()),
    ALARM_BASAL_STEP(0x05.toByte()),
    ALARM_AUTO_WAKEUP_TIMEOUT(0x06.toByte()),
    ALARM_WIRE_OVERDRIVEN(0x07.toByte()),
    ALARM_BEEP_REP_INVALID_INDEX(0x08.toByte()),
    ALARM_INVALID_REP_PATTERN(0x09.toByte()),
    ALARM_TEMP_BASAL_STEP(0x0a.toByte()),
    ALARM_TEMP_BASAL_CKSUM(0x0b.toByte()),
    ALARM_BOLUS_OVERFLOW(0x0c.toByte()),
    ALARM_COP_RESET(0x0d.toByte()),
    ALARM_ILOP_RESET(0x0e.toByte()),
    ALARM_ILAD_RESET(0x0f.toByte()),
    ALARM_SAWCOP_RESET(0x10.toByte()),
    ALARM_BOLUS_STEP(0x11.toByte()),
    ALARM_LVD_RESET(0x12.toByte()),
    ALARM_INVALID_RF_MSG_LENGTH(0x13.toByte()),
    ALARM_OCCLUDED(0x14.toByte()),
    ALARM_BOLUSPROG_CHKSUM(0x15.toByte()),
    ALARM_BOLUS_LOG(0x16.toByte()),
    ALARM_CRITICAL_VAR(0x17.toByte()),
    ALARM_EMPTY_RESERVOIR(0x18.toByte()),
    ALARM_LOADERR(0x19.toByte()),
    ALARM_PSA_FAILURE(0x1a.toByte()),
    ALARM_TICKCNT_NOT_CLEARED(0x1b.toByte()),
    ALARM_PUMP_EXPIRED(0x1c.toByte()),
    ALARM_COMD_BIT_NOT_SET(0x1d.toByte()),
    ALARM_INVALID_COMD_SET(0x1e.toByte()),
    ALARM_ALERTS_ARRAY_CKSM(0x1f.toByte()),
    ALARM_UNIT_TEST(0x20.toByte()),
    ALARM_TICK_TIME_ERROR(0x21.toByte()),
    ALARM_CRITICAL_HAZARD(0x22.toByte()),
    ALARM_PIEZO_FREQ(0x23.toByte()),
    ALARM_TICKCNT_ERROR_RTC(0x24.toByte()),
    ALARM_TICK_FAILURE(0x25.toByte()),
    ALARM_INVALID(0x26.toByte()),
    ALARM_LUMP_ALERT_PROGRAM(0x27.toByte()),
    ALARM_INVALID_PASS_CODE(0x28.toByte()),
    ALARM_ALERT0(0x29.toByte()),
    ALARM_ALERT1(0x2a.toByte()),
    ALARM_ALERT2(0x2b.toByte()),
    ALARM_ALERT3(0x2c.toByte()),
    ALARM_ALERT4(0x2d.toByte()),
    ALARM_ALERT5(0x2e.toByte()),
    ALARM_ALERT6(0x2f.toByte()),
    ALARM_ALERT7(0x30.toByte()),
    ALARM_ILLEGAL_PUMP_STATE(0x31.toByte()),
    ALARM_COP_TEST_FAILURE(0x32.toByte()),
    ALARM_MCTF(0x33.toByte()),
    ALARM_ILLEGAL_RESET(0x34.toByte()),
    ALARM_VETO_NOT_SET(0x35.toByte()),
    ALARM_ILLEGAL_PIN_RESET(0x36.toByte()),
    ALARM_INVALID_BEEP_PATTERN(0x37.toByte()),
    ALARM_WIRE_STATE_MACHINE(0x38.toByte()),
    ALARM_VETO_TEST_DEFAULT(0x39.toByte()),
    ALARM_ALERT_INVALID_INDEX(0x3a.toByte()),
    ALARM_SAWCOP_TEST_FAIL(0x3b.toByte()),
    ALARM_MCUCOP_TEST_FAIL(0x3c.toByte()),
    ALARM_STEP_SENSOR_SHORTED(0x3d.toByte()),
    ALARM_FLASH_FAILURE(0x3e.toByte()),
    ALARM_SPARE63(0x3f.toByte()),
    ALARM_SS_OPEN_CNT_EXCEEDED(0x40.toByte()),
    ALARM_SS_EXCESSIVE_SUMMED(0x41.toByte()),
    ALARM_SS_MIN_PULSE_TRANSITION(0x42.toByte()),
    ALARM_SS_DEFAULT(0x43.toByte()),
    ALARM_OPEN_WIRE1(0x44.toByte()),
    ALARM_OPEN_WIRE2(0x45.toByte()),
    ALARM_LOADERR_FAILURE(0x46.toByte()),
    ALARM_SAW_VETO_FAILURE(0x47.toByte()),
    ALARM_BAD_RFM_CLOCK(0x48.toByte()),
    ALARM_BAD_TICK_HIGH(0x49.toByte()),
    ALARM_BAD_TICK_PERIOD(0x4a.toByte()),
    ALARM_BAD_TRIM_VALUE(0x4b.toByte()),
    ALARM_BAD_BUS_CLOCK(0x4c.toByte()),
    ALARM_BAD_CAL_MODE(0x4d.toByte()),
    ALARM_SAW_TRIM_ERROR(0x4e.toByte()),
    ALARM_RFM_CRYSTAL_ERROR(0x4f.toByte()),
    ALARM_CALST_TIMEOUT(0x50.toByte()),
    ALARM_TICKCNT_ERROR(0x51.toByte()),
    ALARM_BAD_RFM_XTAL_START(0x52.toByte()),
    ALARM_BAD_RX_SENSENSITIVITY(0x53.toByte()),
    ALARM_BAD_TX_PKT_SIZE(0x54.toByte()),
    ALARM_TICK_LOW_PHASE_EXCEEDED(0x55.toByte()),
    ALARM_TICK_HIGH_PHASE_EXCEEDED(0x56.toByte()),
    ALARM_OCCL_CRITVAR_FAIL(0x57.toByte()),
    ALARM_OCCL_PARAM(0x58.toByte()),
    ALARM_PROG_OCCL_FAIL(0x59.toByte()),
    ALARM_PW_TO_HIGH_FOR_OCCL_DET(0x5a.toByte()),
    ALARM_OCCL_CSUM(0x5b.toByte()),
    ALARM_PRIME_OPEN_CNT_TO_LOW(0x5c.toByte()),
    ALARM_BAD_RF_CDTHR(0x5d.toByte()),
    ALARM_FLASH_NOT_SECURE(0x5e.toByte()),
    ALARM_WIRE_TEST_OPEN_GROUND(0x5f.toByte()),
    ALARM_OCCL_STARTUP1(0x60.toByte()),
    ALARM_OCCL_STARTUP2(0x61.toByte()),
    ALARM_OCCL_EXCESS_TIMEOUTS1(0x62.toByte()),
    ALARM_OCCL_PARAM_INVALID(0x63.toByte()),
    ALARM_SPARE100(0x64.toByte()),
    ALARM_SPARE101(0x65.toByte()),
    ALARM_OCCL_EXCESS_TIMEOUTS2(0x66.toByte()),
    ALARM_OCCL_EXCESS_TIMEOUTS3(0x67.toByte()),
    ALARM_OCCL_NOISY_PULSE_WIDTHS(0x68.toByte()),
    ALARM_OCCL_AT_BOLUS_END(0x69.toByte()),
    ALARM_OCCL_ABOVE_THRESHOLD(0x6a.toByte()),
    ALARM_BASAL_UNDERINFUSION(0x80.toByte()),
    ALARM_BASAL_OVERINFUSION(0x81.toByte()),
    ALARM_TEMP_UNDERINFUSION(0x82.toByte()),
    ALARM_TEMP_OVERINFUSION(0x83.toByte()),
    ALARM_BOLUS_UNDERINFUSION(0x84.toByte()),
    ALARM_BOLUS_OVERINFUSION(0x85.toByte()),
    ALARM_BASAL_OVERINFUSION_PULSE(0x86.toByte()),
    ALARM_TEMP_OVERINFUSION_PULSE(0x87.toByte()),
    ALARM_BOLUS_OVERINFUSION_PULSE(0x88.toByte()),
    ALARM_IMMBOLUS_UNDERINFUSION_PULSE(0x89.toByte()),
    ALARM_EXTBOLUS_OVERINFUSION_PULSE(0x8a.toByte()),
    ALARM_PROGRAM_CSUM(0x8b.toByte()),
    ALARM_UNUSED_140(0x8c.toByte()),
    ALARM_UNRECOGNIZED_PULSE(0x8d.toByte()),
    ALARM_SYNC_WITHOUT_TEMP_ACTIVE(0x8e.toByte()),
    ALARM_INTERLOCK_LOAD(0x8f.toByte()),
    ALARM_ILLEGAL_CHAN_PARAM(0x90.toByte()),
    ALARM_BASAL_PULSE_CHAN_INACTIVE(0x91.toByte()),
    ALARM_TEMP_PULSE_CHAN_INACTIVE(0x92.toByte()),
    ALARM_BOLUS_PULSE_CHAN_INACTIVE(0x93.toByte()),
    ALARM_INT_SEMAPHORE_NOT_SET(0x94.toByte()),
    ALARM_ILLEGAL_INTERLOCK_CHAN(0x95.toByte()),
    ALARM_TERMINATE_BOLUS(0x96.toByte()),
    ALARM_OPEN_TRANSITIONS_COUNT(0x97.toByte()),
    ALARM_BLE_TO(0xa0.toByte()),
    ALARM_BLE_INITIATED(0xa1.toByte()),
    ALARM_BLE_UNK_ALARM(0xa2.toByte()),
    ALARM_BLE_IAAS(0xa6.toByte()),
    ALARM_CRC_FAILURE(0xa8.toByte()),
    ALARM_BLE_WD_PING_TIMEOUT(0xa9.toByte()),
    ALARM_BLE_EXCESSIVE_RESETS(0xaa.toByte()),
    ALARM_BLE_NAK_ERROR(0xab.toByte()),
    ALARM_BLE_REQ_HIGH_TIMEOUT(0xac.toByte()),
    ALARM_BLE_UNKNOWN_RESP(0xad.toByte()),
    ALARM_BLE_REQ_STUCK_HIGH(0xaf.toByte()),
    ALARM_BLE_STATE_MACHINE_1(0xb1.toByte()),
    ALARM_BLE_STATE_MACHINE_2(0xb2.toByte()),
    ALARM_BLE_ARB_LOST(0xb4.toByte()),
    ALARM_BLE_ER48_DUAL_NACK(0xc0.toByte()),
    ALARM_BLE_QN_EXCEED_MAX_RETRY(0xc1.toByte()),
    ALARM_BLE_QN_CRIT_VAR_FAIL(0xc2.toByte()),
    ALARM_BLE_QN_OPT_INTVL_INVALID(0xc3.toByte()),
    ALARM_UNKNOWN_CB(0xcb.toByte()),
    ALARM_UNKNOWN_D4(0xd4.toByte()),
    ALARM_UNKNOWN_D5(0xd5.toByte()),
    ALARM_RESET_FAULT_D6(0xd6.toByte()),
    ALARM_RESET_FAULT_D7(0xd7.toByte()),
    ALARM_UNKNOWN_D8(0xd8.toByte()),
    ALARM_UNKNOWN_D9(0xd9.toByte()),
    UNKNOWN(0xff.toByte());

    val code: Int get() = value.toInt() and 0xff

    val pdmFaultCategory: PdmFaultCategory?
        get() = when (this) {
            NONE, UNKNOWN -> null
            ALARM_EMPTY_RESERVOIR -> PdmFaultCategory.RESERVOIR_EMPTY
            ALARM_ALERT0, ALARM_ALERT1, ALARM_ALERT2, ALARM_ALERT3,
            ALARM_ALERT4, ALARM_ALERT5, ALARM_ALERT6, ALARM_ALERT7 -> PdmFaultCategory.AUTO_OFF
            ALARM_PUMP_EXPIRED -> PdmFaultCategory.POD_EXPIRED
            ALARM_OCCLUDED -> PdmFaultCategory.OCCLUDED
            else -> PdmFaultCategory.POD_ERROR
        }
}