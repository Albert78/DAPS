package de.dh.daps.plugin.glucose

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
import de.dh.daps.common.model.GlucoseSource
import de.dh.daps.common.model.GlucoseSourceDriver
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
 * Lightweight sample implementation of [GlucoseSourceDriver].
 * Demonstrates the basic structure of a glucose source driver plugin without complex logic.
 */
class SampleGlucoseSourceDriver : GlucoseSourceDriver, Plugin {
    override val driverId: String = DRIVER_ID
    override val displayName: UiText = UiText.StringResource(R.string.sample_cgm_driver_display_name)
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
            val deviceName = context.preferences.getString(KEY_DEVICE_NAME).first() ?: "Sample Glucose Sensor 01"
            if (!deviceId.isNullOrEmpty()) {
                _lifecycleState.value = DeviceLifecycleState.DeviceBound(
                    deviceId = deviceId,
                    displayName = UiText.DynamicString(deviceName)
                )
            } else {
                _lifecycleState.value = DeviceLifecycleState.NoDevice
            }
        }
    }

    /**
     * Renders a basic setup screen allowing the user to confirm pairing with the sample glucose source.
     */
    @Composable
    override fun SetupScreen(
        onConnected: (GlucoseSource) -> Unit,
        onCancel: () -> Unit,
    ) {
        FramedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Sample Glucose Source Setup",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "Confirm connection to the simulated sample glucose sensor.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        val source = SampleGlucoseSource()
                        val deviceId = "sample-cgm-sensor-01"
                        val deviceName = "Sample Glucose Sensor 01"
                        pluginContext?.preferences?.let { prefs ->
                            scope.launch {
                                prefs.putString(KEY_DEVICE_ID, deviceId)
                                prefs.putString(KEY_DEVICE_NAME, deviceName)
                            }
                        }
                        _lifecycleState.value = DeviceLifecycleState.DeviceBound(
                            deviceId = deviceId,
                            displayName = UiText.DynamicString(deviceName)
                        )
                        onConnected(source)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Confirm Connection")
                }
            }
        }
    }

    /**
     * Re-connects to a sample glucose source using stored driver settings.
     */
    override suspend fun attach(): Result<GlucoseSource> {
        val pc = pluginContext
            ?: return Result.failure(IllegalStateException("PluginContext not initialized"))
        val prefs = pc.preferences
        val deviceId = prefs.getString(KEY_DEVICE_ID).first()
        val deviceName = prefs.getString(KEY_DEVICE_NAME).first() ?: "Sample Glucose Sensor 01"

        if (deviceId.isNullOrEmpty()) {
            _lifecycleState.value = DeviceLifecycleState.NoDevice
            return Result.failure(IllegalStateException("No saved device configuration found for sample glucose source"))
        }

        _lifecycleState.value = DeviceLifecycleState.DeviceBound(
            deviceId = deviceId,
            displayName = UiText.DynamicString(deviceName)
        )
        return Result.success(SampleGlucoseSource())
    }

    companion object {
        const val DRIVER_ID = "de.dh.daps.plugin.cgm.sample"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_DEVICE_NAME = "device_name"
    }
}