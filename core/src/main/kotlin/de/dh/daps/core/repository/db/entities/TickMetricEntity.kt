package de.dh.daps.core.repository.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import de.dh.daps.common.model.ID_UNDEFINED
import de.dh.daps.common.model.data.Tick
import de.dh.daps.common.model.data.Timestamp

@Entity(tableName = "tick_metrics")
data class TickMetricEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Long = ID_UNDEFINED,
    val tick: Tick,
    val handlerName: String,
    val startTime: Timestamp,
    val endTime: Timestamp
)