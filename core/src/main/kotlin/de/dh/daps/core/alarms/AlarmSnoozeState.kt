package de.dh.raaps.core.alarms

import de.dh.raaps.common.model.data.AlarmType
import de.dh.raaps.common.model.data.Timestamp

/**
 * Encapsulates the active snooze state of a specific [AlarmType].
 */
data class AlarmSnoozeState(
    val alarmType: AlarmType,
    val snoozedUntil: Timestamp,
    val snoozeDurationMinutes: Int
)