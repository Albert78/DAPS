package de.dh.daps.common.domain.model.alarm

import android.content.Context
import de.dh.daps.common.ID_UNDEFINED
import de.dh.daps.common.R

/**
 * Creates system default alert profiles.
 * Uses the passed Context to resolve localized profile names from string resources.
 */
fun getDefaultAlertProfiles(context: Context): List<AlertProfile> {
    val standardAlarmDefaults = mapOf(
        AlarmSeverity.CRITICAL to AlertSignalConfig(
            displayMode = AlertDisplayMode.FullScreen(SoundConfig(volume = 100)),
            vibrationMode = VibrationMode.CONTINUOUS,
            overrideDnd = true
        ),
        AlarmSeverity.WARNING to AlertSignalConfig(
            displayMode = AlertDisplayMode.NotificationOnly,
            vibrationMode = VibrationMode.LONG
        ),
        AlarmSeverity.INFO to AlertSignalConfig(
            displayMode = AlertDisplayMode.NotificationOnly,
            vibrationMode = VibrationMode.SHORT
        )
    )

    val quietAlarmDefaults = mapOf(
        AlarmSeverity.CRITICAL to AlertSignalConfig(
            displayMode = AlertDisplayMode.FullScreen(SoundConfig(volume = 100)),
            vibrationMode = VibrationMode.CONTINUOUS,
            overrideDnd = true
        ),
        AlarmSeverity.WARNING to AlertSignalConfig(
            displayMode = AlertDisplayMode.NotificationOnly,
            vibrationMode = VibrationMode.SHORT
        ),
        AlarmSeverity.INFO to AlertSignalConfig(
            displayMode = AlertDisplayMode.NotificationOnly,
            vibrationMode = VibrationMode.OFF
        )
    )

    val loudAlarmDefaults = mapOf(
        AlarmSeverity.CRITICAL to AlertSignalConfig(
            displayMode = AlertDisplayMode.FullScreen(SoundConfig(volume = 100)),
            vibrationMode = VibrationMode.CONTINUOUS,
            overrideDnd = true
        ),
        AlarmSeverity.WARNING to AlertSignalConfig(
            displayMode = AlertDisplayMode.NotificationOnly,
            vibrationMode = VibrationMode.LONG,
            overrideDnd = true
        ),
        AlarmSeverity.INFO to AlertSignalConfig(
            displayMode = AlertDisplayMode.NotificationOnly,
            vibrationMode = VibrationMode.SHORT
        )
    )

    val silentAlarmDefaults = mapOf(
        AlarmSeverity.CRITICAL to AlertSignalConfig(
            displayMode = AlertDisplayMode.NotificationOnly,
            vibrationMode = VibrationMode.OFF
        ),
        AlarmSeverity.WARNING to AlertSignalConfig(enabled = false),
        AlarmSeverity.INFO to AlertSignalConfig(enabled = false)
    )

    return listOf(
        AlertProfile(
            id = ID_UNDEFINED,
            name = context.getString(R.string.alarm_profile_default_standard_name),
            isDefault = true,
            alarmDefaults = standardAlarmDefaults,
            recommendationDefault = AlertSignalConfig(
                displayMode = AlertDisplayMode.NotificationOnly,
                vibrationMode = VibrationMode.SHORT
            ),
            reminderDefault = AlertSignalConfig(
                displayMode = AlertDisplayMode.NotificationOnly,
                vibrationMode = VibrationMode.SHORT
            )
        ),
        AlertProfile(
            id = ID_UNDEFINED,
            name = context.getString(R.string.alarm_profile_default_quiet_name),
            isDefault = false,
            alarmDefaults = quietAlarmDefaults,
            recommendationDefault = AlertSignalConfig(
                displayMode = AlertDisplayMode.NotificationOnly,
                vibrationMode = VibrationMode.SHORT
            ),
            reminderDefault = AlertSignalConfig(
                displayMode = AlertDisplayMode.NotificationOnly,
                vibrationMode = VibrationMode.SHORT
            )
        ),
        AlertProfile(
            id = ID_UNDEFINED,
            name = context.getString(R.string.alarm_profile_default_loud_name),
            isDefault = false,
            alarmDefaults = loudAlarmDefaults,
            recommendationDefault = AlertSignalConfig(
                displayMode = AlertDisplayMode.NotificationOnly,
                vibrationMode = VibrationMode.LONG
            ),
            reminderDefault = AlertSignalConfig(
                displayMode = AlertDisplayMode.NotificationOnly,
                vibrationMode = VibrationMode.SHORT
            )
        ),
        AlertProfile(
            id = ID_UNDEFINED,
            name = context.getString(R.string.alarm_profile_default_silent_name),
            isDefault = false,
            alarmDefaults = silentAlarmDefaults,
            recommendationDefault = AlertSignalConfig(enabled = false),
            reminderDefault = AlertSignalConfig(enabled = false)
        )
    )
}