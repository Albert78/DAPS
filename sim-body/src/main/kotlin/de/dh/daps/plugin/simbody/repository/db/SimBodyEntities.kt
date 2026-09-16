package de.dh.daps.plugin.simbody.repository.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import de.dh.daps.common.model.InsulinAmount
import de.dh.daps.common.model.InsulinOrigin
import de.dh.daps.common.model.data.Timestamp

@Entity(tableName = "sim_history")
data class SimHistoryEntity(
    @PrimaryKey
    val timestamp: Timestamp,
    val bgMgDl: Double,
    val carbImpact: Double,
    val insulinImpact: Double,
    val endogenousImpact: Double,
    val exerciseImpact: Double,
    val stressImpact: Double
)

@Entity(tableName = "simulation_state")
data class SimulationStateEntity(
    @PrimaryKey val id: Int = 0, // Only one state entry
    val lastSimulationTimestamp: Timestamp,
    val exerciseIntensity: Double,
    val stressLevel: Double,
    val illnessFactor: Double,
    val isSensorEnabled: Boolean = true,
    val sensorNoiseFactor: Double = 0.0,
    val sensorDrift: Double = 0.0
)

@Entity(tableName = "sim_events")
data class SimEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String, // "MEAL" or "BOLUS"
    val timestamp: Timestamp,
    val amount: InsulinAmount,
    val detailId: String? = null, // MealType ID or InsulinType ID
    val insulinOrigin: InsulinOrigin? = null
)

@Entity(tableName = "body_profiles")
data class BodyProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val isfBlocks: String, // JSON
    val crBlocks: String, // JSON
    val liverGlucoseOutputBlocks: String, // JSON
    val isActive: Boolean = false
)

@Entity(tableName = "pump_state")
data class PumpStateEntity(
    @PrimaryKey val id: Int = 0,
    val batteryLevel: Double,
    val reservoirLevel: Double,
    val isOccluded: Boolean,
    val isPrimed: Boolean,
    val hasHardwareError: Boolean,
    val isBroken: Boolean,
    val lastBasalDeliveryTimestamp: Timestamp,
    val tempBasalPercent: Int?,
    val tempBasalExpiry: Timestamp? = null
)

enum class PumpDeliveryType {
    Bolus, Basal, Tbr
}

@Entity(tableName = "pump_history")
data class PumpHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Timestamp,
    val amount: InsulinAmount,
    val deliveryType: PumpDeliveryType
)