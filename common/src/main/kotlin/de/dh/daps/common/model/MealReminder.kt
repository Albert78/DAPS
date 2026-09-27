package de.dh.daps.common.model

import de.dh.daps.common.ID_UNDEFINED
import de.dh.daps.common.model.data.Timestamp

/**
 * Represents a reminder for a meal.
 *
 * Holds the planned meal time ([mealTimestamp]) and separately the reminder time ([reminderTimestamp]).
 * Initially both times are equal. When snoozed, [reminderTimestamp] is shifted to a later time.
 */
data class MealReminder(
    val id: Long = ID_UNDEFINED,
    val mealId: Long? = null,
    val mealTimestamp: Timestamp,
    val reminderTimestamp: Timestamp = mealTimestamp,
    val description: String = ""
) {
    /**
     * Returns a copy with an updated reminder timestamp (e.g. after snooze).
     */
    fun snooze(newReminderTimestamp: Timestamp): MealReminder {
        return copy(reminderTimestamp = newReminderTimestamp)
    }
}