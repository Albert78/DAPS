package de.dh.daps.plugin.simbody

import android.app.Application
import de.dh.daps.common.model.GlucoseSource
import de.dh.daps.common.model.InsulinPump
import de.dh.daps.common.model.Plugin
import de.dh.daps.common.model.PluginManager
import de.dh.daps.common.model.data.BgReading
import de.dh.daps.core.system.SystemWakeService
import de.dh.daps.plugin.simbody.repository.db.SimBodyDatabase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * A plugin which provides a glucose source and a pump instance which are connected to a
 * "virtual human body", simulating the influence of meals and insulin.
 */
class SimBodyPlugin(
    val application: Application,
    var wakeService: SystemWakeService? = null
) : Plugin {
    private val database = SimBodyDatabase.getInstance(application)
    val bodyModel = BodyModel(DEFAULT_SIM_BODY_PROFILE, database.impactDao())
    val pumpDevice = SimBodyPumpDevice(bodyModel, DEFAULT_SIM_INSULIN_PROFILE, database.pumpDao())
    val pumpDriver = SimBodyInsulinPumpDriver(this)
    val cgmDriver = SimBodyCgmDriver(this)

    private val _glucoseReadings = MutableSharedFlow<BgReading>(
        replay = 0,
        extraBufferCapacity = 16
    )

    private var heartbeat: SimBodyHeartbeat? = null

    override val name: String = "Sim Body Plugin"
    override val neededPermissions: Collection<String> = emptyList()

    override fun initialize(pluginManager: PluginManager) {
        bodyModel.loadState()
        pumpDevice.loadState()
        wakeService?.let { attachWakeService(it) }
    }

    fun attachWakeService(wakeService: SystemWakeService) {
        this.wakeService = wakeService
        if (heartbeat == null) {
            heartbeat = SimBodyHeartbeat(
                wakeService = wakeService,
                bodyModel = bodyModel,
                pumpDevice = pumpDevice,
                onBgReading = { _glucoseReadings.tryEmit(it) }
            ).apply { start() }
        }
    }

    fun getGlucoseSource(): GlucoseSource = SimBodyCgmSource(_glucoseReadings.asSharedFlow())
    fun getInsulinPump(): InsulinPump = SimBodyInsulinPump(pumpDevice)
}