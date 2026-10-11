package de.dh.pump.omnipod.dash.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "omnipod_history")
data class OmnipodHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestampMs: Long,
    val eventType: OmnipodHistoryEventType,
    val detailText: String,
    val unitsDelivered: Double
)