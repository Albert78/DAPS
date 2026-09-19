package de.dh.daps.core.repository.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import de.dh.daps.common.model.ID_UNDEFINED
import de.dh.daps.common.model.data.Timestamp

/**
 * Room entity for a meal reminder.
 */
@Entity(tableName = "meal_reminders")
data class MealReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = ID_UNDEFINED,
    val meal_id: Long? = null,
    val meal_timestamp: Timestamp,
    val reminder_timestamp: Timestamp,
    val description: String = ""
)