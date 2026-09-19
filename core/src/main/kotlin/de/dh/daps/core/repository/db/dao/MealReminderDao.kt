package de.dh.daps.core.repository.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import de.dh.daps.core.repository.db.entities.MealReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MealReminderDao {
    @Query("SELECT * FROM meal_reminders ORDER BY reminder_timestamp ASC")
    suspend fun getAllMealReminders(): List<MealReminderEntity>

    @Query("SELECT * FROM meal_reminders ORDER BY reminder_timestamp ASC")
    fun observeAllMealReminders(): Flow<List<MealReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealReminder(reminder: MealReminderEntity): Long

    @Update
    suspend fun updateMealReminder(reminder: MealReminderEntity)

    @Query("DELETE FROM meal_reminders WHERE id = :id")
    suspend fun deleteMealReminder(id: Long)

    @Query("DELETE FROM meal_reminders")
    suspend fun deleteAllMealReminders()
}