package de.dh.daps.core.repository.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import de.dh.daps.common.ID_UNDEFINED

/**
 * Entity for an alert profile.
 */
@Entity(tableName = "alert_profiles")
data class AlertProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = ID_UNDEFINED,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "is_default")
    val isDefault: Boolean = false,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = false,

    @ColumnInfo(name = "alarm_defaults_json")
    val alarmDefaultsJson: String,

    @ColumnInfo(name = "alarm_overrides_json")
    val alarmOverridesJson: String = "{}",

    @ColumnInfo(name = "recommendation_default_json")
    val recommendationDefaultJson: String,

    @ColumnInfo(name = "recommendation_overrides_json")
    val recommendationOverridesJson: String = "{}",

    @ColumnInfo(name = "reminder_default_json")
    val reminderDefaultJson: String,

    @ColumnInfo(name = "reminder_overrides_json")
    val reminderOverridesJson: String = "{}"
)