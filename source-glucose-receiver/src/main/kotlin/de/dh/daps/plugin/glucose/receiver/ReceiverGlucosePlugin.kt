package de.dh.daps.plugin.glucose.receiver

import android.app.Application
import android.content.Context
import android.content.IntentFilter
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.dh.daps.common.device.driver.GlucoseSourceDriver
import de.dh.daps.common.device.state.DeviceLifecycleState
import de.dh.daps.common.domain.model.data.BgReading
import de.dh.daps.common.domain.model.data.Minutes
import de.dh.daps.common.domain.model.data.Timestamp
import de.dh.daps.common.domain.model.cgm.GlucoseSource
import de.dh.daps.common.domain.model.cgm.GlucoseSourceStatus
import de.dh.daps.common.domain.model.cgm.SourceHardwareInformation
import de.dh.daps.common.domain.model.data.BgReadingsInterval
import de.dh.daps.common.infrastructure.Plugin
import de.dh.daps.common.infrastructure.PluginContext
import de.dh.daps.common.infrastructure.PluginPreferences
import de.dh.daps.common.ui.UiText
import de.dh.daps.plugin.glucose.receiver.ui.ReceiverGlucoseSetupScreen
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

    override fun setup(context: PluginContext) {
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
    override val endDate: StateFlow<Timestamp?> = MutableStateFlow(null)
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

    fun saveAndBindSource(sourceType: ExternalSourceType) {
        pluginContext?.preferences?.let { prefs ->
            scope.launch {
                prefs.putString(KEY_SOURCE_TYPE, sourceType.name)
            }
        }
        bindSource(sourceType)
    }

    suspend fun unbindDevice() {
        pluginContext?.preferences?.remove(KEY_SOURCE_TYPE)
        stop()
        _activeGlucoseSource.value = null
        _lifecycleState.value = DeviceLifecycleState.NoDevice
    }

    /**
     * Renders setup screen branching by [DeviceLifecycleState].
     */
    @Composable
    override fun SetupScreen(
        onDriverReset: () -> Unit,
    ) {
        val currentState by lifecycleState.collectAsStateWithLifecycle()

        ReceiverGlucoseSetupScreen(
            lifecycleState = currentState,
            externalSourceType = externalSourceType,
            onSelectSourceType = { sourceType ->
                saveAndBindSource(sourceType)
            },
            onUnbindDevice = {
                scope.launch {
                    unbindDevice()
                    onDriverReset()
                }
            },
        )
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

    override fun getSettingsScreen(
        glucoseSource: GlucoseSource,
        onClose: () -> Unit,
    ): (@Composable () -> Unit)? = null

    companion object {
        const val DRIVER_ID = "de.dh.daps.plugin.glucose.receiver"
        const val SOURCE_ID = "receiver-glucose-source"
        private const val KEY_SOURCE_TYPE = "source_type"
        val TAG = ReceiverGlucosePlugin::class.simpleName
        var instance: ReceiverGlucosePlugin? = null
    }
}