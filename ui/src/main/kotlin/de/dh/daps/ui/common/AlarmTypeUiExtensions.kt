package de.dh.daps.ui.common

import android.content.Context
import androidx.annotation.StringRes
import de.dh.daps.common.domain.model.alarm.ActiveAlarm
import de.dh.daps.common.domain.model.alarm.AlarmType
import de.dh.daps.common.domain.model.data.GlucoseUnit
import de.dh.daps.ui.R
import de.dh.daps.ui.common.formatters.formatInsulinValue
import de.dh.daps.ui.common.formatters.formatRelativeTime
import de.dh.daps.ui.common.formatters.formatRelativeTimeAgo
import de.dh.daps.ui.common.formatters.formatShortDateTime
import de.dh.daps.ui.common.formatters.glucoseValue
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Returns the string resource ID for the title of the given [AlarmType].
 */
@get:StringRes
val AlarmType.titleResId: Int
    get() = when (this) {
        AlarmType.CRITICAL_LOW_BG -> R.string.alarm_type_critical_low_bg
        AlarmType.LOW_BG -> R.string.alarm_type_low_bg
        AlarmType.HIGH_BG -> R.string.alarm_type_high_bg
        AlarmType.PUMP_OCCLUSION -> R.string.alarm_type_pump_occlusion
        AlarmType.PUMP_SUSPENDED -> R.string.alarm_type_pump_suspended
        AlarmType.PUMP_LOW_INSULIN -> R.string.alarm_type_pump_low_insulin
        AlarmType.PUMP_LOW_BATTERY -> R.string.alarm_type_pump_low_battery
        AlarmType.PUMP_STALE_BOND -> R.string.alarm_type_pump_stale_bond
        AlarmType.PUMP_CATHETER_CHANGE_WARN -> R.string.alarm_type_pump_catheter_change_warn
        AlarmType.PUMP_CATHETER_CHANGE_EXPIRED -> R.string.alarm_type_pump_catheter_change_expired
        AlarmType.PUMP_PATCH_CHANGE_WARN -> R.string.alarm_type_pump_patch_change_warn
        AlarmType.PUMP_PATCH_CHANGE_EXPIRED -> R.string.alarm_type_pump_patch_change_expired
        AlarmType.PUMP_MAINTENANCE_EXCEEDED -> R.string.alarm_type_pump_maintenance_exceeded
        AlarmType.PUMP_COORDINATOR_ERROR -> R.string.alarm_type_pump_coordinator_error
        AlarmType.CGM_SIGNAL_LOSS -> R.string.alarm_type_cgm_signal_loss
        AlarmType.CGM_EXPIRED -> R.string.alarm_type_cgm_expired
        AlarmType.CGM_SENSOR_ERROR -> R.string.alarm_type_cgm_sensor_error
        AlarmType.CGM_STALE_BOND -> R.string.alarm_type_cgm_stale_bond
        AlarmType.SYSTEM_BATTERY_LOW -> R.string.alarm_type_system_battery_low
        AlarmType.SYSTEM_BLUETOOTH_OFF -> R.string.alarm_type_system_bluetooth_off
        AlarmType.SYSTEM_THERAPY_SETTINGS_MISSING -> R.string.alarm_type_system_therapy_settings_missing
    }

/**
 * Returns the string resource ID for the description/details of the given [AlarmType],
 * or null if the alarm type does not have a static description string (e.g. glucose alarms).
 */
@get:StringRes
val AlarmType.descriptionResId: Int?
    get() = when (this) {
        AlarmType.CRITICAL_LOW_BG,
        AlarmType.LOW_BG,
        AlarmType.HIGH_BG,
        AlarmType.CGM_SIGNAL_LOSS,
        AlarmType.PUMP_MAINTENANCE_EXCEEDED,
        AlarmType.PUMP_LOW_INSULIN,
        AlarmType.PUMP_CATHETER_CHANGE_WARN,
        AlarmType.PUMP_CATHETER_CHANGE_EXPIRED,
        AlarmType.PUMP_PATCH_CHANGE_WARN,
        AlarmType.PUMP_PATCH_CHANGE_EXPIRED -> null
        AlarmType.PUMP_OCCLUSION -> R.string.alarm_type_pump_occlusion_desc
        AlarmType.PUMP_SUSPENDED -> R.string.alarm_type_pump_suspended_desc
        AlarmType.PUMP_LOW_BATTERY -> R.string.alarm_type_pump_low_battery_desc
        AlarmType.PUMP_STALE_BOND -> R.string.alarm_type_pump_stale_bond_desc
        AlarmType.PUMP_COORDINATOR_ERROR -> R.string.alarm_type_pump_coordinator_error_desc
        AlarmType.CGM_EXPIRED -> R.string.alarm_type_cgm_expired_desc
        AlarmType.CGM_SENSOR_ERROR -> R.string.alarm_type_cgm_sensor_error_desc
        AlarmType.CGM_STALE_BOND -> R.string.glucose_source_issue_stale_bluetooth_bond
        AlarmType.SYSTEM_BATTERY_LOW -> R.string.alarm_type_system_battery_low_desc
        AlarmType.SYSTEM_BLUETOOTH_OFF -> R.string.alarm_type_system_bluetooth_off_desc
        AlarmType.SYSTEM_THERAPY_SETTINGS_MISSING -> R.string.alarm_type_system_therapy_settings_missing_desc
    }

/**
 * Returns the string resource ID for the title of the given [ActiveAlarm].
 */
@get:StringRes
val ActiveAlarm.titleResId: Int
    get() = type.titleResId

/**
 * Returns the string resource ID for the description/details of the given [ActiveAlarm].
 */
@get:StringRes
val ActiveAlarm.descriptionResId: Int?
    get() = type.descriptionResId

/**
 * Returns the localized formatted description text for the given [ActiveAlarm],
 * resolving any dynamic payload details (such as remaining insulin amount, expiration dates,
 * or stale duration).
 *
 * @param context The Android [Context] used to resolve string resources.
 * @param includeGlucoseValue Whether to format glucose alarms into a text description (e.g. for notifications).
 * @param unit Glucose unit used when formatting glucose alarms.
 */
fun ActiveAlarm.getFormattedDescription(
    context: Context,
    includeGlucoseValue: Boolean = false,
    unit: GlucoseUnit = GlucoseUnit.MG_DL
): String? = when (this) {
    is ActiveAlarm.GlucoseAlarm -> {
        if (includeGlucoseValue && bgValue.isValid()) {
            glucoseValue(bgValue, context, unit = unit, withUnit = true)
        } else null
    }
    is ActiveAlarm.CgmSignalLoss -> {
        context.getString(R.string.aps_issue_stale_bg, minutes)
    }
    is ActiveAlarm.PumpMaintenanceExceeded -> {
        context.getString(R.string.alarm_type_pump_maintenance_exceeded_desc, minutes)
    }
    is ActiveAlarm.PumpLowInsulin -> {
        val formattedAmount = formatInsulinValue(amount, context)
        context.getString(R.string.alarm_type_pump_low_insulin_desc, formattedAmount)
    }
    is ActiveAlarm.PumpCatheterChangeWarn -> {
        val dateStr = recommendedChangeDate?.let { formatRelativeTime(it, context) }
        if (dateStr != null) {
            context.getString(R.string.alarm_type_pump_catheter_change_warn_desc_with_date, dateStr)
        } else {
            context.getString(R.string.alarm_type_pump_catheter_change_warn_desc)
        }
    }
    is ActiveAlarm.PumpCatheterChangeExpired -> {
        val dateStr = recommendedChangeDate?.let { formatRelativeTime(it, context) }
        if (dateStr != null) {
            context.getString(R.string.alarm_type_pump_catheter_change_expired_desc_with_date, dateStr)
        } else {
            context.getString(R.string.alarm_type_pump_catheter_change_expired_desc)
        }
    }
    is ActiveAlarm.PumpPatchChangeWarn -> {
        val dateTimeStr = expirationTimestamp?.let { if (it.isValid()) formatShortDateTime(
            it,
            context
        ) else null }
        if (dateTimeStr != null) {
            context.getString(R.string.alarm_type_pump_patch_change_warn_desc_with_date, dateTimeStr)
        } else {
            context.getString(R.string.alarm_type_pump_patch_change_warn_desc)
        }
    }
    is ActiveAlarm.PumpPatchChangeExpired -> {
        val dateTimeStr = expirationTimestamp?.let { timestamp ->
            if (timestamp.isValid()) {
                val expDate = Instant.ofEpochMilli(timestamp.ms).atZone(ZoneId.systemDefault()).toLocalDate()
                if (expDate == LocalDate.now()) {
                    formatRelativeTimeAgo(timestamp, context)
                } else {
                    formatRelativeTime(expDate, context)
                }
            } else null
        }
        if (dateTimeStr != null) {
            context.getString(R.string.alarm_type_pump_patch_change_expired_desc_with_date, dateTimeStr)
        } else {
            context.getString(R.string.alarm_type_pump_patch_change_expired_desc)
        }
    }
    else -> descriptionResId?.let { context.getString(it) }
}