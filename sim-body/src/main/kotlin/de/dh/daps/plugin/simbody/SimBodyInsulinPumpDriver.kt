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

    override fun initialize(context: PluginContext) {
        this.pluginContext = context
        scope.launch {
            val deviceId = context.preferences.getString(KEY_DEVICE_ID).first()
            if (!deviceId.isNullOrEmpty()) {
                _lifecycleState.value = DeviceLifecycleState.DeviceBound(
                    deviceId = deviceId,
                    displayName = UiText.StringResource(R.string.sim_body_pump_device_display_name)
                )
            } else {
                _lifecycleState.value = DeviceLifecycleState.NoDevice
            }
        }
    }

    /**
     * Renders the setup screen for connecting to the virtual human body simulation pump.
     */
    @Composable
    override fun SetupScreen(
        onConnected: (InsulinPump) -> Unit,
        onCancel: () -> Unit,
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
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.btn_cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val pump = simBodyPlugin.getInsulinPump()
                            val deviceId = "simbody-virtual-pump-01"
                            pluginContext?.preferences?.let { prefs ->
                                scope.launch {
                                    prefs.putString(KEY_DEVICE_ID, deviceId)
                                }
                            }
                            _lifecycleState.value = DeviceLifecycleState.DeviceBound(
                                deviceId = deviceId,
                                displayName = UiText.StringResource(R.string.sim_body_pump_device_display_name)
                            )
                            onConnected(pump)
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.btn_connect))
                    }
                }
            }
        }
    }

    /**
     * Re-connects to the virtual human body simulation pump using stored driver settings.
     */
    override suspend fun attach(): Result<InsulinPump> {
        val pc = pluginContext
            ?: return Result.failure(IllegalStateException("PluginContext not initialized"))
        val deviceId = pc.preferences.getString(KEY_DEVICE_ID).first()

        if (deviceId.isNullOrEmpty()) {
            _lifecycleState.value = DeviceLifecycleState.NoDevice
            return Result.failure(IllegalStateException("No saved device configuration found for SimBody Pump"))
        }

        _lifecycleState.value = DeviceLifecycleState.DeviceBound(
            deviceId = deviceId,
            displayName = UiText.StringResource(R.string.sim_body_pump_device_display_name)
        )
        return Result.success(simBodyPlugin.getInsulinPump())
    }

    companion object {
        const val DRIVER_ID = "de.dh.daps.plugin.simbody"
        private const val KEY_DEVICE_ID = "device_id"
    }
}