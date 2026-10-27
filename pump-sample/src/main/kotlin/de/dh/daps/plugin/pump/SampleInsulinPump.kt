package de.dh.daps.plugin.pump

import de.dh.daps.common.domain.model.therapy.BasalStatus
import de.dh.daps.common.domain.model.therapy.BolusEvent
import de.dh.daps.common.domain.model.therapy.BolusStatus
import de.dh.daps.common.domain.model.therapy.InsulinAmount
import de.dh.daps.common.domain.model.therapy.InsulinConcentration
import de.dh.daps.common.domain.model.therapy.InsulinHistory
import de.dh.daps.common.domain.model.therapy.InsulinPump
import de.dh.daps.common.domain.model.therapy.InsulinPumpStatus
import de.dh.daps.common.domain.model.therapy.PumpAlerts
import de.dh.daps.common.domain.model.therapy.PumpCapabilities
import de.dh.daps.common.domain.model.therapy.PumpHardwareInformation
import de.dh.daps.common.domain.model.therapy.PumpLifecycleState
import de.dh.daps.common.domain.model.therapy.InsulinProfile
import de.dh.daps.common.domain.model.therapy.Timestamp
import de.dh.daps.common.ui.UiText
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

class SampleInsulinPump : InsulinPump {

    // *************************** Insulin pump members ********************************

    override val insulinPumpId: String = SampleInsulinPumpDriver.DRIVER_ID
    override val insulinPumpDisplayName: UiText = UiText.StringResource(R.string.sample_pump_device_display_name)

    override val insulinTypeId: StateFlow<String?> = MutableStateFlow(null)
    override val insulinConcentration: StateFlow<InsulinConcentration> = MutableStateFlow(InsulinConcentration.U100)

    override val hardwareInformation: StateFlow<PumpHardwareInformation?> = MutableStateFlow(
        PumpHardwareInformation(
            manufacturer = "Sample Manufacturer",
            model = "Sample Model",
            serialNumber = "12345678"
        )
    )
    override val pumpCapabilities: StateFlow<PumpCapabilities> = MutableStateFlow(
        PumpCapabilities(
            minBasalRate = InsulinAmount(0.05),
            supportsZeroBasal = true,
            minBasalIncrement = InsulinAmount(0.01),
            minBolusAmount = InsulinAmount(0.1),
            minBolusIncrement = InsulinAmount(0.1),
            maxBolusSize = InsulinAmount(25.0)
        )
    )
    override val isConnected: StateFlow<Boolean> = MutableStateFlow(true)
    override val pumpStatus: StateFlow<InsulinPumpStatus> = MutableStateFlow(
        object : InsulinPumpStatus {
            override val pumpSuspended: Boolean = false
            override val batteryRemainingPercent: Int = 85
            override val reservoirRemainingUnits: InsulinAmount = InsulinAmount(120.5)
            override val lastSyncTimestamp: Timestamp = Timestamp.now()
            override val lifecycleState: PumpLifecycleState = PumpLifecycleState.Durable()
        }
    )
    override val alerts: StateFlow<PumpAlerts> = MutableStateFlow(PumpAlerts())
    override val basalStatus: StateFlow<BasalStatus> = MutableStateFlow(BasalStatus(activeRate = InsulinAmount(0.5)))
    override val bolusStatus: StateFlow<BolusStatus> = MutableStateFlow(BolusStatus())
    override val bolusEvents: SharedFlow<BolusEvent> = MutableSharedFlow()
    override val history: StateFlow<InsulinHistory?> = MutableStateFlow(null)

    override suspend fun bolus(amount: InsulinAmount, bolusId: String?) {
        // TODO: Implement bolus delivery
    }

    override suspend fun stopBolus() {
        // TODO: Stop ongoing bolus
    }

    override suspend fun tempBasal(percent: Int, durationHours: Int) {
        // TODO: Set temporary basal rate
    }

    override suspend fun cancelTempBasal() {
        // TODO: Cancel temporary basal rate
    }

    override suspend fun setSuspend(suspended: Boolean) {
        // TODO: Set pump suspend state
    }

    override suspend fun setProfile(profile: InsulinProfile) {
        // TODO: Set therapy profile
    }

    override suspend fun syncHistory(start: Timestamp) {
        // TODO: Sync pump history
    }

    override suspend fun refreshStatus() {
        // TODO: Refresh pump status from hardware
    }

    override fun stop() {
        // TODO
    }
}