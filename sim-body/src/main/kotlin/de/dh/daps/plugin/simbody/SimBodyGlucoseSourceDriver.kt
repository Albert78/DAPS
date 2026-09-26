package de.dh.daps.plugin.simbody

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.dh.daps.common.model.GlucoseSource
import de.dh.daps.common.model.GlucoseSourceConnectionDescriptor
import de.dh.daps.common.model.GlucoseSourceDriver
import de.dh.daps.common.model.Plugin

/**
 * Driver implementation for the SimBody virtual glucose source simulation.
 * Manages virtual glucose sensor connection setup and reconnection for the simulated human body.
 */
class SimBodyGlucoseSourceDriver(
    private val simBodyPlugin: SimBodyPlugin,
) : GlucoseSourceDriver, Plugin {
    override val driverId: String = DRIVER_ID
    override val displayName: String = "SimBody Virtual Glucose Source Driver"
    override val name: String = "SimBody Glucose Source Driver Plugin"
    override val neededPermissions: Collection<String> = emptyList()

    /**
     * Renders the setup screen for connecting to the virtual human body simulation glucose source.
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
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "SimBody Virtual Glucose Source Setup",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Connect to the simulated glucose sensor attached to the virtual human body model.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val source = simBodyPlugin.getGlucoseSource()
                            val descriptor = GlucoseSourceConnectionDescriptor(
                                driverId = DRIVER_ID,
                                sourceId = "simbody-virtual-cgm-01",
                                displayName = "SimBody Virtual Glucose Sensor",
                                connectionParameters = mapOf("simulated" to "true"),
                            )
                            onConnected(source, descriptor)
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Connect")
                    }
                }
            }
        }
    }

    /**
     * Re-connects to the virtual human body simulation glucose source using a stored connection descriptor.
     */
    override suspend fun connect(descriptor: GlucoseSourceConnectionDescriptor): Result<GlucoseSource> {
        if (descriptor.driverId != DRIVER_ID) {
            return Result.failure(
                IllegalArgumentException("Invalid driver ID for SimBody Glucose Source driver: ${descriptor.driverId}"),
            )
        }
        return Result.success(simBodyPlugin.getGlucoseSource())
    }

    companion object {
        const val DRIVER_ID = "de.dh.daps.plugin.simbody.cgm"
    }
}