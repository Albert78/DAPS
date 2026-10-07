package de.dh.daps.core.repository.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import de.dh.daps.core.repository.db.entities.CoreInsightEntity
import de.dh.daps.core.repository.db.entities.TickMetricEntity
import de.dh.daps.core.repository.db.entities.WakeupMetricEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SystemMetricsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(insight: CoreInsightEntity): Long

    @Query("SELECT * FROM core_insights ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<CoreInsightEntity>>

    @Query("SELECT * FROM core_insights ORDER BY timestamp ASC")
    suspend fun getAllCoreInsights(): List<CoreInsightEntity>

    @Query("SELECT * FROM core_insights ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getLatest(limit: Int): List<CoreInsightEntity>

    @Query("DELETE FROM core_insights WHERE timestamp < :timestamp")
    suspend fun pruneOlderThan(timestamp: Long)

    @Query("DELETE FROM core_insights")
    suspend fun deleteAllCoreInsights()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCoreInsights(items: List<CoreInsightEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWakeupMetric(metric: WakeupMetricEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTickMetric(metric: TickMetricEntity): Long

    @Query("DELETE FROM wakeup_metrics WHERE scheduledTime < :timestamp")
    suspend fun pruneWakeupMetricsOlderThan(timestamp: Long)

    @Query("DELETE FROM tick_metrics WHERE startTime < :timestamp")
    suspend fun pruneTickMetricsOlderThan(timestamp: Long)
}