package de.dh.daps.core.repository.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import de.dh.daps.common.ID_UNDEFINED
import de.dh.daps.common.domain.model.retention.RetentionOption
import de.dh.daps.common.domain.model.retention.RetentionPolicySettings

@Entity(tableName = "retention_settings")
data class RetentionSettingsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = ID_UNDEFINED,
    val glucoseReadings: RetentionOption,
    val therapyHistory: RetentionOption,
    val deviceHardwareLogs: RetentionOption,
    val reminders: RetentionOption,
    val systemMetrics: RetentionOption,
    val other: RetentionOption
)

fun RetentionSettingsEntity.toModel(): RetentionPolicySettings {
    return RetentionPolicySettings(
        glucoseReadings = glucoseReadings,
        therapyHistory = therapyHistory,
        deviceHardwareLogs = deviceHardwareLogs,
        reminders = reminders,
        systemMetrics = systemMetrics,
        other = other
    )
}

fun RetentionPolicySettings.toEntity(id: Long = ID_UNDEFINED): RetentionSettingsEntity {
    return RetentionSettingsEntity(
        id = id,
        glucoseReadings = glucoseReadings,
        therapyHistory = therapyHistory,
        deviceHardwareLogs = deviceHardwareLogs,
        reminders = reminders,
        systemMetrics = systemMetrics,
        other = other
    )
}