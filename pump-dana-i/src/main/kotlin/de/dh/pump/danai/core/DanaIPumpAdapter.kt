package de.dh.pump.danai.core

import de.dh.daps.common.model.BasalStatus
import de.dh.daps.common.model.BolusEvent
import de.dh.daps.common.model.BolusStatus
import de.dh.daps.common.model.InsulinAmount
import de.dh.daps.common.model.InsulinConcentration
import de.dh.daps.common.model.InsulinHistory
import de.dh.daps.common.model.InsulinPump
import de.dh.daps.common.model.InsulinPumpStatus
import de.dh.daps.common.model.PumpAlerts
import de.dh.daps.common.model.PumpCapabilities
import de.dh.daps.common.model.PumpHardwareInformation
import de.dh.daps.common.model.PumpLifecycleState
import de.dh.daps.common.model.data.InsulinProfile
import de.dh.daps.common.model.data.Timestamp
import de.dh.daps.common.ui.UiText
import de.dh.pump.danai.core.connection.DanaILink
import de.dh.pump.danai.core.model.DanaIBolusSpeed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.ZoneId

/**
 * Adapter that maps the Dana-i specific [DanaIPump] to the generic [InsulinPump] interface.
 */
class DanaIPumpAdapter(
    val pump: DanaIPump,
    val controller: DanaIController,
    private val scope: CoroutineScope
) : InsulinPump {
    override val insulinPumpId: String = PUMP_DRIVER_ID
    override val insulinPumpDisplayName: UiText
        get() = UiText.DynamicString(controller.deviceName.ifBlank { "Sooil Dana-i" })

    override val insulinTypeId: StateFlow<String?>
        get() = pump.insulinTypeId

    override val insulinConcentration: StateFlow<InsulinConcentration>
        get() = pump.insulinConcentration

    override val hardwareInformation: StateFlow<PumpHardwareInformation?> = pump.hardware.map { hi ->
        hi?.let {
            PumpHardwareInformation(
                DANA_I_MANUFACTURER,
                it.hardwareModel.toString(),
                it.serialNumber
            )
        }
    }.stateIn(scope, SharingStarted.Eagerly, null)

    override val pumpCapabilities: StateFlow<PumpCapabilities> = pump.pumpCapabilities.map {
        it ?: PumpCapabilities(
            minBasalRate = DANA_I_MIN_BASAL_RATE,
            supportsZeroBasal = true,
            minBasalIncrement = DANA_I_MIN_BASAL_INCREMENT,
            minBolusAmount = DANA_I_MIN_BOLUS_AMOUNT,
            minBolusIncrement = DANA_I_MIN_BOLUS_INCREMENT,
            maxBolusSize = DANA_I_MAX_BOLUS_VALUE
        )
    }.stateIn(
        scope, SharingStarted.Eagerly, PumpCapabilities(
            minBasalRate = DANA_I_MIN_BASAL_RATE,
            supportsZeroBasal = true,
            minBasalIncrement = DANA_I_MIN_BASAL_INCREMENT,
            minBolusAmount = DANA_I_MIN_BOLUS_AMOUNT,
            minBolusIncrement = DANA_I_MIN_BOLUS_INCREMENT,
            maxBolusSize = DANA_I_MAX_BOLUS_VALUE
        )
    )

    override val isConnected: StateFlow<Boolean> = pump.isConnected
    override val isBlocked: StateFlow<Boolean> get() = pump.isMaintenanceMode
    override val requiresStaleBondRemoval: StateFlow<Boolean> get() = controller.requiresStaleBondRemoval

    private val danaPreferences: DanaPumpPreferences? = try {
        DanaPumpPreferences(controller.link.preferences)
    } catch (_: Exception) {
        null
    }

    override val pumpStatus: StateFlow<InsulinPumpStatus> = if (danaPreferences != null) {
        combine(
            pump.status,
            danaPreferences.getCatheterLastChangedFlow(),
            danaPreferences.getCatheterWearDurationDaysFlow()
        ) { status, catheterLastChanged, wearDurationDays ->
            val approximateExpiration = catheterLastChanged?.let { ts ->
                Instant.ofEpochMilli(ts.ms)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .plusDays(wearDurationDays.toLong())
            }
            val currentLifecycle = status.lifecycleState as? PumpLifecycleState.Durable
            val updatedLifecycle = (currentLifecycle ?: PumpLifecycleState.Durable()).copy(
                catheterLastChanged = catheterLastChanged,
                approximateCatheterExpiration = approximateExpiration
            )
            (status as? DanaPumpStatusData)?.copy(lifecycleState = updatedLifecycle) ?: status
        }.stateIn(scope, SharingStarted.Eagerly, pump.status.value)
    } else {
        pump.status
    }

    suspend fun saveCatheterLastChanged(timestamp: Timestamp?) {
        danaPreferences?.saveCatheterLastChanged(timestamp)
    }
    override val alerts: StateFlow<PumpAlerts> get() = pump.alerts
    override val basalStatus: StateFlow<BasalStatus> get() = pump.basalStatus
    override val bolusStatus: StateFlow<BolusStatus> get() = pump.bolusStatus
    override val bolusEvents: SharedFlow<BolusEvent> get() = pump.bolusEvents

    override val history: StateFlow<InsulinHistory?> get() = pump.history

    override suspend fun bolus(amount: InsulinAmount, bolusId: String?) {
        // TODO: Read bolus speed from configuration
        val speed = DanaIBolusSpeed.U12_SECONDS
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

    override suspend fun syncHistory(start: Timestamp) {
        pump.syncHistory(start)
    }

    override suspend fun refreshStatus() {
        pump.refreshStatus()
    }

    val isMaintenanceMode: StateFlow<Boolean>
        get() = pump.isMaintenanceMode

    fun setMaintenanceMode(active: Boolean) {
        pump.setMaintenanceMode(active)
    }

    override fun stop() {
        controller.stop()
    }

    companion object {
        const val PUMP_DRIVER_ID = "de.dh.daps.plugin.danai"

        fun create(link: DanaILink, appLogger: DanaILogger, scope: CoroutineScope): DanaIPumpAdapter {
            val pump = DanaIPump()
            val controller = DanaIController(pump, link, appLogger, scope)
            return DanaIPumpAdapter(pump, controller, scope)
        }
    }
}