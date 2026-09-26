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
import de.dh.daps.common.model.InsulinPump
import de.dh.daps.common.model.InsulinPumpDriver
import de.dh.daps.common.model.PluginManager
import de.dh.daps.common.model.PumpConnectionDescriptor

/**
 * Driver implementation for the SimBody virtual insulin pump simulation.
 * Manages virtual pump connection setup and reconnection for the simulated human body.
 */
class SimBodyInsulinPumpDriver(
    private val simBodyPlugin: SimBodyPlugin,
) : InsulinPumpDriver {
    override val driverId: String = DRIVER_ID
    override val displayName: String = "SimBody Virtual Pump Driver"
    override val name: String = "SimBody Pump Driver Plugin"
    override val neededPermissions: Collection<String> = emptyList()

    override fun initialize(pluginManager: PluginManager) {
        // Driver initialized alongside SimBodyPlugin
    }

    /**
     * Renders the setup screen for connecting to the virtual human body simulation pump.
     */
    @Composable
    override fun SetupScreen(
        onConnected: (InsulinPump, PumpConnectionDescriptor) -> Unit,
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
                    text = "SimBody Virtual Pump Setup",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Connect to the simulated insulin pump attached to the virtual human body model.",
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
                            val pump = simBodyPlugin.getInsulinPump()
                            val descriptor = PumpConnectionDescriptor(
                                driverId = DRIVER_ID,
                                deviceId = "simbody-virtual-pump-01",
                                displayName = "SimBody Virtual Insulin Pump",
                                connectionParameters = mapOf("simulated" to "true"),
                            )
                            onConnected(pump, descriptor)
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
     * Re-connects to the virtual human body simulation pump using a stored connection descriptor.
     */
    override suspend fun connect(descriptor: PumpConnectionDescriptor): Result<InsulinPump> {
        if (descriptor.driverId != DRIVER_ID) {
            return Result.failure(
                IllegalArgumentException("Invalid driver ID for SimBody driver: ${descriptor.driverId}")
            )
        }
        return Result.success(simBodyPlugin.getInsulinPump())
    }

    companion object {
        const val DRIVER_ID = "de.dh.daps.plugin.simbody"
    }
}