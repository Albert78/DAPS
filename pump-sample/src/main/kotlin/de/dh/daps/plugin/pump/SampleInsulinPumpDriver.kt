package de.dh.daps.plugin.pump

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
 * Lightweight sample implementation of [InsulinPumpDriver].
 * Demonstrates the basic structure of a pump driver plugin without complex logic.
 */
class SampleInsulinPumpDriver : InsulinPumpDriver, Plugin {
    override val driverId: String = DRIVER_ID
    override val displayName: UiText = UiText.StringResource(R.string.sample_pump_driver_display_name)
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

    override suspend fun initState() {
        val pc = pluginContext ?: return
        val deviceId = pc.preferences.getString(KEY_DEVICE_ID).first()
        val deviceName = pc.preferences.getString(KEY_DEVICE_NAME).first() ?: "Sample Pump 01"
        if (!deviceId.isNullOrEmpty()) {
            _lifecycleState.value = DeviceLifecycleState.DeviceBound(
                deviceId = deviceId,
                displayName = UiText.DynamicString(deviceName)
            )
            _activePump.value = SampleInsulinPump()
        } else {
            _lifecycleState.value = DeviceLifecycleState.NoDevice
            _activePump.value = null
        }
    }

    /**
     * Renders a basic setup screen allowing the user to confirm pairing with the sample pump.
     */
    @Composable
    override fun SetupScreen(
        onNavigateUp: () -> Unit,
    ) {
        FramedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Sample Pump Setup",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "Confirm setup for the simulated sample pump.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        val deviceId = "sample-pump-device-01"
                        val deviceName = "Sample Pump 01"
                        pluginContext?.preferences?.let { prefs ->
                            scope.launch {
                                prefs.putString(KEY_DEVICE_ID, deviceId)
                                prefs.putString(KEY_DEVICE_NAME, deviceName)
                            }
                        }
                        _lifecycleState.value = DeviceLifecycleState.DeviceBound(
                            deviceId = deviceId,
                            displayName = UiText.DynamicString(deviceName),
                        )
                        _activePump.value = SampleInsulinPump()
                        onNavigateUp()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Confirm Setup")
                }
            }
        }
    }

    companion object {
        const val DRIVER_ID = "de.dh.daps.plugin.pump.sample"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_DEVICE_NAME = "device_name"
    }
}