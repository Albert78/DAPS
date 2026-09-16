package de.dh.daps.common.model.data

import de.dh.daps.common.model.ID_UNDEFINED

/**
 * Defines a set of alarm configurations.
 * Contains defaults per [AlarmSeverity] and optional overrides for specific [AlarmType]s.
 */
data class AlarmProfile(
    var id: Long = ID_UNDEFINED,
    val name: String,
    val isDefault: Boolean = false,
    val severityDefaults: Map<AlarmSeverity, AlarmSignalConfig> = emptyMap(),
    val customOverrides: Map<AlarmType, AlarmSignalConfig> = emptyMap()
) {
    /**
     * Resolves the effective [AlarmSignalConfig] for a given [AlarmType].
     */
    fun getConfigFor(alarmType: AlarmType): AlarmSignalConfig {
        val override = customOverrides[alarmType]
        if (override != null) return override

        val defaultForSeverity = severityDefaults[alarmType.defaultSeverity]
        if (defaultForSeverity != null) return defaultForSeverity

        return AlarmSignalConfig()
    }
}