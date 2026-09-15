package de.dh.daps.core.repository.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import de.dh.daps.common.model.ID_UNDEFINED

/**
 * Entity for an alarm profile.
 */
@Entity(tableName = "alarm_profiles")
data class AlarmProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = ID_UNDEFINED,
    val name: String,
    val is_default: Boolean = false,
    val is_active: Boolean = false,
    val severity_defaults_json: String,
    val custom_overrides_json: String = "{}"
)