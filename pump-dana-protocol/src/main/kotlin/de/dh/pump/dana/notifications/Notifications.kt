package de.dh.pump.dana.notifications

import de.dh.pump.PumpStatus
import de.dh.pump.dana.commands.DanaRsResponse

/**
 * Technical alarm codes sent by the Dana pump.
 */
enum class DanaAlarmCode(val code: Int) {
    BATTERY_DISCHARGED(0x01),
    PUMP_ERROR(0x02),
    OCCLUSION(0x03),
    PUMP_SHUTDOWN(0x04),
    LOW_BATTERY(0x05),
    BASAL_COMPARE(0x06),
    BLOOD_SUGAR_MEASUREMENT_ALERT(0x07),
    REMAINING_INSULIN_ALERT(0x08),
    EMPTY_RESERVOIR(0x09),
    CHECK_SHAFT(0x0A),
    BASAL_MAX(0x0B),
    DAILY_MAX(0x0C),
    BLOOD_SUGAR_CHECK_MISS_ALARM(0xFD),
    UNKNOWN(-1);

    companion object {
        fun fromCode(code: Int): DanaAlarmCode {
            // Handle aliases from AAPS implementation
            val effectiveCode = when (code) {
                0xFF -> 0x07
                0xFE -> 0x08
                else -> code
            }
            return entries.find { it.code == effectiveCode } ?: UNKNOWN
        }
    }
}

/**
 * Marker interface for all asynchronous messages sent by the Dana pump.
 */
interface DanaNotification : DanaRsResponse

/**
 * Async pump alarm notification.
 */
data class DanaAlarmNotification(
    override val status: PumpStatus = PumpStatus.OK,
    /**
     * Technical alarm code.
     */
    val alarmCode: DanaAlarmCode,
) : DanaNotification

/**
 * Notification sent when a bolus delivery has finished.
 */
data class DanaDeliveryCompleteNotification(
    override val status: PumpStatus = PumpStatus.OK,
    /**
     * Total amount delivered in Units (U).
     */
    val deliveredInsulinUnits: Double,
) : DanaNotification

/**
 * Periodic status update during an active bolus delivery.
 */
data class DanaDeliveryRateDisplayNotification(
    override val status: PumpStatus = PumpStatus.OK,
    /**
     * Amount delivered so far in Units (U).
     */
    val deliveredInsulinUnits: Double,
) : DanaNotification

/**
 * Notification for a missed bolus reminder.
 */
data class DanaMissedBolusAlarmNotification(
    override val status: PumpStatus = PumpStatus.OK,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
) : DanaNotification