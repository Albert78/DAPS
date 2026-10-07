package de.dh.daps.core.repository.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import de.dh.daps.core.repository.db.entities.CurrentSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {
    @Query("SELECT * FROM current_settings LIMIT 1")
    suspend fun getCurrentSettings(): CurrentSettingsEntity?

    @Query("SELECT * FROM current_settings LIMIT 1")
    fun observeCurrentSettings(): Flow<CurrentSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCurrentSettings(data: CurrentSettingsEntity): Long

    @Update
    suspend fun updateCurrentSettings(data: CurrentSettingsEntity)

    @Query("DELETE FROM current_settings")
    suspend fun deleteAllCurrentSettings()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCurrentSettingsList(items: List<CurrentSettingsEntity>)
}