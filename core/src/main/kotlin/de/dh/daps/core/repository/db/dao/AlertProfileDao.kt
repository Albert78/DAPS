package de.dh.daps.core.repository.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import de.dh.daps.core.repository.db.entities.AlertProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertProfileDao {
    @Query("SELECT * FROM alert_profiles ORDER BY is_default DESC, name ASC")
    suspend fun getAllAlertProfiles(): List<AlertProfileEntity>

    @Query("SELECT * FROM alert_profiles ORDER BY is_default DESC, name ASC")
    fun observeAllAlertProfiles(): Flow<List<AlertProfileEntity>>

    @Query("SELECT * FROM alert_profiles WHERE id = :id")
    suspend fun getAlertProfileById(id: Long): AlertProfileEntity?

    @Query("SELECT * FROM alert_profiles WHERE is_default = 1 LIMIT 1")
    suspend fun getDefaultAlertProfile(): AlertProfileEntity?

    @Query("SELECT * FROM alert_profiles WHERE is_default = 1 LIMIT 1")
    fun observeDefaultAlertProfile(): Flow<AlertProfileEntity?>

    @Query("SELECT * FROM alert_profiles WHERE is_active = 1 LIMIT 1")
    suspend fun getActiveAlertProfile(): AlertProfileEntity?

    @Query("SELECT * FROM alert_profiles WHERE is_active = 1 LIMIT 1")
    fun observeActiveAlertProfile(): Flow<AlertProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlertProfiles(profiles: List<AlertProfileEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlertProfile(profile: AlertProfileEntity): Long

    @Update
    suspend fun updateAlertProfile(profile: AlertProfileEntity)

    @Query("DELETE FROM alert_profiles WHERE id = :id")
    suspend fun deleteAlertProfile(id: Long)

    @Query("DELETE FROM alert_profiles")
    suspend fun deleteAllAlertProfiles()

    @Query("UPDATE alert_profiles SET is_default = 0")
    suspend fun clearDefaultFlag()

    @Query("UPDATE alert_profiles SET is_default = 1 WHERE id = :id")
    suspend fun setDefaultFlag(id: Long)

    @Transaction
    suspend fun setDefaultAlertProfile(id: Long) {
        clearDefaultFlag()
        setDefaultFlag(id)
    }

    @Query("UPDATE alert_profiles SET is_active = 0")
    suspend fun clearActiveFlag()

    @Query("UPDATE alert_profiles SET is_active = 1 WHERE id = :id")
    suspend fun setActiveFlag(id: Long)

    @Transaction
    suspend fun setActiveAlertProfile(id: Long) {
        clearActiveFlag()
        setActiveFlag(id)
    }
}