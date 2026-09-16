package de.dh.daps.plugin.simbody.repository.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import de.dh.daps.common.model.data.Timestamp

@Dao
interface PumpDao {
    // Pump State
    @Query("SELECT * FROM pump_state WHERE id = 0")
    suspend fun getPumpState(): PumpStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updatePumpState(state: PumpStateEntity)

    // Pump History
    @Query("SELECT * FROM pump_history ORDER BY timestamp DESC")
    suspend fun getAllHistory(): List<PumpHistoryEntity>

    @Query("SELECT * FROM pump_history WHERE timestamp >= :since ORDER BY timestamp DESC")
    suspend fun getHistorySince(since: Timestamp): List<PumpHistoryEntity>

    @Insert
    suspend fun insertHistoryEntry(entry: PumpHistoryEntity): Long

    @Query("DELETE FROM pump_history WHERE timestamp < :threshold")
    suspend fun deleteOldHistory(threshold: Timestamp)
}