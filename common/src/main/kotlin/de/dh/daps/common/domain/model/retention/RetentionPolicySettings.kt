package de.dh.daps.common.domain.model.retention

import de.dh.daps.common.domain.model.data.Timestamp

/**
 * System-wide data retention settings per category.
 */
data class RetentionPolicySettings(
    val glucoseReadings: RetentionOption = RetentionOption.DAYS_30,
    val therapyHistory: RetentionOption = RetentionOption.DAYS_90,
    val deviceHardwareLogs: RetentionOption = RetentionOption.DAYS_7,
    val reminders: RetentionOption = RetentionOption.DAYS_3,
    val systemMetrics: RetentionOption = RetentionOption.DAYS_3,
    val other: RetentionOption = RetentionOption.DAYS_30
) {
    fun getOption(category: DataRetentionCategory): RetentionOption = when (category) {
        DataRetentionCategory.GLUCOSE_READINGS -> glucoseReadings
        DataRetentionCategory.THERAPY_HISTORY -> therapyHistory
        DataRetentionCategory.DEVICE_HARDWARE_LOGS -> deviceHardwareLogs
        DataRetentionCategory.REMINDERS -> reminders
        DataRetentionCategory.SYSTEM_METRICS -> systemMetrics
        DataRetentionCategory.OTHER -> other
    }

    /**
     * Computes the cutoff timestamp map for all categories based on the given reference [now] time.
     */
    fun computeCutoffs(now: Timestamp): Map<DataRetentionCategory, Timestamp?> {
        return DataRetentionCategory.entries.associateWith { category ->
            getOption(category).calculateCutoff(now)
        }
    }
}