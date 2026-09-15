package de.dh.daps.core.repository.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import de.dh.daps.common.model.ID_UNDEFINED
import de.dh.daps.common.model.InsulinAmount
import de.dh.daps.common.model.data.BgDelta
import de.dh.daps.common.model.data.BgValue
import de.dh.daps.common.model.data.Timestamp
import de.dh.daps.core.aps.CoreReasoning

@Entity(tableName = "core_insights")
data class CoreInsightEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Long = ID_UNDEFINED,
    val timestamp: Timestamp,
    val bgOriginal: BgValue,
    val bgFiltered: BgValue,
    val deviationPerTick: BgDelta,
    val futureActiveInsulin: InsulinAmount,
    val futureActiveCarbs: Double,
    val predictedBgAtPeak: BgValue,
    val targetBg: BgValue,
    val isf: BgDelta,
    val cr: Double,
    val actionBolus: InsulinAmount? = null,
    val actionTempBasalPercent: Int? = null,
    val actionTempBasalDurationInHours: Int? = null,
    val reasoning: CoreReasoning
)