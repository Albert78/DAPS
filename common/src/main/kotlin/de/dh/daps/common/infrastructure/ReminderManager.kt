package de.dh.daps.common.infrastructure

import de.dh.daps.common.domain.model.data.Timestamp
import de.dh.daps.common.domain.model.reminder.Reminder
import de.dh.daps.common.domain.model.reminder.ReminderType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * App-wide service for managing reminders, scheduling background wakeups,
 * purging obsolete/expired reminders, and dispatching due reminders.
 */
interface ReminderManager {
    /**
     * Flow emitting all currently active reminders.
     */
    val activeReminders: StateFlow<List<Reminder>>

    /**
     * Flow emitting all reminders (both active and historical).
     */
    val allReminders: StateFlow<List<Reminder>>

    /**
     * Flow emitting reminders as they become due.
     */
    val dueReminders: Flow<Reminder>

    /**
     * Flow emitting reminders as they expire ("veraltet").
     */
    val expiredReminders: Flow<Reminder>

    /**
     * Schedules or updates a reminder.
     */
    suspend fun scheduleReminder(reminder: Reminder): Reminder

    /**
     * Snoozes an active or due reminder to a new target timestamp.
     */
    suspend fun snoozeReminder(id: Long, newTimestamp: Timestamp)

    /**
     * Dismisses a reminder (marks as DISMISSED).
     */
    suspend fun dismissReminder(id: Long)

    /**
     * Permanently deletes a reminder by ID.
     */
    suspend fun deleteReminder(id: Long)

    /**
     * Cancels/deletes all reminders associated with the given source ID.
     */
    suspend fun cancelRemindersForSource(sourceId: String)

    /**
     * Retrieves all currently active (non-expired, non-dismissed) reminders.
     */
    suspend fun getActiveReminders(): List<Reminder>

    /**
     * Retrieves active reminders filtered by type.
     */
    suspend fun getRemindersByType(type: ReminderType): List<Reminder>

    /**
     * Purges expired, dismissed, or triggered reminders older than [olderThanDays] days.
     */
    suspend fun purgeOldReminders(olderThanDays: Int = 3)

    /**
     * Purges all expired, dismissed, or triggered reminders.
     */
    suspend fun purgeAllPastReminders()

    /**
     * Manually triggers checking and processing of reminders (for wake-up triggers or system ticks).
     */
    suspend fun checkAndProcessReminders()

    /**
     * Starts monitoring and background wake-up scheduling.
     */
    fun start()

    /**
     * Stops monitoring and cancels scheduled wake-ups.
     */
    fun stop()

    /**
     * Creates and schedules a meal reminder.
     */
    suspend fun createMealReminder(
        mealTimestamp: Timestamp,
        reminderTimestamp: Timestamp = mealTimestamp,
        mealId: Long? = null,
        title: String = "",
        description: String = ""
    ): Reminder

    /**
     * Syncs a consumable reminder (catheter / patch change).
     */
    suspend fun syncConsumableReminder(
        sourceId: String,
        reminderTimestamp: Timestamp,
        title: String = "",
        description: String = ""
    ): Reminder?
}

/**
 * Convenience extension for deleting all reminders for a meal ID.
 */
suspend fun ReminderManager.deleteMealReminders(mealId: Long) {
    cancelRemindersForSource(mealId.toString())
}

const val REMINDER_SOURCE_CATHETER_CHANGE = "catheter_change"
const val REMINDER_SOURCE_PATCH_CHANGE = "patch_change"

/**
 * Convenience extension for clearing a consumable reminder (catheter / patch change).
 */
suspend fun ReminderManager.clearConsumableReminder(sourceId: String) {
    cancelRemindersForSource(sourceId)
}