package de.dh.daps.core.repository.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import de.dh.daps.common.ID_UNDEFINED
import de.dh.daps.common.model.ApsMode

/**
 * Entity for the current system settings.
 */
@Entity(
    tableName = "current_settings"
)
data class CurrentSettingsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = ID_UNDEFINED,
    val aps_mode: ApsMode
)