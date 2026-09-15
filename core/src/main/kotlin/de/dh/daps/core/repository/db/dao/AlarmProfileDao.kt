package de.dh.daps.core.repository.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import de.dh.daps.core.repository.db.entities.AlarmProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmProfileDao {
    @Query("SELECT * FROM alarm_profiles ORDER BY is_default DESC, name ASC")
    suspend fun getAllAlarmProfiles(): List<AlarmProfileEntity>

    @Query("SELECT * FROM alarm_profiles ORDER BY is_default DESC, name ASC")
    fun observeAllAlarmProfiles(): Flow<List<AlarmProfileEntity>>

    @Query("SELECT * FROM alarm_profiles WHERE id = :id")
    suspend fun getAlarmProfileById(id: Long): AlarmProfileEntity?

    @Query("SELECT * FROM alarm_profiles WHERE is_active = 1 LIMIT 1")
    suspend fun getActiveAlarmProfile(): AlarmProfileEntity?

    @Query("SELECT * FROM alarm_profiles WHERE is_active = 1 LIMIT 1")
    fun observeActiveAlarmProfile(): Flow<AlarmProfileEntity?>

    @Insert
    suspend fun insertAlarmProfile(profile: AlarmProfileEntity): Long

    @Update
    suspend fun updateAlarmProfile(profile: AlarmProfileEntity)

    @Query("DELETE FROM alarm_profiles WHERE id = :id")
    suspend fun deleteAlarmProfile(id: Long)

    @Query("UPDATE alarm_profiles SET is_active = 0")
    suspend fun clearActiveFlag()

    @Query("UPDATE alarm_profiles SET is_active = 1 WHERE id = :id")
    suspend fun setActiveFlag(id: Long)

    @Transaction
    suspend fun setActiveAlarmProfile(id: Long) {
        clearActiveFlag()
        setActiveFlag(id)
    }
}