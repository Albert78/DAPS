package de.dh.daps.plugin.glucose.receiver

import android.app.Application
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
import de.dh.daps.common.model.CgmConnectionDescriptor
import de.dh.daps.common.model.CgmDriver
import de.dh.daps.common.model.GlucoseSource
import de.dh.daps.common.model.PluginManager

/**
 * Driver implementation for external BroadcastReceiver glucose sources (e.g. xDrip+ / Juggluco).
 */
class ReceiverGlucoseDriver(
    private val application: Application,
) : CgmDriver {
    override val driverId: String = DRIVER_ID
    override val displayName: String = "External Broadcast Receiver Driver"
    override val name: String = "Receiver Glucose Driver Plugin"
    override val neededPermissions: Collection<String> = listOf("com.eveningoutpost.dexdrip.permissions.RECEIVE_BG_ESTIMATE")

    override fun initialize(pluginManager: PluginManager) {
        // Driver initialized alongside application
    }

    /**
     * Renders the setup screen for configuring the external BroadcastReceiver glucose source.
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
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "External Receiver Setup",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Receive blood glucose estimates broadcast from external apps like xDrip+ or Juggluco.",
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
                            val plugin = ReceiverGlucosePlugin(
                                application = application,
                                externalSourceType = ExternalSourceType.xDrip5Min,
                            )
                            val descriptor = CgmConnectionDescriptor(
                                driverId = DRIVER_ID,
                                sourceId = "xdrip-5min-receiver",
                                displayName = "xDrip+ Broadcast Receiver (5 Min)",
                                connectionParameters = mapOf("sourceType" to "xDrip5Min"),
                            )
                            onConnected(plugin, descriptor)
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
     * Re-connects to an external BroadcastReceiver glucose source using a stored connection descriptor.
     */
    override suspend fun connect(descriptor: CgmConnectionDescriptor): Result<GlucoseSource> {
        if (descriptor.driverId != DRIVER_ID) {
            return Result.failure(
                IllegalArgumentException("Invalid driver ID for Receiver Glucose Driver: ${descriptor.driverId}"),
            )
        }
        val sourceType = descriptor.connectionParameters["sourceType"]
        val externalSourceType = when (sourceType) {
            "xDrip1Min" -> ExternalSourceType.xDrip1Min
            else -> ExternalSourceType.xDrip5Min
        }
        val plugin = ReceiverGlucosePlugin(
            application = application,
            externalSourceType = externalSourceType,
        )
        return Result.success(plugin)
    }

    companion object {
        const val DRIVER_ID = "de.dh.daps.plugin.glucose.receiver"
    }
}