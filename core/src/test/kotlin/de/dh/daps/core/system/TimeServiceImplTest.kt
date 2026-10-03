package de.dh.daps.core.system

import android.content.Intent
import androidx.room.InvalidationTracker
import de.dh.daps.common.model.data.Minutes
import de.dh.daps.common.model.data.Timestamp
import de.dh.daps.core.repository.SystemMetricsRepository
import de.dh.daps.core.repository.db.AppDatabase
import de.dh.daps.core.repository.db.dao.AlarmProfileDao
import de.dh.daps.core.repository.db.dao.MealReminderDao
import de.dh.daps.core.repository.db.dao.MetabolicEventsDao
import de.dh.daps.core.repository.db.dao.ProviderDao
import de.dh.daps.core.repository.db.dao.SettingsDao
import de.dh.daps.core.repository.db.dao.SystemMetricsDao
import de.dh.daps.core.repository.db.dao.TherapyDao
import de.dh.daps.core.repository.db.entities.CoreInsightEntity
import de.dh.daps.core.repository.db.entities.TickMetricEntity
import de.dh.daps.core.repository.db.entities.WakeupMetricEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Test

class TimeServiceImplTest {

    private val fakeWakeService = object : SystemWakeService {
        var lastScheduledTimestamp: Timestamp? = null
        var isCancelled = false

        override fun registerHandler(tag: String, handler: WakeupHandler) {}
        override fun unregisterHandler(tag: String) {}
        override fun scheduleWakeup(tag: String, wakeupId: UInt?, timestamp: Timestamp) {
            lastScheduledTimestamp = timestamp
            isCancelled = false
        }
        override fun cancelWakeup(tag: String, wakeupId: UInt?) {
            isCancelled = true
        }
        override fun acquireBusyState(tag: String) {}
        override fun releaseBusyState(tag: String) {}
        override fun dispatchWakeup(intent: Intent) {}
    }

    private val fakeMetricsDao = object : SystemMetricsDao {
        override suspend fun insert(insight: CoreInsightEntity): Long = 0L
        override fun observeAll(): Flow<List<CoreInsightEntity>> = flowOf(emptyList())
        override suspend fun getAllCoreInsights(): List<CoreInsightEntity> = emptyList()
        override suspend fun getLatest(limit: Int): List<CoreInsightEntity> = emptyList()
        override suspend fun pruneOlderThan(timestamp: Long) {}
        override suspend fun deleteAllCoreInsights() {}
        override suspend fun insertCoreInsights(items: List<CoreInsightEntity>) {}
        override suspend fun insertWakeupMetric(metric: WakeupMetricEntity): Long = 0L
        override suspend fun insertTickMetric(metric: TickMetricEntity): Long = 0L
        override suspend fun pruneWakeupMetricsOlderThan(timestamp: Long) {}
        override suspend fun pruneTickMetricsOlderThan(timestamp: Long) {}
    }

    private val fakeDb = object : AppDatabase() {
        override fun systemMetricsDao(): SystemMetricsDao = fakeMetricsDao
        override fun providerDao(): ProviderDao = throw NotImplementedError()
        override fun therapyDao(): TherapyDao = throw NotImplementedError()
        override fun metabolicEventsDao(): MetabolicEventsDao = throw NotImplementedError()
        override fun settingsDao(): SettingsDao = throw NotImplementedError()
        override fun alarmProfileDao(): AlarmProfileDao = throw NotImplementedError()
        override fun mealReminderDao(): MealReminderDao = throw NotImplementedError()
        override fun clearAllTables() {}
        override fun createInvalidationTracker(): InvalidationTracker = InvalidationTracker(this, emptyMap(), emptyMap())
    }

    private val fakeMetricsRepository = SystemMetricsRepository(fakeDb)

    @Test
    fun testInitialSyncSetsTimelineOffsetAndStopResetsState() {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val service = TimeServiceImpl(
            tickInterval = Minutes(5),
            wakeService = fakeWakeService,
            systemMetricsRepository = fakeMetricsRepository,
            scope = scope
        )

        // First synchronization: Should directly snap timeline offset
        val syncTime = Timestamp(123456789L)
        val expectedOffset = Math.floorMod(syncTime.ms, service.timeline.tickSizeMs)
        service.synchronize(syncTime)

        assertEquals(expectedOffset, service.timeline.offsetMs)

        // Stop service: Should reset timeline offset and synchronization state
        service.stop()
        assertEquals(0L, service.timeline.offsetMs)
        assertEquals(true, fakeWakeService.isCancelled)

        // Re-synchronize with a new timestamp after stop: Should snap directly to new offset
        val newSyncTime = Timestamp(987654321L)
        val expectedNewOffset = Math.floorMod(newSyncTime.ms, service.timeline.tickSizeMs)
        service.synchronize(newSyncTime)

        assertEquals(expectedNewOffset, service.timeline.offsetMs)
    }
}