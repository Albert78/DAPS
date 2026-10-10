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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.dh.daps.common.model.DeviceLifecycleState
import de.dh.daps.common.model.Expiration
import de.dh.daps.common.model.GlucoseSource
import de.dh.daps.common.model.GlucoseSourceDriver
import de.dh.daps.common.model.GlucoseSourceStatus
import de.dh.daps.common.model.Plugin
import de.dh.daps.common.model.PluginContext
import de.dh.daps.common.model.PluginPreferences
import de.dh.daps.common.model.SourceHardwareInformation
import de.dh.daps.common.model.data.BgReading
import de.dh.daps.common.model.data.BgReadingsInterval
import de.dh.daps.common.model.data.Minutes
import de.dh.daps.common.model.data.Timestamp
import de.dh.daps.common.ui.UiText
import de.dh.daps.ui.common.composables.FramedCard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

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
    override val displayName: UiText = UiText.StringResource(R.string.glucose_broadcast_receiver_display_name)
    override val sourceDisplayName: UiText = UiText.StringResource(R.string.glucose_broadcast_receiver_display_name)
    override val pluginId: String = DRIVER_ID
    override val neededPermissions: Collection<String> = emptyList()

    private var pluginContext: PluginContext? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _lifecycleState = MutableStateFlow<DeviceLifecycleState>(DeviceLifecycleState.NoDevice)
    override val lifecycleState: StateFlow<DeviceLifecycleState> = _lifecycleState.asStateFlow()

    override fun initialize(context: PluginContext) {
        this.pluginContext = context
    }

    override suspend fun initState() {
        val pc = pluginContext ?: return
        refreshLifecycleState(pc.preferences)
    }

    override val readingsInterval: BgReadingsInterval
        get() = externalSourceType.readingsInterval

    override val readingsTimeDelay: Minutes
        get() = externalSourceType.readingsTimeDelay

    override val status: StateFlow<GlucoseSourceStatus> = MutableStateFlow(GlucoseSourceStatus.Ok)
    override val startDate: StateFlow<Timestamp?> = MutableStateFlow(null)
    override val endDate: StateFlow<Expiration?> = MutableStateFlow(null)
    override val isExpired: StateFlow<Boolean> = MutableStateFlow(false)

    private val _lastConnection = MutableStateFlow<Timestamp?>(null)
    override val lastConnection: StateFlow<Timestamp?> = _lastConnection.asStateFlow()

    private val _sensorType = MutableStateFlow("External Receiver")
    override val sensorType: StateFlow<String> = _sensorType.asStateFlow()

    private val _hardwareInformation = MutableStateFlow<SourceHardwareInformation?>(null)
    override val hardwareInformation: StateFlow<SourceHardwareInformation?> = _hardwareInformation.asStateFlow()

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
    fun injectReading(value: BgReading, sensorType: String): Boolean {
        Log.d(TAG, "New glucose reading: $value")
        _sensorType.value = sensorType
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
        onNavigateUp: () -> Unit,
    ) {
        FramedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
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
                        onClick = onNavigateUp,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            externalSourceType = ExternalSourceType.xDrip5Min
                            pluginContext?.preferences?.let { prefs ->
                                scope.launch {
                                    prefs.putString(KEY_SOURCE_TYPE, externalSourceType.name)
                                }
                            }
                            _lifecycleState.value = DeviceLifecycleState.DeviceBound(
                                deviceId = null,
                                displayName = UiText.StringResource(R.string.glucose_broadcast_receiver_display_name)
                            )
                            onNavigateUp()
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Bind")
                    }
                }
            }
        }
    }

    private suspend fun refreshLifecycleState(preferences: PluginPreferences) {
        val sourceTypeStr = preferences.getString(KEY_SOURCE_TYPE, null).first()
        val parsedSourceType = sourceTypeStr?.let {
            try {
                ExternalSourceType.valueOf(it)
            } catch (_: Exception) {
                null
            }
        }
        if (parsedSourceType != null) {
            externalSourceType = parsedSourceType
            _lifecycleState.value = DeviceLifecycleState.DeviceBound(
                deviceId = null,
                displayName = UiText.StringResource(R.string.glucose_broadcast_receiver_display_name)
            )
        } else {
            _lifecycleState.value = DeviceLifecycleState.NoDevice
        }
    }

    /**
     * Re-connects to an external BroadcastReceiver glucose source using stored driver settings.
     */
    override suspend fun attach(): Result<GlucoseSource> {
        val pc = pluginContext
            ?: return Result.failure(IllegalStateException("PluginContext not initialized"))
        refreshLifecycleState(pc.preferences)

        if (_lifecycleState.value !is DeviceLifecycleState.DeviceBound) {
            return Result.failure(IllegalStateException("No saved source type configuration found for Receiver Glucose Plugin"))
        }

        return Result.success(this)
    }

    companion object {
        const val DRIVER_ID = "de.dh.daps.plugin.glucose.receiver"
        const val SOURCE_ID = "receiver-glucose-source"
        private const val KEY_SOURCE_TYPE = "source_type"
        val TAG = ReceiverGlucosePlugin::class.simpleName
        var instance: ReceiverGlucosePlugin? = null
    }
}