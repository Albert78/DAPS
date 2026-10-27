package de.dh.daps.core.repository.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import de.dh.daps.common.ID_UNDEFINED
import de.dh.daps.common.domain.model.data.Timestamp
import de.dh.daps.common.domain.model.reminder.ReminderStatus
import de.dh.daps.common.domain.model.reminder.ReminderType

/**
 * Room entity for a generic reminder.
 */
@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = ID_UNDEFINED,
    val type: ReminderType = ReminderType.CUSTOM,
    val source_id: String = "",
    val title: String = "",
    val description: String = "",
    val reminder_timestamp: Timestamp,
    val expiration_timestamp: Timestamp? = null,
    val target_route: String = "",
    val payload: String? = null,
    val status: ReminderStatus = ReminderStatus.ACTIVE
)