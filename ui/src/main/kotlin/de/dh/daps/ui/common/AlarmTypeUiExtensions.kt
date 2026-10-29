package de.dh.daps.ui.common

import androidx.annotation.StringRes
import de.dh.daps.common.domain.model.alarm.AlarmType
import de.dh.daps.ui.R

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
        AlarmType.PUMP_PATCH_CHANGE_WARN -> R.string.alarm_type_pump_patch_change_warn
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
        AlarmType.HIGH_BG -> null
        AlarmType.PUMP_OCCLUSION -> R.string.alarm_type_pump_occlusion_desc
        AlarmType.PUMP_SUSPENDED -> R.string.alarm_type_pump_suspended_desc
        AlarmType.PUMP_LOW_INSULIN -> R.string.alarm_type_pump_low_insulin_desc
        AlarmType.PUMP_LOW_BATTERY -> R.string.alarm_type_pump_low_battery_desc
        AlarmType.PUMP_STALE_BOND -> R.string.alarm_type_pump_stale_bond_desc
        AlarmType.PUMP_CATHETER_CHANGE_WARN -> R.string.alarm_type_pump_catheter_change_warn_desc
        AlarmType.PUMP_PATCH_CHANGE_WARN -> R.string.alarm_type_pump_patch_change_warn_desc
        AlarmType.CGM_SIGNAL_LOSS -> R.string.core_issue_no_recent_values
        AlarmType.CGM_EXPIRED -> R.string.glucose_source_issue_expired
        AlarmType.CGM_SENSOR_ERROR -> R.string.glucose_source_issue_sensor_error
        AlarmType.CGM_STALE_BOND -> R.string.glucose_source_issue_stale_bluetooth_bond
        AlarmType.SYSTEM_BATTERY_LOW -> R.string.alarm_type_system_battery_low
        AlarmType.SYSTEM_BLUETOOTH_OFF -> R.string.alarm_type_system_bluetooth_off_desc
        AlarmType.SYSTEM_THERAPY_SETTINGS_MISSING -> R.string.alarm_type_system_therapy_settings_missing_desc
    }