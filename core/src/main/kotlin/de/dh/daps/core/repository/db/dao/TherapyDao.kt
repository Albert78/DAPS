package de.dh.daps.core.repository.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import de.dh.daps.core.repository.db.entities.CurrentTherapySettingsEntity
import de.dh.daps.core.repository.db.entities.InsulinProfileEntity
import de.dh.daps.core.repository.db.entities.ScheduledTherapyAdjustmentEntity
import de.dh.daps.core.repository.db.entities.TherapyAdjustmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TherapyDao {
    // Profiles
    @Query("SELECT * FROM insulin_profiles ORDER BY name ASC")
    suspend fun getAllInsulinProfiles(): List<InsulinProfileEntity>

    @Query("SELECT * FROM insulin_profiles ORDER BY name ASC")
    fun observeAllInsulinProfiles(): Flow<List<InsulinProfileEntity>>

    @Query("SELECT * FROM insulin_profiles WHERE id = :id")
    suspend fun getInsulinProfileById(id: Long): InsulinProfileEntity?

    @Insert
    suspend fun insertInsulinProfile(profile: InsulinProfileEntity): Long

    @Update
    suspend fun updateInsulinProfile(profile: InsulinProfileEntity)

    @Query("DELETE FROM insulin_profiles WHERE id = :id")
    suspend fun deleteInsulinProfile(id: Long)

    @Query("DELETE FROM insulin_profiles")
    suspend fun deleteAllInsulinProfiles()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInsulinProfiles(items: List<InsulinProfileEntity>)

    // Current Therapy Settings
    @Query("SELECT * FROM current_therapy_settings LIMIT 1")
    suspend fun getCurrentTherapySettings(): CurrentTherapySettingsEntity?

    @Query("SELECT * FROM current_therapy_settings LIMIT 1")
    fun observeCurrentTherapySettings(): Flow<CurrentTherapySettingsEntity?>

    @Insert
    suspend fun insertCurrentTherapySettings(data: CurrentTherapySettingsEntity): Long

    @Update
    suspend fun updateCurrentTherapySettings(data: CurrentTherapySettingsEntity)

    @Query("DELETE FROM current_therapy_settings")
    suspend fun deleteAllCurrentTherapySettings()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCurrentTherapySettingsList(items: List<CurrentTherapySettingsEntity>)

    // Scheduled Therapy Adjustments
    @Query("SELECT * FROM scheduled_therapy_adjustments ORDER BY start_time ASC")
    suspend fun getAllScheduledTherapyAdjustments(): List<ScheduledTherapyAdjustmentEntity>

    @Query("SELECT * FROM scheduled_therapy_adjustments ORDER BY start_time ASC")
    fun observeAllScheduledTherapyAdjustments(): Flow<List<ScheduledTherapyAdjustmentEntity>>

    @Query("SELECT * FROM scheduled_therapy_adjustments WHERE id = :id")
    suspend fun getScheduledTherapyAdjustmentById(id: Long): ScheduledTherapyAdjustmentEntity?

    @Insert
    suspend fun insertScheduledTherapyAdjustment(data: ScheduledTherapyAdjustmentEntity): Long

    @Update
    suspend fun updateScheduledTherapyAdjustment(data: ScheduledTherapyAdjustmentEntity)

    @Query("DELETE FROM scheduled_therapy_adjustments WHERE id = :id")
    suspend fun deleteScheduledTherapyAdjustment(id: Long)

    @Query("DELETE FROM scheduled_therapy_adjustments")
    suspend fun deleteAllScheduledTherapyAdjustments()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduledTherapyAdjustments(items: List<ScheduledTherapyAdjustmentEntity>)

    // Therapy Adjustments (Presets)
    @Query("SELECT * FROM therapy_adjustments ORDER BY id ASC")
    suspend fun getAllTherapyAdjustments(): List<TherapyAdjustmentEntity>

    @Query("SELECT * FROM therapy_adjustments ORDER BY id ASC")
    fun observeAllTherapyAdjustments(): Flow<List<TherapyAdjustmentEntity>>

    @Query("SELECT * FROM therapy_adjustments WHERE id = :id")
    suspend fun getTherapyAdjustmentById(id: Long): TherapyAdjustmentEntity?

    @Insert
    suspend fun insertTherapyAdjustment(data: TherapyAdjustmentEntity): Long

    @Update
    suspend fun updateTherapyAdjustment(data: TherapyAdjustmentEntity)

    @Query("DELETE FROM therapy_adjustments WHERE id = :id")
    suspend fun deleteTherapyAdjustment(id: Long)

    @Query("DELETE FROM therapy_adjustments")
    suspend fun deleteAllTherapyAdjustments()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTherapyAdjustments(items: List<TherapyAdjustmentEntity>)
}