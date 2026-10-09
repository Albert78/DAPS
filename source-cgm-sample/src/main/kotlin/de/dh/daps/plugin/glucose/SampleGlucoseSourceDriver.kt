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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Lightweight sample implementation of [GlucoseSourceDriver].
 * Demonstrates the basic structure of a glucose source driver plugin without complex logic.
 */
class SampleGlucoseSourceDriver : GlucoseSourceDriver, Plugin {
    override val driverId: String = DRIVER_ID
    override val displayName: UiText = UiText.StringResource(R.string.sample_cgm_driver_display_name)
    override val pluginId: String = DRIVER_ID
    override val neededPermissions: Collection<String> = emptyList()

    private val _lifecycleState = MutableStateFlow<DeviceLifecycleState>(DeviceLifecycleState.Unconfigured)
    override val lifecycleState: StateFlow<DeviceLifecycleState> = _lifecycleState.asStateFlow()

    override fun initialize(context: PluginContext) {
        // Driver initialization sketch: Register background services or sensor listeners here
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
                        // Sketch: In a real driver, perform transmitter scanning & pairing here
                        val source = SampleGlucoseSource()
                        _lifecycleState.value = DeviceLifecycleState.Configured(
                            deviceId = "sample-cgm-sensor-01",
                            displayName = "Sample Glucose Sensor 01",
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
    override suspend fun connect(): Result<GlucoseSource> {
        _lifecycleState.value = DeviceLifecycleState.Configured(
            deviceId = "sample-cgm-sensor-01",
            displayName = "Sample Glucose Sensor 01",
        )
        return Result.success(SampleGlucoseSource())
    }

    companion object {
        const val DRIVER_ID = "de.dh.daps.plugin.cgm.sample"
    }
}