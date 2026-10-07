package de.dh.pump.omnipod.protocol.state

import de.dh.daps.common.model.PluginPreferences
import de.dh.pump.omnipod.protocol.definition.ActivationProgress
import de.dh.pump.omnipod.protocol.definition.AlertType
import de.dh.pump.omnipod.protocol.definition.BasalProgram
import de.dh.pump.omnipod.protocol.definition.BolusType
import de.dh.pump.omnipod.protocol.definition.DeliveryStatus
import de.dh.pump.omnipod.protocol.definition.PodStatus
import de.dh.pump.omnipod.protocol.response.DefaultStatusResponse
import de.dh.pump.omnipod.protocol.response.SetUniqueIdResponse
import de.dh.pump.omnipod.protocol.response.VersionResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.Serializable
import java.time.ZonedDateTime
import java.util.EnumSet

interface OmnipodDashPodStateManager {
    var activationProgress: ActivationProgress
    val isUniqueIdSet: Boolean
    val isActivationCompleted: Boolean
    val isSuspended: Boolean
    val isPodRunning: Boolean

    var uniqueId: Int?
    var lotNumber: Long?
    var podSequenceNumber: Long?
    var bluetoothAddress: String?
    var ltk: ByteArray?

    val pulsesDelivered: Short?
    val reservoirPulsesRemaining: Short?
    val podStatus: PodStatus?
    val deliveryStatus: DeliveryStatus?
    val minutesSinceActivation: Short?
    val activeAlerts: EnumSet<AlertType>?

    var tempBasal: TempBasal?
    var basalProgram: BasalProgram?
    var lastBolus: LastBolus?

    fun updateFromDefaultStatusResponse(response: DefaultStatusResponse)
    fun updateFromVersionResponse(response: VersionResponse)
    fun updateFromSetUniqueIdResponse(response: SetUniqueIdResponse)
    fun reset()

    data class TempBasal(val startTime: Long, val rate: Double, val durationInMinutes: Short) : Serializable

    data class LastBolus(
        val startTime: Long,
        val requestedUnits: Double,
        var bolusUnitsRemaining: Double,
        var deliveryComplete: Boolean,
        val bolusType: BolusType
    ) {
        fun deliveredUnits(): Double = if (deliveryComplete) requestedUnits - bolusUnitsRemaining else 0.0
    }
}

class OmnipodDashPodStateManagerImpl(
    private val preferences: PluginPreferences? = null
) : OmnipodDashPodStateManager {
    override var activationProgress: ActivationProgress = ActivationProgress.NOT_STARTED
    override val isUniqueIdSet: Boolean get() = activationProgress.isAtLeast(ActivationProgress.SET_UNIQUE_ID)
    override val isActivationCompleted: Boolean get() = activationProgress == ActivationProgress.COMPLETED
    override val isSuspended: Boolean get() = deliveryStatus == DeliveryStatus.SUSPENDED
    override val isPodRunning: Boolean get() = podStatus?.isRunning() == true

    override var uniqueId: Int? = null
    override var lotNumber: Long? = null
    override var podSequenceNumber: Long? = null
    override var bluetoothAddress: String? = null
    override var ltk: ByteArray? = null

    override var pulsesDelivered: Short? = null
    override var reservoirPulsesRemaining: Short? = null
    override var podStatus: PodStatus? = null
    override var deliveryStatus: DeliveryStatus? = null
    override var minutesSinceActivation: Short? = null
    override var activeAlerts: EnumSet<AlertType>? = null

    override var tempBasal: OmnipodDashPodStateManager.TempBasal? = null
    override var basalProgram: BasalProgram? = null
    override var lastBolus: OmnipodDashPodStateManager.LastBolus? = null

    override fun updateFromDefaultStatusResponse(response: DefaultStatusResponse) {
        podStatus = response.podStatus
        deliveryStatus = response.deliveryStatus
        pulsesDelivered = response.totalPulsesDelivered
        reservoirPulsesRemaining = response.reservoirPulsesRemaining
        minutesSinceActivation = response.minutesSinceActivation
        activeAlerts = response.activeAlerts
    }

    override fun updateFromVersionResponse(response: VersionResponse) {
        podStatus = response.podStatus
        lotNumber = response.lotNumber
        podSequenceNumber = response.podSequenceNumber
    }

    override fun updateFromSetUniqueIdResponse(response: SetUniqueIdResponse) {
        podStatus = response.podStatus
        lotNumber = response.lotNumber
        podSequenceNumber = response.podSequenceNumber
        activationProgress = ActivationProgress.SET_UNIQUE_ID
    }

    override fun reset() {
        activationProgress = ActivationProgress.NOT_STARTED
        uniqueId = null
        lotNumber = null
        podSequenceNumber = null
        bluetoothAddress = null
        ltk = null
        pulsesDelivered = null
        reservoirPulsesRemaining = null
        podStatus = null
        deliveryStatus = null
        minutesSinceActivation = null
        activeAlerts = null
        tempBasal = null
        basalProgram = null
        lastBolus = null
    }
}