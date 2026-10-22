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
import de.dh.daps.common.model.InsulinPump
import de.dh.daps.common.model.InsulinPumpDriver
import de.dh.daps.common.model.Plugin
import de.dh.daps.common.model.PluginContext
import de.dh.daps.common.ui.UiText
import de.dh.daps.plugin.simbody.ui.screens.SimBodyPumpSetupScreen
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
        _activePump.value = simBodyPlugin.getInsulinPump()
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

    override fun getSettingsScreen(pump: InsulinPump, onClose: () -> Unit): (@Composable () -> Unit) = {
        SimBodyPumpSettingsContent(onClose = onClose)
    }

    companion object {
        const val DRIVER_ID = "de.dh.daps.plugin.simbody"
        const val DEFAULT_DEVICE_ID = "simbody-virtual-pump-01"
        private const val KEY_DEVICE_ID = "device_id"
    }
}

@Composable
private fun SimBodyPumpSettingsContent(
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
            text = "SimBody Pumpen-Einstellungen",
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
                    text = "Verzögerung der Bolusabgabe im Simulator deaktivieren",
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