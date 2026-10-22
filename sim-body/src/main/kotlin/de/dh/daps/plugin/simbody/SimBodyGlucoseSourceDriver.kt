package de.dh.daps.plugin.simbody

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.dh.daps.common.model.DeviceLifecycleState
import de.dh.daps.common.model.GlucoseSource
import de.dh.daps.common.model.GlucoseSourceDriver
import de.dh.daps.common.model.Plugin
import de.dh.daps.common.model.PluginContext
import de.dh.daps.common.ui.UiText
import de.dh.daps.plugin.simbody.ui.screens.SimBodyCgmSetupScreen
import de.dh.daps.ui.common.composables.PrimaryButton
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
        onDriverReset: () -> Unit,
    ) {
        SimBodyCgmSetupScreen(
            driver = this,
            onDriverReset = onDriverReset,
        )
    }

    override fun getSettingsScreen(glucoseSource: GlucoseSource, onClose: () -> Unit): (@Composable () -> Unit) = {
        SimBodyGlucoseSettingsContent(onClose = onClose)
    }

    companion object {
        const val DRIVER_ID = "de.dh.daps.plugin.simbody.cgm"
        const val DEFAULT_DEVICE_ID = "simbody-virtual-cgm-01"
        private const val KEY_DEVICE_ID = "device_id"
    }
}

@Composable
private fun SimBodyGlucoseSettingsContent(
    onClose: () -> Unit
) {
    var fastSimulation by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "SimBody CGM-Einstellungen",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        HorizontalDivider()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Schnelle Simulation",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Glukosewerte ohne Verzögerung im Simulator bereitstellen",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = fastSimulation,
                onCheckedChange = { fastSimulation = it }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            PrimaryButton(onClick = onClose) {
                Text(stringResource(de.dh.daps.common.R.string.action_close))
            }
        }
    }
}