package de.dh.daps.core.repository.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import de.dh.daps.common.model.ID_UNDEFINED
import de.dh.daps.common.model.data.Minutes
import de.dh.daps.common.model.data.Timestamp

/**
 * Entity for a therapy profile.
 */
@Entity(
    tableName = "insulin_profiles"
)
data class InsulinProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = ID_UNDEFINED,
    val name: String,
    val basal_blocks: List<DBBlock>,
    val isf_blocks: List<DBBlock>,
    val cr_blocks: List<DBBlock>,
    val insulin_type_id: String,
    val insulin_concentration: Double = 1.0,
    val dia: Minutes,
    val peak: Minutes
)

/**
 * Entity for the current active therapy settings.
 */
@Entity(
    tableName = "current_therapy_settings",
    foreignKeys = [
        ForeignKey(
            entity = InsulinProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["insulin_profile_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("insulin_profile_id")]
)
data class CurrentTherapySettingsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = ID_UNDEFINED,
    val insulin_profile_id: Long,
    val default_bg_blocks: List<DBBgBlock>,
    val insulin_adjustment_percentage: Int,
    val target_bg_override: Short? = null,
    val low_threshold_override: Short? = null,
    val alarm_profile_override_id: Long? = null,
    val adjustment_hint: String? = null,
    val adjustment_end_time: Timestamp? = null
)

/**
 * Entity for planned/scheduled therapy adjustments.
 */
@Entity(
    tableName = "scheduled_therapy_adjustments"
)
data class ScheduledTherapyAdjustmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = ID_UNDEFINED,
    val start_time: Timestamp,
    val end_time: Timestamp,
    val insulin_adjustment_percentage: Int = 0,
    val target_bg_override: Short? = null,
    val low_threshold_override: Short? = null,
    val alarm_profile_override_id: Long? = null,
    val adjustment_hint: String? = null
)

/**
 * Entity for predefined or user-configured therapy adjustment presets.
 */
@Entity(
    tableName = "therapy_adjustments"
)
data class TherapyAdjustmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = ID_UNDEFINED,
    val name: String,
    val percentage: Int = 0,
    val target_bg_override: Short? = null,
    val low_threshold_override: Short? = null,
    val alarm_profile_override_id: Long? = null
)