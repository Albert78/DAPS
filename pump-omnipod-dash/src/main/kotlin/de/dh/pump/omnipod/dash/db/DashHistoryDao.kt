package de.dh.pump.omnipod.dash.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DashHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: DashHistoryEntity)

    @Query("SELECT * FROM omnipod_dash_history ORDER BY timestampMs DESC")
    fun getAllEvents(): Flow<List<DashHistoryEntity>>

    @Query("SELECT * FROM omnipod_dash_history ORDER BY timestampMs DESC LIMIT :limit")
    suspend fun getRecentEvents(limit: Int): List<DashHistoryEntity>

    @Query("SELECT * FROM omnipod_dash_history WHERE timestampMs >= :sinceMs ORDER BY timestampMs ASC")
    suspend fun getEventsSince(sinceMs: Long): List<DashHistoryEntity>

    @Query("DELETE FROM omnipod_dash_history")
    suspend fun clearAll()
}