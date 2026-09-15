package de.dh.daps.common.model

import android.content.Context
import de.dh.daps.common.R
import de.dh.daps.common.model.data.AlarmProfile
import de.dh.daps.common.model.data.AlarmSeverity
import de.dh.daps.common.model.data.AlarmSoundConfig
import de.dh.daps.common.model.data.VibrationMode

/**
 * Provides default alarm profiles for initial system population.
 */
fun getDefaultAlarmProfiles(context: Context): List<AlarmProfile> {
    val standardDefaults = mapOf(
        AlarmSeverity.CRITICAL to AlarmSoundConfig(
            volume = 100,
            vibrationMode = VibrationMode.CONTINUOUS,
            overrideDnd = true,
            showFullScreen = true,
        ),
        AlarmSeverity.WARNING to AlarmSoundConfig(
            volume = 70,
            vibrationMode = VibrationMode.LONG,
            overrideDnd = false,
            showFullScreen = false,
        ),
        AlarmSeverity.INFO to AlarmSoundConfig(
            volume = 40,
            vibrationMode = VibrationMode.SHORT,
            overrideDnd = false,
            showFullScreen = false,
        ),
    )

    val quietDefaults = mapOf(
        AlarmSeverity.CRITICAL to AlarmSoundConfig(
            volume = 100,
            vibrationMode = VibrationMode.CONTINUOUS,
            overrideDnd = true,
            showFullScreen = true,
        ),
        AlarmSeverity.WARNING to AlarmSoundConfig(
            volume = 30,
            vibrationMode = VibrationMode.SHORT,
            overrideDnd = false,
            showFullScreen = false,
        ),
        AlarmSeverity.INFO to AlarmSoundConfig(
            volume = 0,
            vibrationMode = VibrationMode.OFF,
            overrideDnd = false,
            showFullScreen = false,
        ),
    )

    val loudDefaults = mapOf(
        AlarmSeverity.CRITICAL to AlarmSoundConfig(
            volume = 100,
            vibrationMode = VibrationMode.CONTINUOUS,
            overrideDnd = true,
            showFullScreen = true,
        ),
        AlarmSeverity.WARNING to AlarmSoundConfig(
            volume = 100,
            vibrationMode = VibrationMode.LONG,
            overrideDnd = true,
            showFullScreen = false,
        ),
        AlarmSeverity.INFO to AlarmSoundConfig(
            volume = 80,
            vibrationMode = VibrationMode.SHORT,
            overrideDnd = false,
            showFullScreen = false,
        ),
    )

    return listOf(
        AlarmProfile(
            id = ID_UNDEFINED,
            name = context.getString(R.string.alarm_profile_default_standard_name),
            isDefault = true,
            severityDefaults = standardDefaults,
        ),
        AlarmProfile(
            id = ID_UNDEFINED,
            name = context.getString(R.string.alarm_profile_default_quiet_name),
            isDefault = false,
            severityDefaults = quietDefaults,
        ),
        AlarmProfile(
            id = ID_UNDEFINED,
            name = context.getString(R.string.alarm_profile_default_loud_name),
            isDefault = false,
            severityDefaults = loudDefaults,
        ),
    )
}