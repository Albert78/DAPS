package de.dh.daps.core.aps

import android.content.Intent
import de.dh.daps.common.model.InsulinAmount
import de.dh.daps.common.model.data.Timestamp
import de.dh.daps.core.repository.db.dao.MealReminderDao
import de.dh.daps.core.repository.db.entities.MealReminderEntity
import de.dh.daps.core.system.SystemWakeService
import de.dh.daps.core.system.WakeupHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Test

class RecommendationManagerTest {

    private val fakeDao = object : MealReminderDao {
        override suspend fun getAllMealReminders(): List<MealReminderEntity> = emptyList()
        override fun observeAllMealReminders(): Flow<List<MealReminderEntity>> = flowOf(emptyList())
        override suspend fun insertMealReminder(reminder: MealReminderEntity): Long = 1L
        override suspend fun insertMealReminders(reminders: List<MealReminderEntity>) {}
        override suspend fun updateMealReminder(reminder: MealReminderEntity) {}
        override suspend fun deleteMealReminder(id: Long) {}
        override suspend fun deleteAllMealReminders() {}
    }

    private val fakeWakeService = object : SystemWakeService {
        override fun registerHandler(tag: String, handler: WakeupHandler) {}
        override fun unregisterHandler(tag: String) {}
        override fun scheduleWakeup(tag: String, wakeupId: UInt?, timestamp: Timestamp) {}
        override fun cancelWakeup(tag: String, wakeupId: UInt?) {}
        override fun acquireBusyState(tag: String) {}
        override fun releaseBusyState(tag: String) {}
        override fun dispatchWakeup(intent: Intent) {}
    }

    @Test
    fun testAddBolusRecommendationRoundsAmountToTwoDecimals() {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val manager = RecommendationManager(fakeDao, fakeWakeService, scope)

        manager.addBolusRecommendation(
            amount = InsulinAmount(1.234567),
            correctionPart = InsulinAmount(0.5555),
            basalPart = InsulinAmount(0.6789),
        )

        val recommendations = manager.recommendations.value
        assertEquals(1, recommendations.size)

        val bolusRec = recommendations.first() as ApsRecommendation.Bolus
        assertEquals(1.23, bolusRec.amount.iu, 0.0001)
        assertEquals(0.56, bolusRec.correctionPart.iu, 0.0001)
        assertEquals(0.68, bolusRec.basalPart.iu, 0.0001)
    }

    @Test
    fun testStopClearsRecommendationsMealRemindersAndDrainsChannel() {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val manager = RecommendationManager(fakeDao, fakeWakeService, scope)

        manager.addCarbsRecommendation(30)
        assertEquals(1, manager.recommendations.value.size)

        manager.stop()

        assertEquals(0, manager.recommendations.value.size)
        assertEquals(0, manager.mealReminders.value.size)
    }
}