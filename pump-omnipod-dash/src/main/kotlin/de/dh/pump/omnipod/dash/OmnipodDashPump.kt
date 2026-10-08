package de.dh.pump.omnipod.dash

import android.content.Context
import de.dh.daps.common.model.BasalStatus
import de.dh.daps.common.model.BolusDeliveryState
import de.dh.daps.common.model.BolusEvent
import de.dh.daps.common.model.BolusStatus
import de.dh.daps.common.model.Expiration
import de.dh.daps.common.model.ExpirationDate
import de.dh.daps.common.model.InsulinAmount
import de.dh.daps.common.model.InsulinConcentration
import de.dh.daps.common.model.InsulinHistory
import de.dh.daps.common.model.InsulinPump
import de.dh.daps.common.model.InsulinPumpStatus
import de.dh.daps.common.model.PumpAlerts
import de.dh.daps.common.model.PumpCapabilities
import de.dh.daps.common.model.PumpHardwareInformation
import de.dh.daps.common.model.ReplaceableComponentType
import de.dh.daps.common.model.data.InsulinProfile
import de.dh.daps.common.model.data.Timestamp
import de.dh.daps.common.ui.UiText
import de.dh.pump.omnipod.dash.db.DashHistoryDatabase
import de.dh.pump.omnipod.dash.db.DashHistoryEntity
import de.dh.pump.omnipod.protocol.ble.OmnipodDashBleManager
import de.dh.pump.omnipod.protocol.command.GetStatusCommand
import de.dh.pump.omnipod.protocol.command.ProgramBolusCommand
import de.dh.pump.omnipod.protocol.command.ProgramTempBasalCommand
import de.dh.pump.omnipod.protocol.command.StopDeliveryCommand
import de.dh.pump.omnipod.protocol.state.OmnipodDashPodStateManager
import de.dh.pump.omnipod.protocol.util.PodPulseCalculator
import de.dh.pump.omnipod.protocol.util.ProfileToBasalScheduleConverter
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit

data class OmnipodDashStatusData(
    override val pumpSuspended: Boolean = false,
    override val batteryRemainingPercent: Int = 100,
    override val reservoirRemainingUnits: InsulinAmount = InsulinAmount.ZERO,
    override val lastSyncTimestamp: Timestamp = Timestamp.now()
) : InsulinPumpStatus

class OmnipodDashPump(
    private val podStateManager: OmnipodDashPodStateManager,
    private val bleManager: OmnipodDashBleManager,
    private val context: Context? = null
) : InsulinPump {

    override val insulinPumpId: String = PUMP_ID
    override val insulinPumpDisplayName: UiText = UiText.StringResource(R.string.omnipod_dash_pump_display_name)
    override var insulinConcentration: InsulinConcentration = InsulinConcentration.U100

    private var sequenceNumber: Short = 1

    private val _startDate = MutableStateFlow<Timestamp?>(null)
    override val startDate: StateFlow<Timestamp?> = _startDate.asStateFlow()

    private val _isExpired = MutableStateFlow(false)
    override val isExpired: StateFlow<Boolean> = _isExpired.asStateFlow()

    private val _expirations = MutableStateFlow<List<Expiration>>(emptyList())
    override val expirations: StateFlow<List<Expiration>> = _expirations.asStateFlow()

    private val _hardwareInformation = MutableStateFlow<PumpHardwareInformation?>(
        PumpHardwareInformation(
            manufacturer = "Insulet",
            model = "Omnipod Dash",
            serialNumber = podStateManager.lotNumber?.toString() ?: "Unknown"
        )
    )
    override val hardwareInformation: StateFlow<PumpHardwareInformation?> = _hardwareInformation.asStateFlow()

    private val _pumpCapabilities = MutableStateFlow(
        PumpCapabilities(
            minBasalRate = InsulinAmount(0.05),
            supportsZeroBasal = true,
            minBasalIncrement = InsulinAmount(0.05),
            minBolusAmount = InsulinAmount(0.05),
            minBolusIncrement = InsulinAmount(0.05),
            maxBolusSize = InsulinAmount(30.0)
        )
    )
    override val pumpCapabilities: StateFlow<PumpCapabilities> = _pumpCapabilities.asStateFlow()

    private val _isConnected = MutableStateFlow(true)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _pumpStatus = MutableStateFlow<InsulinPumpStatus>(OmnipodDashStatusData())
    override val pumpStatus: StateFlow<InsulinPumpStatus> = _pumpStatus.asStateFlow()

    private val _alerts = MutableStateFlow(PumpAlerts())
    override val alerts: StateFlow<PumpAlerts> = _alerts.asStateFlow()

    private val _basalStatus = MutableStateFlow(BasalStatus())
    override val basalStatus: StateFlow<BasalStatus> = _basalStatus.asStateFlow()

    private val _bolusStatus = MutableStateFlow(BolusStatus())
    override val bolusStatus: StateFlow<BolusStatus> = _bolusStatus.asStateFlow()

    private val _bolusEvents = MutableSharedFlow<BolusEvent>()
    override val bolusEvents: SharedFlow<BolusEvent> = _bolusEvents.asSharedFlow()

    private val _history = MutableStateFlow<InsulinHistory?>(null)
    override val history: StateFlow<InsulinHistory?> = _history.asStateFlow()

    private val db: DashHistoryDatabase? by lazy {
        context?.let { DashHistoryDatabase.getInstance(it) }
    }

    init {
        updateExpirationState()
    }

    private fun nextSequenceNumber(): Short {
        val seq = sequenceNumber
        sequenceNumber = ((sequenceNumber + 1) and 0x0F).toShort()
        return seq
    }

    private fun getNonce(): Int {
        return podStateManager.lastNonce ?: 0x49534141
    }

    private fun updateExpirationState() {
        val activationTime = podStateManager.activatedAtTimestampMs
        if (activationTime != null && activationTime > 0) {
            val start = Timestamp(activationTime)
            _startDate.value = start
            val expiresAtMs = activationTime + TimeUnit.HOURS.toMillis(POD_DURATION_HOURS)
            val expiresAt = Timestamp(expiresAtMs)
            val graceAt = Timestamp(activationTime + TimeUnit.HOURS.toMillis(POD_DURATION_HOURS + POD_GRACE_HOURS))
            val nowMs = System.currentTimeMillis()

            _isExpired.value = nowMs >= graceAt.ms
            _expirations.value = listOf(
                Expiration(
                    type = ReplaceableComponentType.Patch,
                    date = ExpirationDate.Hard(expiresAt),
                    graceUntil = graceAt
                )
            )
        }
    }

    override suspend fun bolus(amount: InsulinAmount, bolusId: String?) {
        val pulseCount = PodPulseCalculator.unitsToPulses(amount.iu)
        val cmd = ProgramBolusCommand(
            uniqueId = podStateManager.uniqueId ?: 0,
            sequenceNumber = nextSequenceNumber(),
            nonce = getNonce(),
            pulseCount = pulseCount
        )
        bleManager.sendCommand(cmd).getOrThrow()

        val now = Timestamp.now()
        _bolusStatus.value = BolusStatus(
            state = BolusDeliveryState.DELIVERING,
            bolusId = bolusId,
            targetAmount = amount,
            deliveredAmount = InsulinAmount.ZERO,
            timestamp = now
        )
        _bolusEvents.emit(
            BolusEvent.Started(
                bolusId = bolusId,
                targetAmount = amount,
                timestamp = now
            )
        )
        logEvent("BOLUS", "Programmed bolus of ${amount.iu} U", amount.iu)
    }

    override suspend fun stopBolus() {
        val cmd = StopDeliveryCommand(
            uniqueId = podStateManager.uniqueId ?: 0,
            sequenceNumber = nextSequenceNumber(),
            nonce = getNonce(),
            stopBasal = false,
            stopBolus = true,
            stopTempBasal = false
        )
        bleManager.sendCommand(cmd).getOrThrow()

        val current = _bolusStatus.value
        val now = Timestamp.now()
        _bolusStatus.value = current.copy(state = BolusDeliveryState.STOPPED)
        _bolusEvents.emit(
            BolusEvent.Stopped(
                bolusId = current.bolusId,
                targetAmount = current.targetAmount,
                deliveredAmount = current.deliveredAmount,
                timestamp = now
            )
        )
        logEvent("STOP_BOLUS", "Stopped running bolus", 0.0)
    }

    override suspend fun tempBasal(percent: Int, durationHours: Int) {
        val pulseCount = (percent * 1).toShort()
        val durationHalfHours = (durationHours * 2).toShort()
        val cmd = ProgramTempBasalCommand(
            uniqueId = podStateManager.uniqueId ?: 0,
            sequenceNumber = nextSequenceNumber(),
            nonce = getNonce(),
            pulseCount = pulseCount,
            durationHalfHours = durationHalfHours
        )
        bleManager.sendCommand(cmd).getOrThrow()

        _basalStatus.value = BasalStatus(
            activeRate = InsulinAmount(percent / 100.0),
            isTempBasal = true,
            tempBasalPercent = percent,
            isSuspended = false
        )
        logEvent("TEMP_BASAL", "Started temp basal $percent% for $durationHours h", 0.0)
    }

    override suspend fun cancelTempBasal() {
        val cmd = StopDeliveryCommand(
            uniqueId = podStateManager.uniqueId ?: 0,
            sequenceNumber = nextSequenceNumber(),
            nonce = getNonce(),
            stopBasal = false,
            stopBolus = false,
            stopTempBasal = true
        )
        bleManager.sendCommand(cmd).getOrThrow()
        _basalStatus.value = BasalStatus(isTempBasal = false)
        logEvent("CANCEL_TEMP_BASAL", "Cancelled active temp basal", 0.0)
    }

    override suspend fun setSuspend(suspended: Boolean) {
        val cmd = StopDeliveryCommand(
            uniqueId = podStateManager.uniqueId ?: 0,
            sequenceNumber = nextSequenceNumber(),
            nonce = getNonce(),
            stopBasal = suspended,
            stopBolus = suspended,
            stopTempBasal = suspended
        )
        bleManager.sendCommand(cmd).getOrThrow()
        _basalStatus.value = _basalStatus.value.copy(isSuspended = suspended)
        _pumpStatus.value = OmnipodDashStatusData(pumpSuspended = suspended)
        logEvent("SUSPEND", "Delivery suspended: $suspended", 0.0)
    }

    override suspend fun setProfile(profile: InsulinProfile) {
        val schedulePulses = ProfileToBasalScheduleConverter.convertProfileToHalfHourPulses(profile)
        val currentRate = profile.basalBlocks.firstOrNull()?.amount ?: 0.0
        _basalStatus.value = _basalStatus.value.copy(
            activeRate = InsulinAmount(currentRate),
            isTempBasal = false,
            isSuspended = false
        )
        logEvent("PROFILE", "Set active basal profile: ${profile.name} (${schedulePulses.size} segments)", 0.0)
    }

    override suspend fun syncHistory() {
        refreshStatus()
        db?.let {
            val events = it.historyDao().getRecentEvents(50)
            // Log history
        }
    }

    override suspend fun refreshStatus() {
        val cmd = GetStatusCommand(
            uniqueId = podStateManager.uniqueId ?: 0,
            sequenceNumber = nextSequenceNumber()
        )
        bleManager.sendCommand(cmd).getOrThrow()
        _pumpStatus.value = OmnipodDashStatusData(
            lastSyncTimestamp = Timestamp.now()
        )
    }

    override fun stop() {
        _isConnected.value = false
        bleManager.disconnect()
    }

    private suspend fun logEvent(eventType: String, detailText: String, units: Double) {
        db?.historyDao()?.insertEvent(
            DashHistoryEntity(
                timestampMs = System.currentTimeMillis(),
                eventType = eventType,
                detailText = detailText,
                unitsDelivered = units
            )
        )
    }

    companion object {
        const val PUMP_ID = "de.dh.daps.plugin.omnipod.dash"
        private const val POD_DURATION_HOURS = 72L
        private const val POD_GRACE_HOURS = 8L
    }
}