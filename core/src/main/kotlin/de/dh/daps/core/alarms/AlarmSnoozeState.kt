package de.dh.daps.core.alarms

import de.dh.daps.common.model.data.AlarmType
import de.dh.daps.common.model.data.Timestamp

/**
 * Encapsulates the active snooze state of a specific [AlarmType].
 */
data class AlarmSnoozeState(
    val alarmType: AlarmType,
    val snoozedUntil: Timestamp,
    val snoozeDurationMinutes: Int
)