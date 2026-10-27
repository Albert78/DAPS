package de.dh.daps.plugin.simbody

import androidx.compose.runtime.Composable
import de.dh.daps.common.device.driver.InsulinPumpDriver
import de.dh.daps.common.device.state.DeviceLifecycleState
import de.dh.daps.common.domain.pump.InsulinPump
import de.dh.daps.common.domain.model.therapy.InsulinConcentration
import de.dh.daps.common.infrastructure.Plugin
import de.dh.daps.common.infrastructure.PluginContext
import de.dh.daps.common.ui.UiText
import de.dh.daps.plugin.simbody.ui.screens.SimBodyConfigurationScreen
import de.dh.daps.plugin.simbody.ui.screens.SimBodyPumpSetupScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Driver implementation for the SimBody virtual insulin pump simulation.
 * Manages virtual pump connection setup and reconnection for the simulated human body.
 */
class SimBodyInsulinPumpDriver(
    private val simBodyPlugin: SimBodyPlugin,
) : InsulinPumpDriver, Plugin {
    override val driverId: String = DRIVER_ID
    override val displayName: UiText = UiText.StringResource(R.string.sim_body_pump_driver_display_name)
    override val pluginId: String = DRIVER_ID
    override val neededPermissions: Collection<String> = emptyList()

    private var pluginContext: PluginContext? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _lifecycleState = MutableStateFlow<DeviceLifecycleState>(DeviceLifecycleState.NoDevice)
    override val lifecycleState: StateFlow<DeviceLifecycleState> = _lifecycleState.asStateFlow()

    private val _activePump = MutableStateFlow<InsulinPump?>(null)
    override val activePump: StateFlow<InsulinPump?> = _activePump.asStateFlow()

    override fun setup(context: PluginContext) {
        this.pluginContext = context
    }

    fun bindDevice(deviceId: String = DEFAULT_DEVICE_ID) {
        pluginContext?.preferences?.let { prefs ->
            scope.launch {
                prefs.putString(KEY_DEVICE_ID, deviceId)
            }
        }
        _lifecycleState.value = DeviceLifecycleState.DeviceBound(
            deviceId = deviceId,
            displayName = UiText.StringResource(R.string.sim_body_pump_device_display_name),
        )
        val pump = simBodyPlugin.getInsulinPump()
        restorePumpPreferences(pump)
        _activePump.value = pump
    }

    private fun restorePumpPreferences(pump: InsulinPump) {
        val simPump = pump as? SimBodyInsulinPump ?: return
        pluginContext?.preferences?.let { prefs ->
            scope.launch {
                val typeId = prefs.getString(KEY_INSULIN_TYPE_ID).first()
                if (!typeId.isNullOrEmpty()) {
                    simPump.setInsulinTypeId(typeId)
                }
                val uValue = prefs.getInt(KEY_INSULIN_CONCENTRATION_UVALUE, 100).first()
                if (uValue > 0) {
                    simPump.setInsulinConcentration(InsulinConcentration.fromUValue(uValue))
                }
            }
        }
    }

    fun unbindDevice() {
        scope.launch {
            pluginContext?.preferences?.let { prefs ->
                prefs.remove(KEY_DEVICE_ID)
            }
            _activePump.value = null
            _lifecycleState.value = DeviceLifecycleState.NoDevice
        }
    }

    override suspend fun initState() {
        val pc = pluginContext ?: return
        val deviceId = pc.preferences.getString(KEY_DEVICE_ID).first()
        if (!deviceId.isNullOrEmpty()) {
            bindDevice(deviceId)
        } else {
            _lifecycleState.value = DeviceLifecycleState.NoDevice
            _activePump.value = null
        }
    }

    /**
     * Renders the setup screen for connecting to the virtual human body simulation pump.
     */
    @Composable
    override fun SetupScreen(
        onDriverReset: () -> Unit,
    ) {
        SimBodyPumpSetupScreen(
            driver = this,
            onDriverReset = onDriverReset,
        )
    }

    override fun getSettingsScreen(
        pump: InsulinPump,
        onClose: () -> Unit,
    ): (@Composable () -> Unit) = {
        SimBodyConfigurationScreen(
            pump = pump,
            preferences = pluginContext?.preferences,
            onClose = onClose,
        )
    }

    companion object {
        const val DRIVER_ID = "de.dh.daps.plugin.simbody"
        const val DEFAULT_DEVICE_ID = "simbody-virtual-pump-01"
        private const val KEY_DEVICE_ID = "device_id"
        const val KEY_INSULIN_TYPE_ID = "insulin_type_id"
        const val KEY_INSULIN_CONCENTRATION_UVALUE = "insulin_concentration_uvalue"
    }
}