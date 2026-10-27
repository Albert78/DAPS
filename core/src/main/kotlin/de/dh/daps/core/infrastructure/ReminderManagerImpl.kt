package de.dh.daps.core.infrastructure

import android.content.Context
import android.content.Intent
import de.dh.daps.common.ID_UNDEFINED
import de.dh.daps.common.R as CommonR
import de.dh.daps.common.domain.model.data.Minutes
import de.dh.daps.common.domain.model.data.Timestamp
import de.dh.daps.common.domain.model.reminder.Reminder
import de.dh.daps.common.domain.model.reminder.ReminderStatus
import de.dh.daps.common.domain.model.reminder.ReminderType
import de.dh.daps.common.infrastructure.REMINDER_SOURCE_CATHETER_CHANGE
import de.dh.daps.common.infrastructure.REMINDER_SOURCE_PATCH_CHANGE
import de.dh.daps.common.infrastructure.ReminderManager
import de.dh.daps.common.infrastructure.SystemWakeService
import de.dh.daps.common.infrastructure.TimeService
import de.dh.daps.common.infrastructure.WakeupHandler
import de.dh.daps.common.navigation.AppDeepLinks
import de.dh.daps.core.repository.db.dao.ReminderDao
import de.dh.daps.core.repository.db.mappers.toEntity
import de.dh.daps.core.repository.db.mappers.toModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Concrete implementation of [ReminderManager] handling database persistence,
 * background wake-up scheduling via [SystemWakeService], expiration purging,
 * and dispatching due reminders.
 */
class ReminderManagerImpl(
    private val context: Context,
    private val reminderDao: ReminderDao,
    private val wakeService: SystemWakeService,
    private val timeService: TimeService,
    private val scope: CoroutineScope
) : ReminderManager, WakeupHandler {

    private val _activeReminders = MutableStateFlow<List<Reminder>>(emptyList())
    override val activeReminders: StateFlow<List<Reminder>> = _activeReminders.asStateFlow()

    private val _allReminders = MutableStateFlow<List<Reminder>>(emptyList())
    override val allReminders: StateFlow<List<Reminder>> = _allReminders.asStateFlow()

    private val dueRemindersChannel = Channel<Reminder>(capacity = Channel.UNLIMITED)
    override val dueReminders: Flow<Reminder> = dueRemindersChannel.receiveAsFlow()

    private val expiredRemindersChannel = Channel<Reminder>(capacity = Channel.UNLIMITED)
    override val expiredReminders: Flow<Reminder> = expiredRemindersChannel.receiveAsFlow()

    private val mutex = Mutex()
    private var observeJob: Job? = null

    override fun start() {
        wakeService.registerHandler(WAKE_TAG_REMINDER, this)
        observeJob = scope.launch {
            reminderDao.observeAllReminders().collect { entities ->
                val models = entities.map { it.toModel() }
                _allReminders.value = models
                checkAndProcessRemindersInternal(models)
            }
        }
        scope.launch {
            purgeOldReminders(3)
        }
    }

    override fun stop() {
        wakeService.unregisterHandler(WAKE_TAG_REMINDER)
        wakeService.cancelWakeup(WAKE_TAG_REMINDER, WAKEUP_ID_REMINDER)
        observeJob?.cancel()
        observeJob = null
    }

    override fun onWakeup(wakeupId: UInt?, intent: Intent?) {
        scope.launch {
            checkAndProcessReminders()
        }
    }

    override suspend fun checkAndProcessReminders() {
        val models = reminderDao.getAllReminders().map { it.toModel() }
        checkAndProcessRemindersInternal(models)
    }

    private suspend fun checkAndProcessRemindersInternal(allReminders: List<Reminder>) = mutex.withLock {
        val now = timeService.currentTime

        for (reminder in allReminders) {
            if (reminder.status == ReminderStatus.ACTIVE || reminder.status == ReminderStatus.SNOOZED) {
                if (reminder.isExpired(now)) {
                    reminderDao.updateReminderStatus(reminder.id, ReminderStatus.EXPIRED)
                    expiredRemindersChannel.trySend(reminder.copy(status = ReminderStatus.EXPIRED))
                } else if (reminder.isDue(now)) {
                    reminderDao.updateReminderStatus(reminder.id, ReminderStatus.TRIGGERED)
                    dueRemindersChannel.trySend(reminder.copy(status = ReminderStatus.TRIGGERED))
                }
            }
        }

        val updatedEntities = reminderDao.getAllReminders().map { it.toModel() }
        _allReminders.value = updatedEntities
        val activeList = updatedEntities.filter {
            (it.status == ReminderStatus.ACTIVE || it.status == ReminderStatus.SNOOZED) && !it.isExpired(now)
        }
        _activeReminders.value = activeList

        val nextWakeup = activeList
            .filter { it.reminderTimestamp > now }
            .minByOrNull { it.reminderTimestamp }

        if (nextWakeup != null) {
            wakeService.scheduleWakeup(WAKE_TAG_REMINDER, WAKEUP_ID_REMINDER, nextWakeup.reminderTimestamp)
        } else {
            wakeService.cancelWakeup(WAKE_TAG_REMINDER, WAKEUP_ID_REMINDER)
        }
    }

    override suspend fun scheduleReminder(reminder: Reminder): Reminder {
        val id = reminderDao.insertReminder(reminder.toEntity())
        val saved = reminder.copy(id = id)
        checkAndProcessReminders()
        return saved
    }

    override suspend fun snoozeReminder(id: Long, newTimestamp: Timestamp) {
        val existing = reminderDao.getReminderById(id)?.toModel() ?: return
        val snoozed = existing.snooze(newTimestamp)
        reminderDao.updateReminder(snoozed.toEntity())
        checkAndProcessReminders()
    }

    override suspend fun dismissReminder(id: Long) {
        reminderDao.updateReminderStatus(id, ReminderStatus.DISMISSED)
        checkAndProcessReminders()
    }

    override suspend fun deleteReminder(id: Long) {
        reminderDao.deleteReminder(id)
        checkAndProcessReminders()
    }

    override suspend fun cancelRemindersForSource(sourceId: String) {
        reminderDao.deleteRemindersBySource(sourceId)
        checkAndProcessReminders()
    }

    override suspend fun getActiveReminders(): List<Reminder> {
        val now = timeService.currentTime
        return reminderDao.getAllReminders()
            .map { it.toModel() }
            .filter { (it.status == ReminderStatus.ACTIVE || it.status == ReminderStatus.SNOOZED) && !it.isExpired(now) }
    }

    override suspend fun getRemindersByType(type: ReminderType): List<Reminder> {
        val now = timeService.currentTime
        return reminderDao.getRemindersByType(type)
            .map { it.toModel() }
            .filter { (it.status == ReminderStatus.ACTIVE || it.status == ReminderStatus.SNOOZED) && !it.isExpired(now) }
    }

    override suspend fun purgeOldReminders(olderThanDays: Int) {
        val cutoff = timeService.currentTime.minusHours(olderThanDays * 24)
        reminderDao.deletePastRemindersOlderThan(cutoff.ms)
        checkAndProcessReminders()
    }

    override suspend fun purgeAllPastReminders() {
        reminderDao.deleteAllPastReminders()
        checkAndProcessReminders()
    }

    override suspend fun createMealReminder(
        mealTimestamp: Timestamp,
        reminderTimestamp: Timestamp,
        mealId: Long?,
        title: String,
        description: String
    ): Reminder {
        val sourceId = mealId?.toString() ?: ""
        val route = if (mealId != null && mealId != ID_UNDEFINED) AppDeepLinks.meal(mealId) else ""
        val resolvedTitle = title.ifBlank { context.getString(CommonR.string.notification_meal_reminder_title) }
        val resolvedDescription = description.ifBlank { context.getString(CommonR.string.notification_meal_reminder_text_default) }
        val reminder = Reminder(
            type = ReminderType.MEAL,
            sourceId = sourceId,
            title = resolvedTitle,
            description = resolvedDescription,
            reminderTimestamp = reminderTimestamp,
            expirationTimestamp = reminderTimestamp + Minutes(120),
            targetRoute = route
        )
        return scheduleReminder(reminder)
    }

    override suspend fun syncConsumableReminder(
        sourceId: String,
        reminderTimestamp: Timestamp,
        title: String,
        description: String
    ): Reminder? {
        cancelRemindersForSource(sourceId)
        if (reminderTimestamp.ms > System.currentTimeMillis()) {
            val defaultTitle = when (sourceId) {
                REMINDER_SOURCE_CATHETER_CHANGE -> context.getString(CommonR.string.notification_catheter_change_title)
                REMINDER_SOURCE_PATCH_CHANGE -> context.getString(CommonR.string.notification_patch_change_title)
                else -> ""
            }
            val defaultDescription = when (sourceId) {
                REMINDER_SOURCE_CATHETER_CHANGE -> context.getString(CommonR.string.notification_catheter_change_description)
                REMINDER_SOURCE_PATCH_CHANGE -> context.getString(CommonR.string.notification_patch_change_description)
                else -> ""
            }
            val reminder = Reminder(
                type = ReminderType.PUMP,
                sourceId = sourceId,
                title = title.ifBlank { defaultTitle },
                description = description.ifBlank { defaultDescription },
                reminderTimestamp = reminderTimestamp,
                expirationTimestamp = reminderTimestamp + Minutes(180),
                targetRoute = AppDeepLinks.SYSTEM_CONTROL_PUMP
            )
            return scheduleReminder(reminder)
        }
        return null
    }

    companion object {
        const val WAKE_TAG_REMINDER = "ReminderManager"
        val WAKEUP_ID_REMINDER: UInt = 500u
    }
}