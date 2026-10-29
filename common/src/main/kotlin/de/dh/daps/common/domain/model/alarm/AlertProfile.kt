package de.dh.daps.common.domain.model.alarm

import de.dh.daps.common.ID_UNDEFINED
import de.dh.daps.common.domain.model.reminder.Reminder
import de.dh.daps.common.domain.model.reminder.ReminderType

enum class RecommendationType {
    CARBS,
    BOLUS,
    TEMP_BASAL
}

/**
 * Comprehensive alert profile.
 * Encapsulates signal configurations and type-specific overrides for alarms, APS recommendations, and reminders.
 */
data class AlertProfile(
    var id: Long = ID_UNDEFINED,
    val name: String = "",
    val isDefault: Boolean = false,

    // 1. Alarms: Severity defaults & specific AlarmType overrides
    val alarmDefaults: Map<AlarmSeverity, AlertSignalConfig> = emptyMap(),
    val alarmOverrides: Map<AlarmType, AlertSignalConfig> = emptyMap(),

    // 2. APS Recommendations: Default & specific RecommendationType overrides
    val recommendationDefault: AlertSignalConfig = AlertSignalConfig(),
    val recommendationOverrides: Map<RecommendationType, AlertSignalConfig> = emptyMap(),

    // 3. Reminders: Default & specific ReminderType overrides
    val reminderDefault: AlertSignalConfig = AlertSignalConfig(),
    val reminderOverrides: Map<ReminderType, AlertSignalConfig> = emptyMap()
) {
    /**
     * Resolves the effective signal configuration for a specific [AlarmType].
     */
    fun getConfigFor(alarmType: AlarmType): AlertSignalConfig {
        return alarmOverrides[alarmType]
            ?: alarmDefaults[alarmType.defaultSeverity]
            ?: AlertSignalConfig()
    }

    /**
     * Resolves the effective signal configuration for a specific [RecommendationType].
     */
    fun getConfigFor(recommendationType: RecommendationType): AlertSignalConfig {
        return recommendationOverrides[recommendationType] ?: recommendationDefault
    }

    /**
     * Resolves the effective signal configuration for a specific [Reminder] instance.
     */
    fun getConfigFor(reminder: Reminder): AlertSignalConfig {
        return reminderOverrides[reminder.type] ?: reminderDefault
    }
}