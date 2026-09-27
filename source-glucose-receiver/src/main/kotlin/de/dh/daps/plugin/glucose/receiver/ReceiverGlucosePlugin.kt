package de.dh.daps.plugin.glucose.receiver

import android.app.Application
import android.content.Context
import android.content.IntentFilter
import android.util.Log
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
import de.dh.daps.common.model.GlucoseSourceStatus
import de.dh.daps.common.model.Plugin
import de.dh.daps.common.model.data.BgReading
import de.dh.daps.common.model.data.BgReadingsInterval
import de.dh.daps.common.model.data.Minutes
import de.dh.daps.common.model.data.Timestamp
import de.dh.daps.common.ui.UiText
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach

/**
 * Glucose plugin which receives glucose values from other Android apps via a BroadcastReceiver.
 * Acts both as a [GlucoseSourceDriver] for connection setup and as a [GlucoseSource] for data streaming.
 */
class ReceiverGlucosePlugin(
    val application: Application,
    var externalSourceType: ExternalSourceType = ExternalSourceType.xDrip5Min,
) : GlucoseSourceDriver, GlucoseSource, Plugin {

    override val driverId: String = DRIVER_ID
    override val glucoseSourceId: String = SOURCE_ID
    override val driverDisplayName: UiText = UiText.StringResource(R.string.glucose_broadcast_receiver_display_name)
    override val sourceDisplayName: UiText = UiText.StringResource(R.string.glucose_broadcast_receiver_display_name)
    override val pluginId: String = DRIVER_ID
    override val neededPermissions: Collection<String> = listOf("com.eveningoutpost.dexdrip.permissions.RECEIVE_BG_ESTIMATE")

    override val readingsInterval: BgReadingsInterval
        get() = externalSourceType.readingsInterval

    override val readingsTimeDelay: Minutes
        get() = externalSourceType.readingsTimeDelay

    override val status: StateFlow<GlucoseSourceStatus> = MutableStateFlow(GlucoseSourceStatus.Ok)
    override val expirationDate: StateFlow<Timestamp?> = MutableStateFlow(null)

    private val _lastConnection = MutableStateFlow<Timestamp?>(null)
    override val lastConnection: StateFlow<Timestamp?> = _lastConnection.asStateFlow()

    override fun getSensorTypeName(): String = "External Receiver"

    private val dataReceiver = DataReceiver()

    private val _readings = MutableSharedFlow<BgReading>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    override fun start() {
        instance = this
        registerReceiver()
    }

    override fun stop() {
        instance = null
        unregisterReceiver()
    }

    private fun registerReceiver() {
        try {
            val filter = IntentFilter("com.eveningoutpost.dexdrip.BgEstimate")
            application.registerReceiver(dataReceiver, filter, Context.RECEIVER_EXPORTED)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to dynamically register DataReceiver: ${e.message}")
        }
    }

    private fun unregisterReceiver() {
        try {
            application.unregisterReceiver(dataReceiver)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to unregister DataReceiver: ${e.message}")
        }
    }

    /**
     * Values received by our BroadcastReceiver will go here.
     */
    fun injectReading(value: BgReading): Boolean {
        Log.d(TAG, "New glucose reading: $value")
        _lastConnection.value = Timestamp.now()
        return _readings.tryEmit(value)
    }

    override fun getValues(): Flow<BgReading> {
        return _readings.asSharedFlow().onEach {
            _lastConnection.value = Timestamp.now()
        }
    }

    /**
     * Renders the setup screen for configuring the external BroadcastReceiver glucose source.
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
                            externalSourceType = ExternalSourceType.xDrip5Min
                            val descriptor = GlucoseSourceConnectionDescriptor(
                                driverId = DRIVER_ID,
                                sourceId = "xdrip-5min-receiver",
                                displayName = "xDrip+ Broadcast Receiver (5 Min)",
                                connectionParameters = mapOf("sourceType" to "xDrip5Min"),
                            )
                            onConnected(this@ReceiverGlucosePlugin, descriptor)
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
    override suspend fun connect(descriptor: GlucoseSourceConnectionDescriptor): Result<GlucoseSource> {
        if (descriptor.driverId != DRIVER_ID) {
            return Result.failure(
                IllegalArgumentException("Invalid driver ID for Receiver Glucose Driver: ${descriptor.driverId}"),
            )
        }
        val sourceType = descriptor.connectionParameters["sourceType"]
        externalSourceType = when (sourceType) {
            "xDrip1Min" -> ExternalSourceType.xDrip1Min
            else -> ExternalSourceType.xDrip5Min
        }
        return Result.success(this)
    }

    companion object {
        const val DRIVER_ID = "de.dh.daps.plugin.glucose.receiver"
        const val SOURCE_ID = "receiver-glucose-source"
        val TAG = ReceiverGlucosePlugin::class.simpleName
        var instance: ReceiverGlucosePlugin? = null
    }
}