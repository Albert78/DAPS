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