package de.dh.raaps.common.model.data

import de.dh.raaps.common.model.ID_UNDEFINED

/**
 * Defines a set of alarm configurations.
 * Contains defaults per [AlarmSeverity] and optional overrides for specific [AlarmType]s.
 */
data class AlarmProfile(
    var id: Long = ID_UNDEFINED,
    val name: String,
    val isDefault: Boolean = false,
    val severityDefaults: Map<AlarmSeverity, AlarmSoundConfig> = emptyMap(),
    val customOverrides: Map<AlarmType, AlarmSoundConfig> = emptyMap()
) {
    /**
     * Resolves the effective [AlarmSoundConfig] for a given [AlarmType].
     */
    fun getConfigFor(alarmType: AlarmType): AlarmSoundConfig {
        val override = customOverrides[alarmType]
        if (override != null) return override

        val defaultForSeverity = severityDefaults[alarmType.defaultSeverity]
        if (defaultForSeverity != null) return defaultForSeverity

        return AlarmSoundConfig()
    }
}