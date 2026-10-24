package de.dh.daps.core.repository.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import de.dh.daps.common.model.reminder.ReminderStatus
import de.dh.daps.common.model.reminder.ReminderType
import de.dh.daps.core.repository.db.entities.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY reminder_timestamp ASC")
    suspend fun getAllReminders(): List<ReminderEntity>

    @Query("SELECT * FROM reminders ORDER BY reminder_timestamp ASC")
    fun observeAllReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE status = 'ACTIVE' ORDER BY reminder_timestamp ASC")
    fun observeActiveReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: Long): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE type = :type ORDER BY reminder_timestamp ASC")
    suspend fun getRemindersByType(type: ReminderType): List<ReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminders(reminders: List<ReminderEntity>)

    @Update
    suspend fun updateReminder(reminder: ReminderEntity)

    @Query("UPDATE reminders SET status = :status WHERE id = :id")
    suspend fun updateReminderStatus(id: Long, status: ReminderStatus)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminder(id: Long)

    @Query("DELETE FROM reminders WHERE source_id = :sourceId")
    suspend fun deleteRemindersBySource(sourceId: String)

    @Query("DELETE FROM reminders")
    suspend fun deleteAllReminders()
}