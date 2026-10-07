package de.dh.pump.omnipod.dash

import de.dh.daps.common.model.BasalStatus
import de.dh.daps.common.model.BolusDeliveryState
import de.dh.daps.common.model.BolusEvent
import de.dh.daps.common.model.BolusStatus
import de.dh.daps.common.model.Expiration
import de.dh.daps.common.model.InsulinAmount
import de.dh.daps.common.model.InsulinConcentration
import de.dh.daps.common.model.InsulinHistory
import de.dh.daps.common.model.InsulinPump
import de.dh.daps.common.model.InsulinPumpStatus
import de.dh.daps.common.model.PumpAlerts
import de.dh.daps.common.model.PumpCapabilities
import de.dh.daps.common.model.PumpHardwareInformation
import de.dh.daps.common.model.data.InsulinProfile
import de.dh.daps.common.model.data.Timestamp
import de.dh.daps.common.ui.UiText
import de.dh.pump.omnipod.protocol.ble.OmnipodDashBleManager
import de.dh.pump.omnipod.protocol.command.DeactivateCommand
import de.dh.pump.omnipod.protocol.command.GetStatusCommand
import de.dh.pump.omnipod.protocol.command.ProgramBolusCommand
import de.dh.pump.omnipod.protocol.command.ProgramTempBasalCommand
import de.dh.pump.omnipod.protocol.command.StopDeliveryCommand
import de.dh.pump.omnipod.protocol.definition.PodConstants
import de.dh.pump.omnipod.protocol.state.OmnipodDashPodStateManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

data class OmnipodDashStatusData(
    override val pumpSuspended: Boolean = false,
    override val batteryRemainingPercent: Int = 100,
    override val reservoirRemainingUnits: InsulinAmount = InsulinAmount.ZERO,
    override val lastSyncTimestamp: Timestamp = Timestamp.now()
) : InsulinPumpStatus

class OmnipodDashPump(
    private val podStateManager: OmnipodDashPodStateManager,
    private val bleManager: OmnipodDashBleManager
) : InsulinPump {

    override val insulinPumpId: String = PUMP_ID
    override val insulinPumpDisplayName: UiText = UiText.StringResource(R.string.omnipod_dash_pump_display_name)
    override var insulinConcentration: InsulinConcentration = InsulinConcentration.U100

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

    override suspend fun bolus(amount: InsulinAmount, bolusId: String?) {
        val pulseCount = (amount.iu / PodConstants.POD_PULSE_BOLUS_UNITS).toInt().toShort()
        val nonce = 1229869870
        val cmd = ProgramBolusCommand(
            uniqueId = podStateManager.uniqueId ?: 0,
            sequenceNumber = 1,
            nonce = nonce,
            pulseCount = pulseCount
        )
        bleManager.sendCommand(cmd).getOrThrow()
        _bolusStatus.value = BolusStatus(
            state = BolusDeliveryState.DELIVERING,
            bolusId = bolusId,
            targetAmount = amount,
            deliveredAmount = InsulinAmount.ZERO,
            timestamp = Timestamp.now()
        )
        _bolusEvents.emit(
            BolusEvent.Started(
                bolusId = bolusId,
                targetAmount = amount,
                timestamp = Timestamp.now()
            )
        )
    }

    override suspend fun stopBolus() {
        val nonce = 1229869870
        val cmd = StopDeliveryCommand(
            uniqueId = podStateManager.uniqueId ?: 0,
            sequenceNumber = 2,
            nonce = nonce,
            stopBasal = false,
            stopBolus = true,
            stopTempBasal = false
        )
        bleManager.sendCommand(cmd).getOrThrow()
        val current = _bolusStatus.value
        _bolusStatus.value = current.copy(state = BolusDeliveryState.STOPPED)
        _bolusEvents.emit(
            BolusEvent.Stopped(
                bolusId = current.bolusId,
                targetAmount = current.targetAmount,
                deliveredAmount = current.deliveredAmount,
                timestamp = Timestamp.now()
            )
        )
    }

    override suspend fun tempBasal(percent: Int, durationHours: Int) {
        val nonce = 1229869870
        val pulseCount = (percent * 1).toShort()
        val durationHalfHours = (durationHours * 2).toShort()
        val cmd = ProgramTempBasalCommand(
            uniqueId = podStateManager.uniqueId ?: 0,
            sequenceNumber = 3,
            nonce = nonce,
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
    }

    override suspend fun cancelTempBasal() {
        val nonce = 1229869870
        val cmd = StopDeliveryCommand(
            uniqueId = podStateManager.uniqueId ?: 0,
            sequenceNumber = 4,
            nonce = nonce,
            stopBasal = false,
            stopBolus = false,
            stopTempBasal = true
        )
        bleManager.sendCommand(cmd).getOrThrow()
        _basalStatus.value = BasalStatus(isTempBasal = false)
    }

    override suspend fun setSuspend(suspended: Boolean) {
        val nonce = 1229869870
        val cmd = StopDeliveryCommand(
            uniqueId = podStateManager.uniqueId ?: 0,
            sequenceNumber = 5,
            nonce = nonce,
            stopBasal = suspended,
            stopBolus = suspended,
            stopTempBasal = suspended
        )
        bleManager.sendCommand(cmd).getOrThrow()
        _basalStatus.value = _basalStatus.value.copy(isSuspended = suspended)
        _pumpStatus.value = OmnipodDashStatusData(pumpSuspended = suspended)
    }

    override suspend fun setProfile(profile: InsulinProfile) {
        // Profile programming logic
    }

    override suspend fun syncHistory() {
        // History sync logic
    }

    override suspend fun refreshStatus() {
        val cmd = GetStatusCommand(
            uniqueId = podStateManager.uniqueId ?: 0,
            sequenceNumber = 6
        )
        bleManager.sendCommand(cmd).getOrThrow()
    }

    override fun stop() {
        _isConnected.value = false
        bleManager.disconnect()
    }

    companion object {
        const val PUMP_ID = "de.dh.daps.plugin.omnipod.dash"
    }
}