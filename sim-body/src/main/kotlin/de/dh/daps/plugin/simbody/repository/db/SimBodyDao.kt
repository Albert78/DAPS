package de.dh.daps.plugin.simbody.repository.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import de.dh.daps.common.model.data.Timestamp
import kotlinx.coroutines.flow.Flow

@Dao
interface SimBodyDao {
    // Simulation History (Combined BG and Impacts)
    @Query("SELECT * FROM sim_history ORDER BY timestamp DESC")
    fun observeAllHistory(): Flow<List<SimHistoryEntity>>

    @Query("SELECT * FROM sim_history WHERE timestamp >= :since ORDER BY timestamp DESC")
    suspend fun getHistorySince(since: Timestamp): List<SimHistoryEntity>

    @Query("SELECT * FROM sim_history ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestHistoryEntry(): SimHistoryEntity?

    @Query("SELECT * FROM sim_history WHERE timestamp <= :since ORDER BY timestamp DESC LIMIT 1")
    suspend fun getHistoryNear(since: Timestamp): SimHistoryEntity?

    @Query("SELECT * FROM sim_history ORDER BY timestamp ASC LIMIT 1")
    suspend fun getEarliestHistoryEntry(): SimHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(entry: SimHistoryEntity): Long

    @Query("DELETE FROM sim_history WHERE timestamp < :threshold")
    suspend fun deleteOldHistory(threshold: Timestamp)

    // Simulation State
    @Query("SELECT * FROM simulation_state WHERE id = 0")
    suspend fun getSimulationState(): SimulationStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateSimulationState(state: SimulationStateEntity)

    // Simulation Events
    @Query("SELECT * FROM sim_events WHERE timestamp >= :since ORDER BY timestamp DESC")
    suspend fun getEventsSince(since: Timestamp): List<SimEventEntity>

    @Insert
    suspend fun insertEvent(event: SimEventEntity): Long

    @Query("DELETE FROM sim_events WHERE timestamp < :threshold")
    suspend fun deleteOldEvents(threshold: Timestamp)

    // Body Profiles
    @Query("SELECT * FROM body_profiles")
    fun observeAllBodyProfiles(): Flow<List<BodyProfileEntity>>

    @Query("SELECT * FROM body_profiles WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveBodyProfile(): BodyProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBodyProfile(profile: BodyProfileEntity): Long

    @Update
    suspend fun updateBodyProfile(profile: BodyProfileEntity)
}