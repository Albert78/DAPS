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
import de.dh.daps.common.ui.UiText
import de.dh.daps.ui.common.composables.FramedCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Lightweight sample implementation of [InsulinPumpDriver].
 * Demonstrates the basic structure of a pump driver plugin without complex logic.
 */
class SampleInsulinPumpDriver : InsulinPumpDriver, Plugin {
    override val driverId: String = DRIVER_ID
    override val displayName: UiText = UiText.StringResource(R.string.sample_pump_driver_display_name)
    override val pluginId: String = DRIVER_ID
    override val neededPermissions: Collection<String> = emptyList()

    private val _lifecycleState = MutableStateFlow<DeviceLifecycleState>(DeviceLifecycleState.Unconfigured)
    override val lifecycleState: StateFlow<DeviceLifecycleState> = _lifecycleState.asStateFlow()

    /**
     * Renders a basic setup screen allowing the user to confirm pairing with the sample pump.
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
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Sample Pump Setup",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "Confirm connection to the simulated sample pump.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        // Sketch: In a real driver, perform scanning, pairing & key exchange here
                        val pump = SampleInsulinPump()
                        _lifecycleState.value = DeviceLifecycleState.Configured(
                            deviceId = "sample-pump-device-01",
                            displayName = UiText.DynamicString("Sample Pump 01"),
                        )
                        onConnected(pump)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Confirm Connection")
                }
            }
        }
    }

    /**
     * Re-connects to a sample pump using stored driver settings.
     */
    override suspend fun connect(): Result<InsulinPump> {
        _lifecycleState.value = DeviceLifecycleState.Configured(
            deviceId = "sample-pump-device-01",
            displayName = UiText.DynamicString("Sample Pump 01"),
        )
        return Result.success(SampleInsulinPump())
    }

    companion object {
        const val DRIVER_ID = "de.dh.daps.plugin.pump.sample"
    }
}