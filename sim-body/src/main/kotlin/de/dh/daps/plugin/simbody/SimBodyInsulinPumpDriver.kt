package de.dh.daps.plugin.simbody

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.dh.daps.common.model.DeviceLifecycleState
import de.dh.daps.common.model.InsulinPump
import de.dh.daps.common.model.InsulinPumpDriver
import de.dh.daps.common.model.Plugin
import de.dh.daps.common.model.PluginContext
import de.dh.daps.common.ui.UiText
import de.dh.daps.ui.common.composables.FramedCard
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

    override fun initialize(context: PluginContext) {
        this.pluginContext = context
    }

    private fun bindDevice(deviceId: String) {
        _lifecycleState.value = DeviceLifecycleState.DeviceBound(
            deviceId = deviceId,
            displayName = UiText.StringResource(R.string.sim_body_pump_device_display_name),
        )
        _activePump.value = simBodyPlugin.getInsulinPump()
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
        onNavigateUp: () -> Unit,
    ) {
        FramedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.sim_body_pump_setup_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.sim_body_pump_setup_description),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = onNavigateUp,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.btn_cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val deviceId = "simbody-virtual-pump-01"
                            pluginContext?.preferences?.let { prefs ->
                                scope.launch {
                                    prefs.putString(KEY_DEVICE_ID, deviceId)
                                }
                            }
                            bindDevice(deviceId)
                            onNavigateUp()
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.btn_connect))
                    }
                }
            }
        }
    }

    companion object {
        const val DRIVER_ID = "de.dh.daps.plugin.simbody"
        private const val KEY_DEVICE_ID = "device_id"
    }
}