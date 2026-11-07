package de.dh.daps.common.domain.model.retention

/**
 * Categories of data for formal retention policies.
 */
enum class DataRetentionCategory {
    GLUCOSE_READINGS,
    THERAPY_HISTORY,
    DEVICE_HARDWARE_LOGS,
    REMINDERS,
    SYSTEM_METRICS,
    OTHER
}

/**
 * Returns the supported [RetentionOption] choices for this category.
 */
fun DataRetentionCategory.allowedOptions(): List<RetentionOption> = when (this) {
    DataRetentionCategory.GLUCOSE_READINGS -> listOf(
        RetentionOption.DAYS_7,
        RetentionOption.DAYS_14,
        RetentionOption.DAYS_30,
        RetentionOption.DAYS_90,
        RetentionOption.UNLIMITED
    )
    DataRetentionCategory.THERAPY_HISTORY -> listOf(
        RetentionOption.DAYS_14,
        RetentionOption.DAYS_30,
        RetentionOption.DAYS_90,
        RetentionOption.DAYS_180,
        RetentionOption.UNLIMITED
    )
    DataRetentionCategory.DEVICE_HARDWARE_LOGS -> listOf(
        RetentionOption.DAY_1,
        RetentionOption.DAYS_3,
        RetentionOption.DAYS_7,
        RetentionOption.DAYS_14,
        RetentionOption.DAYS_30,
        RetentionOption.UNLIMITED
    )
    DataRetentionCategory.REMINDERS -> listOf(
        RetentionOption.DAY_1,
        RetentionOption.DAYS_3,
        RetentionOption.DAYS_7,
        RetentionOption.DAYS_14,
        RetentionOption.UNLIMITED
    )
    DataRetentionCategory.SYSTEM_METRICS -> listOf(
        RetentionOption.DAY_1,
        RetentionOption.DAYS_3,
        RetentionOption.DAYS_7,
        RetentionOption.DAYS_14,
        RetentionOption.DAYS_30,
        RetentionOption.UNLIMITED
    )
    DataRetentionCategory.OTHER -> listOf(
        RetentionOption.DAYS_14,
        RetentionOption.DAYS_30,
        RetentionOption.DAYS_90,
        RetentionOption.UNLIMITED
    )
}