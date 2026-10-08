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
import java.io.Serializable
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
    var activatedAtTimestampMs: Long?
    var lastNonce: Int?

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
    suspend fun saveToPreferences()
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
    override var activatedAtTimestampMs: Long? = null
    override var lastNonce: Int? = null

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

    override suspend fun saveToPreferences() {
        preferences?.apply {
            putInt(KEY_UNIQUE_ID, uniqueId ?: 0)
            putLong(KEY_LOT_NUMBER, lotNumber ?: 0L)
            putLong(KEY_POD_SEQ_NUMBER, podSequenceNumber ?: 0L)
            putString(KEY_BLE_ADDRESS, bluetoothAddress)
            putString(KEY_LTK, ltk?.joinToString("") { "%02x".format(it) })
            putLong(KEY_ACTIVATED_AT, activatedAtTimestampMs ?: 0L)
            putInt(KEY_LAST_NONCE, lastNonce ?: 0)
            putString(KEY_ACTIVATION_PROGRESS, activationProgress.name)
        }
    }

    override fun reset() {
        activationProgress = ActivationProgress.NOT_STARTED
        uniqueId = null
        lotNumber = null
        podSequenceNumber = null
        bluetoothAddress = null
        ltk = null
        activatedAtTimestampMs = null
        lastNonce = null
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

    companion object {
        private const val KEY_UNIQUE_ID = "omnipod_dash_unique_id"
        private const val KEY_LOT_NUMBER = "omnipod_dash_lot_number"
        private const val KEY_POD_SEQ_NUMBER = "omnipod_dash_pod_seq_number"
        private const val KEY_BLE_ADDRESS = "omnipod_dash_ble_address"
        private const val KEY_LTK = "omnipod_dash_ltk"
        private const val KEY_ACTIVATED_AT = "omnipod_dash_activated_at"
        private const val KEY_LAST_NONCE = "omnipod_dash_last_nonce"
        private const val KEY_ACTIVATION_PROGRESS = "omnipod_dash_activation_progress"
    }
}