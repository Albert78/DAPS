package de.dh.pump.omnipod.protocol.state

import de.dh.daps.common.model.InsulinConcentration
import de.dh.daps.common.model.PluginPreferences
import de.dh.pump.omnipod.protocol.definition.ActivationProgress
import de.dh.pump.omnipod.protocol.definition.AlarmType
import de.dh.pump.omnipod.protocol.definition.AlertType
import de.dh.pump.omnipod.protocol.definition.BasalProgram
import de.dh.pump.omnipod.protocol.definition.BolusType
import de.dh.pump.omnipod.protocol.definition.DeliveryStatus
import de.dh.pump.omnipod.protocol.definition.PodStatus
import de.dh.pump.omnipod.protocol.response.AlarmStatusResponse
import de.dh.pump.omnipod.protocol.response.DefaultStatusResponse
import de.dh.pump.omnipod.protocol.response.NakResponse
import de.dh.pump.omnipod.protocol.response.SetUniqueIdResponse
import de.dh.pump.omnipod.protocol.response.VersionResponse
import de.dh.pump.omnipod.protocol.util.PodPulseCalculator
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.EnumSet
import java.util.TimeZone
import kotlin.math.roundToInt

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

    var lastAlarmType: AlarmType?
    var lastPdmRef: String?
    var lastFaultEventByte: Byte?

    var tempBasal: TempBasal?
    var basalProgram: BasalProgram?
    var lastBolus: LastBolus?
    var activeCommand: ActiveCommand?

    var timeZoneOffset: Int?
    var lastUpdatedSystem: Long
    var basalExpected: Double?
    var bolusPulsesDelivered: Short?
    val basalPulsesDelivered: Short?
    val basalDelivered: Double
    val basalDrift: Double
    var lastBasalCorrectionTime: Long?
    var basalCorrectionInProgress: Boolean
    var insulinTypeId: String?
    var insulinConcentration: InsulinConcentration

    fun updateFromDefaultStatusResponse(response: DefaultStatusResponse)
    fun updateFromAlarmStatusResponse(response: AlarmStatusResponse)
    fun updateFromVersionResponse(response: VersionResponse)
    fun updateFromSetUniqueIdResponse(response: SetUniqueIdResponse)
    fun resyncNonceFromNak(nakResponse: NakResponse)
    fun recoverActivationFromPodStatus(status: PodStatus): ActivationProgress
    fun createActiveCommand(
        sequenceNumber: Short,
        commandType: ActiveCommandType,
        requestedUnits: Double? = null,
        pulseCount: Short? = null,
        historyId: Long? = null
    ): ActiveCommand
    fun clearActiveCommand()

    fun updateTimeZone()
    fun integrateExpectedDelivery(
        startTime: Long,
        endTime: Long,
        timeZoneOffset: Int?,
        tempBasal: TempBasal?,
        basalProgram: BasalProgram?
    ): Double?
    fun calculateBolusPulseIncrease(
        previousTotalPulses: Short,
        newTotalPulses: Short,
        previousBolusPulsesRemaining: Short?,
        newBolusPulsesRemaining: Short
    ): Short
    fun needsBasalCorrection(): Boolean

    suspend fun saveToPreferences()
    suspend fun loadFromPreferences()
    fun reset()

    @Serializable
    enum class ActiveCommandType {
        BOLUS,
        TEMP_BASAL,
        STOP_DELIVERY,
        BASAL_SCHEDULE
    }

    @Serializable
    data class ActiveCommand(
        val sequenceNumber: Short,
        val timestampMs: Long,
        val commandType: ActiveCommandType,
        val requestedUnits: Double? = null,
        val pulseCount: Short? = null,
        val historyId: Long? = null,
        val pulsesDeliveredAtStart: Short? = null
    )

    @Serializable
    data class TempBasal(val startTime: Long, val rate: Double, val durationInMinutes: Short)

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

    override var insulinTypeId: String? = null
    override var insulinConcentration: InsulinConcentration = InsulinConcentration.U100

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

    override var lastAlarmType: AlarmType? = null
    override var lastPdmRef: String? = null
    override var lastFaultEventByte: Byte? = null

    override var tempBasal: OmnipodDashPodStateManager.TempBasal? = null
    override var basalProgram: BasalProgram? = null
    override var lastBolus: OmnipodDashPodStateManager.LastBolus? = null
    override var activeCommand: OmnipodDashPodStateManager.ActiveCommand? = null

    override var timeZoneOffset: Int? = null
    override var lastUpdatedSystem: Long = System.currentTimeMillis()
    override var basalExpected: Double? = null
    override var bolusPulsesDelivered: Short? = null

    override val basalPulsesDelivered: Short?
        get() = pulsesDelivered?.let { total ->
            bolusPulsesDelivered?.let { bolus ->
                (total - bolus).coerceAtLeast(0).toShort()
            }
        }

    override val basalDelivered: Double
        get() = (basalPulsesDelivered?.toInt() ?: 0) * PodPulseCalculator.INSULIN_PER_PULSE_U

    override val basalDrift: Double
        get() = basalDelivered - (basalExpected ?: basalDelivered)

    override var lastBasalCorrectionTime: Long? = null
    override var basalCorrectionInProgress: Boolean = false

    override fun updateTimeZone() {
        val now = System.currentTimeMillis()
        timeZoneOffset = TimeZone.getDefault().getOffset(now)
    }

    override fun integrateExpectedDelivery(
        startTime: Long,
        endTime: Long,
        timeZoneOffset: Int?,
        tempBasal: OmnipodDashPodStateManager.TempBasal?,
        basalProgram: BasalProgram?
    ): Double? {
        if (timeZoneOffset == null) return null
        if (startTime > endTime) return null
        if (startTime == endTime) return 0.0

        val boundaries = mutableSetOf(startTime, endTime)

        tempBasal?.let { tb ->
            val tempStart = tb.startTime
            val tempEnd = tb.startTime + tb.durationInMinutes * 60_000L
            if (tempStart in startTime until endTime) boundaries.add(tempStart)
            if (tempEnd in startTime until endTime) boundaries.add(tempEnd)
        }

        basalProgram?.segments?.forEach { segment ->
            val dayStartLocal = ((startTime + timeZoneOffset) / 86400_000L) * 86400_000L - timeZoneOffset
            var segmentStart = dayStartLocal + segment.startSlotIndex.toLong() * 30 * 60_000L

            if (segmentStart <= startTime) {
                segmentStart += 86400_000L
            }

            while (segmentStart < endTime) {
                boundaries.add(segmentStart)
                segmentStart += 86400_000L
            }
        }

        val deviceOffsetMs = TimeZone.getDefault().getOffset(startTime)
        val podTimeAdjustmentMs = timeZoneOffset - deviceOffsetMs

        val sortedBoundaries = boundaries.sorted()
        var totalDelivery = 0.0

        for (i in 0 until sortedBoundaries.size - 1) {
            val boundaryStart = sortedBoundaries[i]
            val boundaryEnd = sortedBoundaries[i + 1]
            val segmentHours = (boundaryEnd - boundaryStart) / 3600_000.0
            val segmentMid = (boundaryStart + boundaryEnd) / 2

            val rate = tempBasal?.let { tb ->
                val tempEnd = tb.startTime + tb.durationInMinutes * 60_000L
                tb.rate.takeIf { segmentMid in tb.startTime until tempEnd }
            } ?: basalProgram?.rateAt(segmentMid + podTimeAdjustmentMs) ?: return null

            totalDelivery += rate * segmentHours
        }

        return totalDelivery
    }

    override fun calculateBolusPulseIncrease(
        previousTotalPulses: Short,
        newTotalPulses: Short,
        previousBolusPulsesRemaining: Short?,
        newBolusPulsesRemaining: Short
    ): Short {
        var increase = newTotalPulses - previousTotalPulses
        if (previousBolusPulsesRemaining != null) {
            val expectedIncrease = previousBolusPulsesRemaining - newBolusPulsesRemaining
            if (increase > expectedIncrease) {
                increase = expectedIncrease
            }
        }
        return increase.coerceAtLeast(0).toShort()
    }

    override fun needsBasalCorrection(): Boolean {
        val correctionThreshold = -PodPulseCalculator.INSULIN_PER_PULSE_U / 2.0 // -0.025 U

        if (!isActivationCompleted) return false
        if (isSuspended) return false
        val status = podStatus
        if (status == null || !status.isRunning()) return false

        lastBasalCorrectionTime?.let {
            if (System.currentTimeMillis() - it < 2 * 60 * 1000L) return false
        }

        val drift = basalDrift

        if (drift >= PodPulseCalculator.INSULIN_PER_PULSE_U * 2 || drift <= -PodPulseCalculator.INSULIN_PER_PULSE_U * 2) {
            basalExpected = basalDelivered
            return false
        }

        if (drift > correctionThreshold) {
            return false
        }

        if (tempBasal?.rate == 0.0) {
            val timeSinceLastBolus = lastBolus?.startTime?.let { System.currentTimeMillis() - it }
            if (timeSinceLastBolus == null || timeSinceLastBolus >= 5 * 60 * 1000L) {
                return false
            }
        }

        return true
    }

    override fun createActiveCommand(
        sequenceNumber: Short,
        commandType: OmnipodDashPodStateManager.ActiveCommandType,
        requestedUnits: Double?,
        pulseCount: Short?,
        historyId: Long?
    ): OmnipodDashPodStateManager.ActiveCommand {
        val cmd = OmnipodDashPodStateManager.ActiveCommand(
            sequenceNumber = sequenceNumber,
            timestampMs = System.currentTimeMillis(),
            commandType = commandType,
            requestedUnits = requestedUnits,
            pulseCount = pulseCount,
            historyId = historyId,
            pulsesDeliveredAtStart = pulsesDelivered
        )
        activeCommand = cmd
        return cmd
    }

    override fun clearActiveCommand() {
        activeCommand = null
    }

    override fun updateFromDefaultStatusResponse(response: DefaultStatusResponse) {
        val now = System.currentTimeMillis()
        val previousTotalPulses = pulsesDelivered
        val previousBolusRemaining = lastBolus?.let {
            (it.bolusUnitsRemaining / PodPulseCalculator.INSULIN_PER_PULSE_U).roundToInt().toShort()
        }

        if (timeZoneOffset == null) {
            updateTimeZone()
        }

        // 1. Update basal expected
        basalExpected = basalExpected?.let { currentExp ->
            integrateExpectedDelivery(
                startTime = lastUpdatedSystem,
                endTime = now,
                timeZoneOffset = timeZoneOffset,
                tempBasal = tempBasal,
                basalProgram = basalProgram
            )?.let { delta -> currentExp + delta }
        } ?: basalDelivered.takeIf { isActivationCompleted }

        // 2. Update bolus pulses delivered (exclude basal corrections)
        val totalPulsesNow = response.totalPulsesDelivered
        bolusPulsesDelivered = bolusPulsesDelivered?.let { currentBolusPulses ->
            previousTotalPulses?.takeIf { lastBolus?.deliveryComplete == false && !basalCorrectionInProgress }?.let { prevTotal ->
                (currentBolusPulses + calculateBolusPulseIncrease(
                    prevTotal,
                    totalPulsesNow,
                    previousBolusRemaining,
                    response.bolusPulsesRemaining
                )).toShort()
            } ?: currentBolusPulses
        } ?: totalPulsesNow.takeIf { isActivationCompleted }

        // 3. Update status fields
        podStatus = response.podStatus
        deliveryStatus = response.deliveryStatus
        pulsesDelivered = response.totalPulsesDelivered
        reservoirPulsesRemaining = response.reservoirPulsesRemaining
        minutesSinceActivation = response.minutesSinceActivation
        activeAlerts = response.activeAlerts
        lastUpdatedSystem = now

        // 4. Update last bolus status
        lastBolus?.let { bolus ->
            if (!bolus.deliveryComplete) {
                val remainingUnits = PodPulseCalculator.pulsesToUnits(response.bolusPulsesRemaining)
                bolus.bolusUnitsRemaining = remainingUnits
                if (response.bolusPulsesRemaining <= 0 || !response.deliveryStatus.bolusDeliveringActive()) {
                    bolus.deliveryComplete = true
                }
            }
        }

        recoverActivationFromPodStatus(response.podStatus)
    }

    override fun updateFromAlarmStatusResponse(response: AlarmStatusResponse) {
        val now = System.currentTimeMillis()
        podStatus = response.podStatus
        deliveryStatus = response.deliveryStatus
        pulsesDelivered = response.totalPulsesDelivered
        reservoirPulsesRemaining = response.reservoirPulsesRemaining
        minutesSinceActivation = response.minutesSinceActivation
        activeAlerts = response.activeAlerts
        lastAlarmType = response.alarmType
        lastPdmRef = response.pdmRef
        lastFaultEventByte = response.rawErrorEventByte
        lastUpdatedSystem = now
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

    override fun resyncNonceFromNak(nakResponse: NakResponse) {
        val syncCount = nakResponse.securityNakSyncCount
        if (syncCount > 0) {
            lastNonce = syncCount.toInt()
        }
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
            activeCommand?.let { putString(KEY_ACTIVE_COMMAND, Json.encodeToString(it)) } ?: putString(KEY_ACTIVE_COMMAND, null)

            timeZoneOffset?.let { putInt(KEY_TIMEZONE_OFFSET, it) } ?: putInt(KEY_TIMEZONE_OFFSET, Int.MIN_VALUE)
            putLong(KEY_LAST_UPDATED_SYSTEM, lastUpdatedSystem)
            basalExpected?.let { putString(KEY_BASAL_EXPECTED, it.toString()) } ?: putString(KEY_BASAL_EXPECTED, null)
            bolusPulsesDelivered?.let { putInt(KEY_BOLUS_PULSES_DELIVERED, it.toInt()) } ?: putInt(KEY_BOLUS_PULSES_DELIVERED, -1)
            lastBasalCorrectionTime?.let { putLong(KEY_LAST_BASAL_CORRECTION_TIME, it) } ?: putLong(KEY_LAST_BASAL_CORRECTION_TIME, 0L)
            tempBasal?.let { putString(KEY_TEMP_BASAL, Json.encodeToString(it)) } ?: putString(KEY_TEMP_BASAL, null)
            basalProgram?.let { putString(KEY_BASAL_PROGRAM, Json.encodeToString(it)) } ?: putString(KEY_BASAL_PROGRAM, null)
            insulinTypeId?.let { putString(KEY_INSULIN_TYPE_ID, it) } ?: putString(KEY_INSULIN_TYPE_ID, null)
            putFloat(KEY_INSULIN_CONCENTRATION, insulinConcentration.factor.toFloat())
        }
    }

    override suspend fun loadFromPreferences() {
        preferences?.let { prefs ->
            insulinTypeId = prefs.getString(KEY_INSULIN_TYPE_ID).first()
            val concFactor = prefs.getFloat(KEY_INSULIN_CONCENTRATION, 1.0f).first()
            insulinConcentration = InsulinConcentration(concFactor.toDouble())
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
            val activeCmdJson = prefs.getString(KEY_ACTIVE_COMMAND).first()
            activeCommand = if (!activeCmdJson.isNullOrEmpty()) {
                try {
                    Json.decodeFromString<OmnipodDashPodStateManager.ActiveCommand>(activeCmdJson)
                } catch (_: Exception) {
                    null
                }
            } else {
                null
            }

            val tzOff = prefs.getInt(KEY_TIMEZONE_OFFSET, Int.MIN_VALUE).first()
            timeZoneOffset = if (tzOff != Int.MIN_VALUE) tzOff else null

            val lastUpd = prefs.getLong(KEY_LAST_UPDATED_SYSTEM, 0L).first()
            if (lastUpd > 0L) lastUpdatedSystem = lastUpd

            val expStr = prefs.getString(KEY_BASAL_EXPECTED).first()
            basalExpected = expStr?.toDoubleOrNull()

            val bolusP = prefs.getInt(KEY_BOLUS_PULSES_DELIVERED, -1).first()
            bolusPulsesDelivered = if (bolusP >= 0) bolusP.toShort() else null

            val lastCorr = prefs.getLong(KEY_LAST_BASAL_CORRECTION_TIME, 0L).first()
            lastBasalCorrectionTime = if (lastCorr > 0L) lastCorr else null

            val tbJson = prefs.getString(KEY_TEMP_BASAL).first()
            tempBasal = if (!tbJson.isNullOrEmpty()) {
                try {
                    Json.decodeFromString<OmnipodDashPodStateManager.TempBasal>(tbJson)
                } catch (_: Exception) {
                    null
                }
            } else null

            val bpJson = prefs.getString(KEY_BASAL_PROGRAM).first()
            basalProgram = if (!bpJson.isNullOrEmpty()) {
                try {
                    Json.decodeFromString<BasalProgram>(bpJson)
                } catch (_: Exception) {
                    null
                }
            } else null
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
        lastAlarmType = null
        lastPdmRef = null
        lastFaultEventByte = null
        tempBasal = null
        basalProgram = null
        lastBolus = null
        activeCommand = null
        timeZoneOffset = null
        lastUpdatedSystem = System.currentTimeMillis()
        basalExpected = null
        bolusPulsesDelivered = null
        lastBasalCorrectionTime = null
        basalCorrectionInProgress = false
        insulinTypeId = null
        insulinConcentration = InsulinConcentration.U100
    }

    companion object {
        private const val KEY_INSULIN_TYPE_ID = "omnipod_dash_insulin_type_id"
        private const val KEY_INSULIN_CONCENTRATION = "omnipod_dash_insulin_concentration"
        private const val KEY_UNIQUE_ID = "omnipod_dash_unique_id"
        private const val KEY_LOT_NUMBER = "omnipod_dash_lot_number"
        private const val KEY_POD_SEQ_NUMBER = "omnipod_dash_pod_seq_number"
        private const val KEY_BLE_ADDRESS = "omnipod_dash_ble_address"
        private const val KEY_LTK = "omnipod_dash_ltk"
        private const val KEY_ACTIVATED_AT = "omnipod_dash_activated_at"
        private const val KEY_LAST_NONCE = "omnipod_dash_last_nonce"
        private const val KEY_ACTIVATION_PROGRESS = "omnipod_dash_activation_progress"
        private const val KEY_ACTIVE_COMMAND = "omnipod_dash_active_command"
        private const val KEY_TIMEZONE_OFFSET = "omnipod_dash_timezone_offset"
        private const val KEY_LAST_UPDATED_SYSTEM = "omnipod_dash_last_updated_system"
        private const val KEY_BASAL_EXPECTED = "omnipod_dash_basal_expected"
        private const val KEY_BOLUS_PULSES_DELIVERED = "omnipod_dash_bolus_pulses_delivered"
        private const val KEY_LAST_BASAL_CORRECTION_TIME = "omnipod_dash_last_basal_correction_time"
        private const val KEY_TEMP_BASAL = "omnipod_dash_temp_basal"
        private const val KEY_BASAL_PROGRAM = "omnipod_dash_basal_program"
    }
}