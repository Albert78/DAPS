package de.dh.daps.plugin.glucose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.dh.daps.common.model.GlucoseSource
import de.dh.daps.common.model.GlucoseSourceConnectionDescriptor
import de.dh.daps.common.model.GlucoseSourceDriver
import de.dh.daps.common.model.Plugin
import de.dh.daps.common.model.PluginContext
import de.dh.daps.common.ui.UiText

/**
 * Lightweight sample implementation of [GlucoseSourceDriver].
 * Demonstrates the basic structure of a glucose source driver plugin without complex logic.
 */
class SampleGlucoseSourceDriver : GlucoseSourceDriver, Plugin {
    override val driverId: String = DRIVER_ID
    override val displayName: UiText = UiText.StringResource(R.string.sample_cgm_driver_display_name)
    override val pluginName: UiText = UiText.StringResource(R.string.sample_cgm_driver_name)
    override val neededPermissions: Collection<String> = emptyList()

    override fun initialize(context: PluginContext) {
        // Driver initialization sketch: Register background services or sensor listeners here
    }

    /**
     * Renders a basic setup screen allowing the user to confirm pairing with the sample glucose source.
     */
    @Composable
    override fun SetupScreen(
        onConnected: (GlucoseSource, GlucoseSourceConnectionDescriptor) -> Unit,
        onCancel: () -> Unit,
    ) {
        Card(
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
                        val descriptor = GlucoseSourceConnectionDescriptor(
                            driverId = DRIVER_ID,
                            sourceId = "sample-cgm-sensor-01",
                            displayName = "Sample Glucose Sensor 01",
                        )
                        onConnected(source, descriptor)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Confirm Connection")
                }
            }
        }
    }

    /**
     * Re-connects to a sample glucose source using a stored connection descriptor.
     */
    override suspend fun connect(descriptor: GlucoseSourceConnectionDescriptor): Result<GlucoseSource> {
        // Sketch: Validate descriptor parameters and establish connection with sensor
        if (descriptor.driverId != DRIVER_ID) {
            return Result.failure(IllegalArgumentException("Invalid driver ID for Sample Glucose Source Driver: ${descriptor.driverId}"))
        }
        return Result.success(SampleGlucoseSource())
    }

    companion object {
        const val DRIVER_ID = "de.dh.daps.plugin.cgm.sample"
    }
}