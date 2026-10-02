package de.dh.daps.common.model.data

/**
 * Concrete types of alarms supported by the system.
 */
enum class AlarmType(
    val defaultSeverity: AlarmSeverity,
    val category: AlarmCategory,
    val isSafetyCritical: Boolean = false
) {
    CRITICAL_LOW_BG(
        defaultSeverity = AlarmSeverity.CRITICAL,
        category = AlarmCategory.GLUCOSE,
        isSafetyCritical = true
    ),
    LOW_BG(
        defaultSeverity = AlarmSeverity.WARNING,
        category = AlarmCategory.GLUCOSE
    ),
    HIGH_BG(
        defaultSeverity = AlarmSeverity.WARNING,
        category = AlarmCategory.GLUCOSE
    ),
    PUMP_OCCLUSION(
        defaultSeverity = AlarmSeverity.CRITICAL,
        category = AlarmCategory.PUMP,
        isSafetyCritical = true
    ),
    PUMP_SUSPENDED(
        defaultSeverity = AlarmSeverity.CRITICAL,
        category = AlarmCategory.PUMP,
        isSafetyCritical = true
    ),
    PUMP_LOW_INSULIN(
        defaultSeverity = AlarmSeverity.WARNING,
        category = AlarmCategory.PUMP
    ),
    PUMP_LOW_BATTERY(
        defaultSeverity = AlarmSeverity.WARNING,
        category = AlarmCategory.PUMP
    ),
    CGM_SIGNAL_LOSS(
        defaultSeverity = AlarmSeverity.WARNING,
        category = AlarmCategory.CGM
    ),
    SYSTEM_BATTERY_LOW(
        defaultSeverity = AlarmSeverity.INFO,
        category = AlarmCategory.SYSTEM
    );

    /**
     * Set of lower-severity alarms that are subsumed when this alarm is snoozed.
     * For example, snoozing [CRITICAL_LOW_BG] also snoozes [LOW_BG].
     */
    val subsumedAlarms: Set<AlarmType>
        get() = when (this) {
            CRITICAL_LOW_BG -> setOf(LOW_BG)
            else -> emptySet()
        }
}