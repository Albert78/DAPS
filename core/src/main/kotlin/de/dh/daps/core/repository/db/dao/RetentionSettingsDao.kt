package de.dh.daps.core.repository.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import de.dh.daps.core.repository.db.entities.RetentionSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RetentionSettingsDao {
    @Query("SELECT * FROM retention_settings LIMIT 1")
    suspend fun getSettings(): RetentionSettingsEntity?

    @Query("SELECT * FROM retention_settings LIMIT 1")
    fun observeSettings(): Flow<RetentionSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(data: RetentionSettingsEntity): Long

    @Update
    suspend fun updateSettings(data: RetentionSettingsEntity)
}