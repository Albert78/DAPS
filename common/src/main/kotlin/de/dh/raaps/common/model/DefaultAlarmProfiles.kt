package de.dh.raaps.common.model

import de.dh.raaps.common.model.data.AlarmProfile
import de.dh.raaps.common.model.data.AlarmSeverity
import de.dh.raaps.common.model.data.AlarmSoundConfig
import de.dh.raaps.common.model.data.VibrationMode

/**
 * Provides default alarm profiles for initial system population.
 */
fun getDefaultAlarmProfiles(): List<AlarmProfile> {
    val standardDefaults = mapOf(
        AlarmSeverity.CRITICAL to AlarmSoundConfig(
            volume = 100,
            vibrationMode = VibrationMode.CONTINUOUS,
            overrideDnd = true
        ),
        AlarmSeverity.WARNING to AlarmSoundConfig(
            volume = 70,
            vibrationMode = VibrationMode.LONG,
            overrideDnd = false
        ),
        AlarmSeverity.INFO to AlarmSoundConfig(
            volume = 40,
            vibrationMode = VibrationMode.SHORT,
            overrideDnd = false
        )
    )

    val quietDefaults = mapOf(
        AlarmSeverity.CRITICAL to AlarmSoundConfig(
            volume = 100,
            vibrationMode = VibrationMode.CONTINUOUS,
            overrideDnd = true
        ),
        AlarmSeverity.WARNING to AlarmSoundConfig(
            volume = 30,
            vibrationMode = VibrationMode.SHORT,
            overrideDnd = false
        ),
        AlarmSeverity.INFO to AlarmSoundConfig(
            volume = 0,
            vibrationMode = VibrationMode.OFF,
            overrideDnd = false
        )
    )

    val loudDefaults = mapOf(
        AlarmSeverity.CRITICAL to AlarmSoundConfig(
            volume = 100,
            vibrationMode = VibrationMode.CONTINUOUS,
            overrideDnd = true
        ),
        AlarmSeverity.WARNING to AlarmSoundConfig(
            volume = 100,
            vibrationMode = VibrationMode.LONG,
            overrideDnd = true
        ),
        AlarmSeverity.INFO to AlarmSoundConfig(
            volume = 80,
            vibrationMode = VibrationMode.SHORT,
            overrideDnd = false
        )
    )

    return listOf(
        AlarmProfile(
            id = ID_UNDEFINED,
            name = "Standard",
            isDefault = true,
            severityDefaults = standardDefaults
        ),
        AlarmProfile(
            id = ID_UNDEFINED,
            name = "Kino / Diskret",
            isDefault = false,
            severityDefaults = quietDefaults
        ),
        AlarmProfile(
            id = ID_UNDEFINED,
            name = "Laut / Draussen",
            isDefault = false,
            severityDefaults = loudDefaults
        )
    )
}