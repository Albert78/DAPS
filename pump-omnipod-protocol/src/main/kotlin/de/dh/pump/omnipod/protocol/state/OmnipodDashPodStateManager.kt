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
import kotlinx.coroutines.flow.first
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
    fun recoverActivationFromPodStatus(status: PodStatus): ActivationProgress
    suspend fun saveToPreferences()
    suspend fun loadFromPreferences()
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
        recoverActivationFromPodStatus(response.podStatus)
    }

    override fun updateFromVersionResponse(response: VersionResponse) {
        podStatus = response.podStatus
        lotNumber = response.lotNumber
        podSequenceNumber = response.podSequenceNumber
        recoverActivationFromPodStatus(response.podStatus)
    }

    override fun updateFromSetUniqueIdResponse(response: SetUniqueIdResponse) {
        podStatus = response.podStatus
        lotNumber = response.lotNumber
        podSequenceNumber = response.podSequenceNumber
        activationProgress = ActivationProgress.SET_UNIQUE_ID
        recoverActivationFromPodStatus(response.podStatus)
    }

    override fun recoverActivationFromPodStatus(status: PodStatus): ActivationProgress {
        val recoveredProgress = when (status) {
            PodStatus.UNINITIALIZED, PodStatus.MFG_TEST -> ActivationProgress.NOT_STARTED
            PodStatus.FILLED, PodStatus.UID_SET -> ActivationProgress.SET_UNIQUE_ID
            PodStatus.ENGAGING_CLUTCH_DRIVE, PodStatus.CLUTCH_DRIVE_ENGAGED, PodStatus.PRIMING -> ActivationProgress.PRIME_COMPLETED
            PodStatus.BASAL_PROGRAM_SET -> ActivationProgress.CANNULA_INSERTED
            PodStatus.RUNNING_ABOVE_MIN_VOLUME, PodStatus.RUNNING_BELOW_MIN_VOLUME -> ActivationProgress.COMPLETED
            else -> activationProgress
        }
        if (recoveredProgress.isAtLeast(activationProgress)) {
            activationProgress = recoveredProgress
        }
        return activationProgress
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

    override suspend fun loadFromPreferences() {
        preferences?.let { prefs ->
            uniqueId = prefs.getInt(KEY_UNIQUE_ID, 0).first().takeIf { it != 0 }
            lotNumber = prefs.getLong(KEY_LOT_NUMBER, 0L).first().takeIf { it != 0L }
            podSequenceNumber = prefs.getLong(KEY_POD_SEQ_NUMBER, 0L).first().takeIf { it != 0L }
            bluetoothAddress = prefs.getString(KEY_BLE_ADDRESS).first()
            ltk = prefs.getString(KEY_LTK).first()?.takeIf { it.isNotEmpty() }?.let { hex ->
                try {
                    hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                } catch (_: Exception) {
                    null
                }
            }
            activatedAtTimestampMs = prefs.getLong(KEY_ACTIVATED_AT, 0L).first().takeIf { it != 0L }
            lastNonce = prefs.getInt(KEY_LAST_NONCE, 0).first().takeIf { it != 0 }
            val progressName = prefs.getString(KEY_ACTIVATION_PROGRESS).first()
            activationProgress = if (!progressName.isNullOrEmpty()) {
                try {
                    ActivationProgress.valueOf(progressName)
                } catch (_: Exception) {
                    ActivationProgress.NOT_STARTED
                }
            } else {
                ActivationProgress.NOT_STARTED
            }
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