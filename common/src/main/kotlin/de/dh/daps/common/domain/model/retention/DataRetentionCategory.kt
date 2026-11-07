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