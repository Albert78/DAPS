package de.dh.daps.plugin.glucose.receiver

import android.app.Application
import android.content.Context
import android.content.IntentFilter
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

    private val _activeGlucoseSource = MutableStateFlow<GlucoseSource?>(null)
    override val activeGlucoseSource: StateFlow<GlucoseSource?> = _activeGlucoseSource.asStateFlow()

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

    private fun bindSource(sourceType: ExternalSourceType) {
        externalSourceType = sourceType
        _lifecycleState.value = DeviceLifecycleState.DeviceBound(
            deviceId = null,
            displayName = UiText.StringResource(R.string.glucose_broadcast_receiver_display_name),
        )
        _activeGlucoseSource.value = this
    }

    suspend fun unbindDevice() {
        pluginContext?.preferences?.let { prefs ->
            prefs.remove(KEY_SOURCE_TYPE)
        }
        stop()
        _activeGlucoseSource.value = null
        _lifecycleState.value = DeviceLifecycleState.NoDevice
    }

    /**
     * Renders setup screen branching by [DeviceLifecycleState].
     */
    @Composable
    override fun SetupScreen(
        onNavigateUp: () -> Unit,
    ) {
        val currentState by lifecycleState.collectAsStateWithLifecycle()

        when (val state = currentState) {
            is DeviceLifecycleState.DeviceBound -> {
                FramedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = state.displayName.asString(),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Broadcast Empfänger (xDrip+ / Juggluco)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Badge(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ) {
                                Text(
                                    text = "Gebunden",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        HorizontalDivider()

                        Text(
                            text = "Der Broadcast-Empfänger nimmt Glukosewerte von externen Apps entgegen.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    scope.launch { unbindDevice() }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error,
                                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                            ) {
                                Text("Bindung verwerfen")
                            }

                            Button(
                                onClick = onNavigateUp,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("Fertig")
                            }
                        }
                    }
                }
            }
            else -> {
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
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Empfange Blutzuckerwerte, die von externen Apps wie xDrip+ oder Juggluco gesendet werden.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = onNavigateUp,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("Abbrechen")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val sourceType = ExternalSourceType.xDrip5Min
                                    pluginContext?.preferences?.let { prefs ->
                                        scope.launch {
                                            prefs.putString(KEY_SOURCE_TYPE, sourceType.name)
                                        }
                                    }
                                    bindSource(sourceType)
                                    onNavigateUp()
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("Einrichten")
                            }
                        }
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
            bindSource(parsedSourceType)
        } else {
            _lifecycleState.value = DeviceLifecycleState.NoDevice
            _activeGlucoseSource.value = null
        }
    }

    companion object {
        const val DRIVER_ID = "de.dh.daps.plugin.glucose.receiver"
        const val SOURCE_ID = "receiver-glucose-source"
        private const val KEY_SOURCE_TYPE = "source_type"
        val TAG = ReceiverGlucosePlugin::class.simpleName
        var instance: ReceiverGlucosePlugin? = null
    }
}