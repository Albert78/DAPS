package de.dh.daps.plugin.simbody.backup

import kotlinx.serialization.Serializable

@Serializable
data class SimBodyBackupDto(
    val version: Int = 1,
    val simulationState: SimulationStateBackupDto? = null,
    val pumpState: PumpStateBackupDto? = null,
    val bodyProfiles: List<BodyProfileBackupDto> = emptyList(),
    val simHistory: List<SimHistoryBackupDto> = emptyList(),
    val simEvents: List<SimEventBackupDto> = emptyList(),
    val pumpHistory: List<PumpHistoryBackupDto> = emptyList()
)

@Serializable
data class SimulationStateBackupDto(
    val lastSimulationTimestampMs: Long,
    val exerciseIntensity: Double,
    val stressLevel: Double,
    val illnessFactor: Double,
    val isSensorEnabled: Boolean = true,
    val sensorNoiseFactor: Double = 0.0,
    val sensorDrift: Double = 0.0
)

@Serializable
data class PumpStateBackupDto(
    val batteryLevel: Double,
    val reservoirLevel: Double,
    val isOccluded: Boolean,
    val isPrimed: Boolean,
    val hasHardwareError: Boolean,
    val isBroken: Boolean,
    val isSuspended: Boolean = false,
    val lastBasalDeliveryTimestampMs: Long,
    val tempBasalPercent: Int? = null,
    val tempBasalExpiryMs: Long? = null
)

@Serializable
data class BodyProfileBackupDto(
    val id: Long = 0,
    val name: String,
    val isfBlocks: String,
    val crBlocks: String,
    val liverGlucoseOutputBlocks: String,
    val isActive: Boolean = false
)

@Serializable
data class SimHistoryBackupDto(
    val timestampMs: Long,
    val bgMgDl: Double,
    val carbImpact: Double,
    val insulinImpact: Double,
    val endogenousImpact: Double,
    val exerciseImpact: Double,
    val stressImpact: Double
)

@Serializable
data class SimEventBackupDto(
    val id: Long = 0,
    val type: String,
    val timestampMs: Long,
    val amountIu: Double,
    val detailId: String? = null,
    val insulinOriginName: String? = null
)

@Serializable
data class PumpHistoryBackupDto(
    val id: Long = 0,
    val timestampMs: Long,
    val amountIu: Double,
    val deliveryTypeName: String
)