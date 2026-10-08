package de.dh.pump.omnipod.dash.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "omnipod_dash_history")
data class DashHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestampMs: Long,
    val eventType: String,
    val detailText: String,
    val unitsDelivered: Double
)