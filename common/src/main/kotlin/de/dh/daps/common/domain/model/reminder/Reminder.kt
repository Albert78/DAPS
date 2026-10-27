package de.dh.daps.common.domain.model.reminder

import de.dh.daps.common.ID_UNDEFINED
import de.dh.daps.common.domain.model.data.Timestamp

/**
 * Type of reminder.
 */
enum class ReminderType {
    MEAL,
    PLUGIN,
    PUMP,
    CGM,
    CUSTOM
}

/**
 * Current status of a reminder.
 */
enum class ReminderStatus {
    ACTIVE,
    TRIGGERED,
    EXPIRED,
    SNOOZED,
    DISMISSED
}

/**
 * Generic reminder data model across the application.
 *
 * @property id Database primary key.
 * @property type Categorizes the reminder.
 * @property sourceId Identifies the component or plugin that created this reminder (e.g. "meal", "plugin:pump-omnipod-dash").
 * @property title Title text shown in notifications or UI cards.
 * @property description Detailed description text.
 * @property reminderTimestamp Timestamp when the reminder is due.
 * @property expirationTimestamp Optional timestamp after which the reminder is considered obsolete.
 * @property targetRoute Deep-link navigation URI string invoked on click (e.g. "app://daps.dh.de/meal/123").
 * @property payload Optional custom metadata payload (e.g., JSON string or key-value string).
 * @property status Current status of the reminder.
 */
data class Reminder(
    val id: Long = ID_UNDEFINED,
    val type: ReminderType = ReminderType.CUSTOM,
    val sourceId: String = "",
    val title: String = "",
    val description: String = "",
    val reminderTimestamp: Timestamp = Timestamp.now(),
    val expirationTimestamp: Timestamp? = null,
    val targetRoute: String = "",
    val payload: String? = null,
    val status: ReminderStatus = ReminderStatus.ACTIVE
) {
    fun isExpired(now: Timestamp = Timestamp.now()): Boolean {
        return expirationTimestamp != null && now >= expirationTimestamp
    }

    fun isDue(now: Timestamp = Timestamp.now()): Boolean {
        return status == ReminderStatus.ACTIVE && reminderTimestamp <= now && !isExpired(now)
    }

    fun snooze(newReminderTimestamp: Timestamp): Reminder {
        return copy(
            reminderTimestamp = newReminderTimestamp,
            status = ReminderStatus.SNOOZED
        )
    }
}