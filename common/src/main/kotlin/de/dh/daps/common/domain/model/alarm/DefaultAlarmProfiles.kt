package de.dh.daps.common.domain.model.alarm

import android.content.Context
import de.dh.daps.common.ID_UNDEFINED
import de.dh.daps.common.R

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

    val silentDefaults = mapOf(
        AlarmSeverity.CRITICAL to AlarmSignalConfig(
            displayMode = AlertDisplayMode.NotificationOnly,
            vibrationMode = VibrationMode.OFF,
            overrideDnd = false
        ),
        AlarmSeverity.WARNING to AlarmSignalConfig(
            displayMode = AlertDisplayMode.NotificationOnly,
            vibrationMode = VibrationMode.OFF,
            overrideDnd = false
        ),
        AlarmSeverity.INFO to AlarmSignalConfig(
            displayMode = AlertDisplayMode.NotificationOnly,
            vibrationMode = VibrationMode.OFF,
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
        ),
        AlarmProfile(
            id = ID_UNDEFINED,
            name = context.getString(R.string.alarm_profile_default_silent_name),
            isDefault = false,
            severityDefaults = silentDefaults
        )
    )
}