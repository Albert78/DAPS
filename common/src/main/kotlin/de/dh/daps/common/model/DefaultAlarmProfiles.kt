package de.dh.daps.common.model

import android.content.Context
import de.dh.daps.common.R
import de.dh.daps.common.model.data.AlarmProfile
import de.dh.daps.common.model.data.AlarmSeverity
import de.dh.daps.common.model.data.AlarmSignalConfig
import de.dh.daps.common.model.data.AlertDisplayMode
import de.dh.daps.common.model.data.SoundConfig
import de.dh.daps.common.model.data.VibrationMode

/**
 * Provides default alarm profiles for initial system population.
 */
fun getDefaultAlarmProfiles(context: Context): List<AlarmProfile> {
    val standardDefaults = mapOf(
        AlarmSeverity.CRITICAL to AlarmSignalConfig(
            displayMode = AlertDisplayMode.FullScreen(SoundConfig(volume = 100)),
            vibrationMode = VibrationMode.CONTINUOUS,
            overrideDnd = true
        ),
        AlarmSeverity.WARNING to AlarmSignalConfig(
            displayMode = AlertDisplayMode.NotificationOnly,
            vibrationMode = VibrationMode.LONG,
            overrideDnd = false
        ),
        AlarmSeverity.INFO to AlarmSignalConfig(
            displayMode = AlertDisplayMode.NotificationOnly,
            vibrationMode = VibrationMode.SHORT,
            overrideDnd = false
        )
    )

    val quietDefaults = mapOf(
        AlarmSeverity.CRITICAL to AlarmSignalConfig(
            displayMode = AlertDisplayMode.FullScreen(SoundConfig(volume = 100)),
            vibrationMode = VibrationMode.CONTINUOUS,
            overrideDnd = true
        ),
        AlarmSeverity.WARNING to AlarmSignalConfig(
            displayMode = AlertDisplayMode.NotificationOnly,
            vibrationMode = VibrationMode.SHORT,
            overrideDnd = false
        ),
        AlarmSeverity.INFO to AlarmSignalConfig(
            displayMode = AlertDisplayMode.NotificationOnly,
            vibrationMode = VibrationMode.OFF,
            overrideDnd = false
        )
    )

    val loudDefaults = mapOf(
        AlarmSeverity.CRITICAL to AlarmSignalConfig(
            displayMode = AlertDisplayMode.FullScreen(SoundConfig(volume = 100)),
            vibrationMode = VibrationMode.CONTINUOUS,
            overrideDnd = true
        ),
        AlarmSeverity.WARNING to AlarmSignalConfig(
            displayMode = AlertDisplayMode.NotificationOnly,
            vibrationMode = VibrationMode.LONG,
            overrideDnd = true
        ),
        AlarmSeverity.INFO to AlarmSignalConfig(
            displayMode = AlertDisplayMode.NotificationOnly,
            vibrationMode = VibrationMode.SHORT,
            overrideDnd = false
        )
    )

    return listOf(
        AlarmProfile(
            id = ID_UNDEFINED,
            name = context.getString(R.string.alarm_profile_default_standard_name),
            isDefault = true,
            severityDefaults = standardDefaults
        ),
        AlarmProfile(
            id = ID_UNDEFINED,
            name = context.getString(R.string.alarm_profile_default_quiet_name),
            isDefault = false,
            severityDefaults = quietDefaults
        ),
        AlarmProfile(
            id = ID_UNDEFINED,
            name = context.getString(R.string.alarm_profile_default_loud_name),
            isDefault = false,
            severityDefaults = loudDefaults
        )
    )
}