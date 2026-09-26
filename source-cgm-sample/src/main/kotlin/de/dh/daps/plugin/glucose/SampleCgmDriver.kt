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
import de.dh.daps.common.model.CgmConnectionDescriptor
import de.dh.daps.common.model.CgmDriver
import de.dh.daps.common.model.GlucoseSource
import de.dh.daps.common.model.PluginManager

/**
 * Lightweight sample implementation of [CgmDriver].
 * Demonstrates the basic structure of a CGM driver plugin without complex logic.
 */
class SampleCgmDriver : CgmDriver {
    override val driverId: String = DRIVER_ID
    override val displayName: String = "Sample CGM Driver"
    override val name: String = "Sample CGM Driver Plugin"
    override val neededPermissions: Collection<String> = emptyList()

    override fun initialize(pluginManager: PluginManager) {
        // Driver initialization sketch: Register background services or sensor listeners here
    }

    /**
     * Renders a basic setup screen allowing the user to confirm pairing with the sample CGM source.
     */
    @Composable
    override fun SetupScreen(
        onConnected: (GlucoseSource, CgmConnectionDescriptor) -> Unit,
        onCancel: () -> Unit,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Sample CGM Setup",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "Confirm connection to the simulated sample CGM sensor.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        // Sketch: In a real driver, perform transmitter scanning & pairing here
                        val source = SampleCgmPlugin()
                        val descriptor = CgmConnectionDescriptor(
                            driverId = DRIVER_ID,
                            sourceId = "sample-cgm-sensor-01",
                            displayName = "Sample CGM Sensor 01",
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
     * Re-connects to a sample CGM source using a stored connection descriptor.
     */
    override suspend fun connect(descriptor: CgmConnectionDescriptor): Result<GlucoseSource> {
        // Sketch: Validate descriptor parameters and establish connection with sensor
        if (descriptor.driverId != DRIVER_ID) {
            return Result.failure(IllegalArgumentException("Invalid driver ID for Sample CGM Driver: ${descriptor.driverId}"))
        }
        return Result.success(SampleCgmPlugin())
    }

    companion object {
        const val DRIVER_ID = "de.dh.daps.plugin.cgm.sample"
    }
}