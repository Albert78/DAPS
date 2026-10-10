package de.dh.daps.plugin.simbody

import androidx.compose.runtime.Composable
import de.dh.daps.common.model.DeviceLifecycleState
import de.dh.daps.common.model.GlucoseSource
import de.dh.daps.common.model.GlucoseSourceDriver
import de.dh.daps.common.model.Plugin
import de.dh.daps.common.model.PluginContext
import de.dh.daps.common.ui.UiText
import de.dh.daps.plugin.simbody.ui.screens.SimBodyCgmSetupScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Driver implementation for the SimBody virtual CGM glucose source.
 * Connects the app to the simulated metabolic engine's blood glucose output.
 */
class SimBodyGlucoseSourceDriver(
    private val simBodyPlugin: SimBodyPlugin,
) : GlucoseSourceDriver, Plugin {
    override val driverId: String = DRIVER_ID
    override val displayName: UiText = UiText.StringResource(R.string.sim_body_cgm_driver_display_name)
    override val pluginId: String = DRIVER_ID
    override val neededPermissions: Collection<String> = emptyList()

    private var pluginContext: PluginContext? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _lifecycleState = MutableStateFlow<DeviceLifecycleState>(DeviceLifecycleState.NoDevice)
    override val lifecycleState: StateFlow<DeviceLifecycleState> = _lifecycleState.asStateFlow()

    private val _activeGlucoseSource = MutableStateFlow<GlucoseSource?>(null)
    override val activeGlucoseSource: StateFlow<GlucoseSource?> = _activeGlucoseSource.asStateFlow()

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
            displayName = UiText.StringResource(R.string.sim_body_cgm_sensor_display_name),
        )
        _activeGlucoseSource.value = simBodyPlugin.getGlucoseSource()
    }

    fun unbindDevice() {
        scope.launch {
            pluginContext?.preferences?.let { prefs ->
                prefs.remove(KEY_DEVICE_ID)
            }
            _activeGlucoseSource.value = null
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
            _activeGlucoseSource.value = null
        }
    }

    /**
     * Renders the setup screen for connecting to the virtual human body simulation CGM.
     */
    @Composable
    override fun SetupScreen(
        onNavigateUp: () -> Unit,
    ) {
        SimBodyCgmSetupScreen(
            driver = this,
            onNavigateUp = onNavigateUp,
        )
    }

    companion object {
        const val DRIVER_ID = "de.dh.daps.plugin.simbody.cgm"
        const val DEFAULT_DEVICE_ID = "simbody-virtual-cgm-01"
        private const val KEY_DEVICE_ID = "device_id"
    }
}