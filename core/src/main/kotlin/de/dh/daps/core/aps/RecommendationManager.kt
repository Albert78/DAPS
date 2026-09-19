package de.dh.daps.core.aps

import android.content.Intent
import android.util.Log
import de.dh.daps.common.model.DeferredBolus
import de.dh.daps.common.model.ID_UNDEFINED
import de.dh.daps.common.model.InsulinAmount
import de.dh.daps.common.model.MealReminder
import de.dh.daps.common.model.data.Timestamp
import de.dh.daps.core.repository.db.dao.MealReminderDao
import de.dh.daps.core.repository.db.mappers.toEntity
import de.dh.daps.core.repository.db.mappers.toModel
import de.dh.daps.core.system.SystemWakeService
import de.dh.daps.core.system.WakeupHandler
import kotlinx.coroutines.CoroutineScope
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
 * Central manager for treatment recommendations and meal reminders in the APS system.
 *
 * Manages:
 * - Emergency carbs recommendations
 * - Meal reminders ([MealReminder])
 * - Core decisions for Open-Loop mode (Temp-Basal, Bolus, DeferredBolus entries)
 */
class RecommendationManager(
    private val mealReminderDao: MealReminderDao,
    private val wakeService: SystemWakeService,
    private val scope: CoroutineScope
) {
    private val mutex = Mutex()

    private val _recommendations = MutableStateFlow<List<ApsRecommendation>>(emptyList())
    val recommendations: StateFlow<List<ApsRecommendation>> = _recommendations.asStateFlow()

    private val _mealReminders = MutableStateFlow<List<MealReminder>>(emptyList())
    val mealReminders: StateFlow<List<MealReminder>> = _mealReminders.asStateFlow()

    private val dueMealRemindersChannel = Channel<MealReminder>(capacity = Channel.UNLIMITED)
    val dueMealReminders: Flow<MealReminder> = dueMealRemindersChannel.receiveAsFlow()

    init {
        wakeService.registerHandler(WAKE_TAG_MEAL_REMINDER, object : WakeupHandler {
            override fun onWakeup(wakeupId: UInt?, intent: Intent?) {
                scope.launch {
                    checkAndProcessReminders()
                }
            }
        })

        scope.launch {
            mealReminderDao.observeAllMealReminders().collect { entities ->
                val models = entities.map { it.toModel() }
                _mealReminders.value = models
                checkAndProcessReminders(models)
            }
        }
    }

    // -----------------------------------------------------------------------------------------
    // --- Treatment Recommendations ---
    // -----------------------------------------------------------------------------------------

    /**
     * Records a recommendation for emergency carb intake.
     */
    fun addCarbsRecommendation(amountInGram: Int) {
        _recommendations.value = _recommendations.value.filterNot { it is ApsRecommendation.Carbs } + ApsRecommendation.Carbs(amountInGram)
    }

    /**
     * Records a recommendation for bolus delivery in Open-Loop mode.
     */
    fun addBolusRecommendation(
        amount: InsulinAmount,
        includedDeferredBoluses: List<DeferredBolus>? = null,
        correctionPart: InsulinAmount = InsulinAmount.ZERO,
        basalPart: InsulinAmount = InsulinAmount.ZERO
    ) {
        _recommendations.value = _recommendations.value.filterNot { it is ApsRecommendation.Bolus } + ApsRecommendation.Bolus(
            amount = amount,
            includedDeferredBoluses = includedDeferredBoluses,
            correctionPart = correctionPart,
            basalPart = basalPart
        )
    }

    /**
     * Records a recommendation for temp basal rate in Open-Loop mode.
     */
    fun addTempBasalRecommendation(durationInHours: Int, percent: Int) {
        _recommendations.value = _recommendations.value.filterNot { it is ApsRecommendation.TempBasal } + ApsRecommendation.TempBasal(
            durationInHours = durationInHours,
            percent = percent
        )
    }

    /**
     * Clears any active temp basal recommendation.
     */
    fun clearTempBasalRecommendation() {
        _recommendations.value = _recommendations.value.filterNot { it is ApsRecommendation.TempBasal }
    }

    /**
     * Clears all active treatment recommendations.
     */
    fun clearRecommendations() {
        _recommendations.value = emptyList()
    }

    /**
     * Removes a specific recommendation from the list.
     */
    fun removeRecommendation(recommendation: ApsRecommendation) {
        _recommendations.value = _recommendations.value - recommendation
    }

    // -----------------------------------------------------------------------------------------
    // --- Meal Reminders ---
    // -----------------------------------------------------------------------------------------

    /**
     * Schedules a new meal reminder.
     */
    suspend fun scheduleMealReminder(
        mealTimestamp: Timestamp,
        reminderTimestamp: Timestamp = mealTimestamp,
        mealId: Long? = null,
        description: String = ""
    ): MealReminder = mutex.withLock {
        val reminder = MealReminder(
            mealId = mealId,
            mealTimestamp = mealTimestamp,
            reminderTimestamp = reminderTimestamp,
            description = description
        )
        val id = mealReminderDao.insertMealReminder(reminder.toEntity())
        val savedReminder = reminder.copy(id = id)
        Log.i(TAG, "Scheduled meal reminder #$id for $reminderTimestamp (meal at $mealTimestamp)")
        return savedReminder
    }

    /**
     * Snoozes an existing meal reminder by updating its reminder timestamp.
     */
    suspend fun snoozeMealReminder(id: Long, newReminderTimestamp: Timestamp) = mutex.withLock {
        val existing = _mealReminders.value.find { it.id == id } ?: return@withLock
        val updated = existing.snooze(newReminderTimestamp)
        mealReminderDao.updateMealReminder(updated.toEntity())
        Log.i(TAG, "Snoozed meal reminder #$id to $newReminderTimestamp")
    }

    /**
     * Deletes a meal reminder.
     */
    suspend fun deleteMealReminder(id: Long) = mutex.withLock {
        if (id != ID_UNDEFINED) {
            mealReminderDao.deleteMealReminder(id)
            Log.i(TAG, "Deleted meal reminder #$id")
        }
    }

    private suspend fun checkAndProcessReminders(reminders: List<MealReminder> = _mealReminders.value) = mutex.withLock {
        val now = Timestamp.now()
        val dueReminders = reminders.filter { it.reminderTimestamp <= now }

        for (reminder in dueReminders) {
            Log.i(TAG, "Meal reminder #${reminder.id} fired (reminder time: ${reminder.reminderTimestamp}, now: $now)")
            dueMealRemindersChannel.send(reminder)
            if (reminder.id != ID_UNDEFINED) {
                mealReminderDao.deleteMealReminder(reminder.id)
                Log.i(TAG, "Deleted fired meal reminder #${reminder.id}")
            }
        }

        val currentReminders = mealReminderDao.getAllMealReminders().map { it.toModel() }
        val futureReminders = currentReminders.filter { it.reminderTimestamp > now }
        val nextReminder = futureReminders.minByOrNull { it.reminderTimestamp }

        if (nextReminder != null) {
            Log.d(TAG, "Scheduling wakeup for next meal reminder #${nextReminder.id} at ${nextReminder.reminderTimestamp}")
            wakeService.scheduleWakeup(WAKE_TAG_MEAL_REMINDER, WAKEUP_ID_MEAL_REMINDER, nextReminder.reminderTimestamp)
        }
    }

    companion object {
        val TAG = RecommendationManager::class.simpleName
        private const val WAKE_TAG_MEAL_REMINDER = "MealReminder"
        private const val WAKEUP_ID_MEAL_REMINDER = 1u
    }
}