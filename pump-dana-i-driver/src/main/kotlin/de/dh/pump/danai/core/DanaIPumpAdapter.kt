package de.dh.pump.danai.core

import de.dh.pump.danai.core.connection.DanaILink
import de.dh.pump.danai.core.model.DanaIBolusSpeed
import de.dh.daps.common.model.BasalStatus
import de.dh.daps.common.model.BolusEvent
import de.dh.daps.common.model.BolusStatus
import de.dh.daps.common.model.HardwareInformation
import de.dh.daps.common.model.InsulinAmount
import de.dh.daps.common.model.InsulinConcentration
import de.dh.daps.common.model.InsulinHistory
import de.dh.daps.common.model.InsulinPump
import de.dh.daps.common.model.InsulinPumpStatus
import de.dh.daps.common.model.PumpAlerts
import de.dh.daps.common.model.PumpCapabilities
import de.dh.daps.common.model.data.InsulinProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Adapter that maps the Dana-i specific [DanaIPump] to the generic [InsulinPump] interface.
 */
class DanaIPumpAdapter(
    val pump: DanaIPump,
    val controller: DanaIController,
    private val scope: CoroutineScope
) : InsulinPump {
    override var insulinConcentration: InsulinConcentration
        get() = pump.insulinConcentration
        set(value) {
            pump.insulinConcentration = value
        }
    override val hardwareInformation: StateFlow<HardwareInformation?> = pump.hardware.map { hi ->
        hi?.let {
            HardwareInformation(
                DANA_I_MANUFACTURER,
                it.hardwareModel.toString(),
                it.serialNumber,
                "${it.productCode} (${it.bleModel})"
            )
        }
    }.stateIn(scope, SharingStarted.Eagerly, null)

    override val pumpCapabilities: StateFlow<PumpCapabilities> = pump.pumpCapabilities.map {
        it ?: PumpCapabilities(
            minBasalRate = DANA_I_MIN_BASAL_RATE,
            supportsZeroBasal = true,
            minBasalIncrement = DANA_I_DEFAULT_MIN_BASAL_INCREMENT,
            minBolusIncrement = DANA_I_DEFAULT_MIN_BOLUS_INCREMENT,
            maxBolusSize = DANA_I_DEFAULT_MAX_BOLUS_VALUE
        )
    }.stateIn(
        scope, SharingStarted.Eagerly, PumpCapabilities(
            minBasalRate = DANA_I_MIN_BASAL_RATE,
            supportsZeroBasal = true,
            minBasalIncrement = DANA_I_DEFAULT_MIN_BASAL_INCREMENT,
            minBolusIncrement = DANA_I_DEFAULT_MIN_BOLUS_INCREMENT,
            maxBolusSize = DANA_I_DEFAULT_MAX_BOLUS_VALUE
        )
    )

    override val isConnected: StateFlow<Boolean> = pump.isConnected
    override val pumpStatus: StateFlow<InsulinPumpStatus> = pump.status
    override val alerts: StateFlow<PumpAlerts> get() = pump.alerts
    override val basalStatus: StateFlow<BasalStatus> get() = pump.basalStatus
    override val bolusStatus: StateFlow<BolusStatus> get() = pump.bolusStatus
    override val bolusEvents: SharedFlow<BolusEvent> get() = pump.bolusEvents

    override val history: StateFlow<InsulinHistory?> get() = pump.history

    override suspend fun bolus(amount: InsulinAmount, bolusId: String?) {
        // TODO: Read bolus speed from configuration
        val speed = DanaIBolusSpeed.U30_SECONDS
        pump.bolus(amount, speed, bolusId)
    }

    override suspend fun stopBolus() {
        pump.stopBolus()
    }

    override suspend fun tempBasal(percent: Int, durationHours: Int) {
        pump.tempBasal(percent, durationHours)
    }

    override suspend fun cancelTempBasal() {
        pump.cancelTempBasal()
    }

    override suspend fun setSuspend(suspended: Boolean) {
        pump.setSuspend(suspended)
    }

    override suspend fun setProfile(profile: InsulinProfile) {
        pump.setProfile(profile)
    }

    override suspend fun syncHistory() {
        pump.syncHistory()
    }

    override suspend fun refreshStatus() {
        pump.refreshStatus()
    }

    override fun stop() {
        controller.stop()
    }

    companion object {
        fun create(link: DanaILink, appLogger: DanaILogger, scope: CoroutineScope): DanaIPumpAdapter {
            val pump = DanaIPump()
            val controller = DanaIController(pump, link, appLogger, scope)
            return DanaIPumpAdapter(pump, controller, scope)
        }
    }
}