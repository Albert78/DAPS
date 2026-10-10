package de.dh.pump.danai.ui

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import de.dh.daps.common.model.DeviceLifecycleState
import de.dh.daps.common.model.InsulinPump
import de.dh.daps.common.model.InsulinPumpDriver
import de.dh.daps.common.model.Plugin
import de.dh.daps.common.model.PluginContext
import de.dh.daps.common.model.PluginPreferences
import de.dh.daps.common.ui.UiText
import de.dh.pump.danai.R
import de.dh.pump.danai.core.DanaILogger
import de.dh.pump.danai.core.DanaIPumpAdapter
import de.dh.pump.danai.core.DanaPumpPreferences
import de.dh.pump.danai.core.connection.DanaILink
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Insulin pump driver plugin for Sooil Dana-i pumps.
 * Provides user UI scanning workflows and reconnect capability via BLE.
 */
class DanaIInsulinPumpDriver : InsulinPumpDriver, Plugin {
    override val driverId: String = DRIVER_ID
    override val displayName: UiText = UiText.StringResource(R.string.danai_pump_driver_display_name)
    override val pluginId: String = DRIVER_ID
    override val neededPermissions: Collection<String> = listOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.ACCESS_FINE_LOCATION,
    )

    private var pluginContext: PluginContext? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val defaultLogger = object : DanaILogger {
        override fun log(message: String) {
            Log.d(TAG, message)
        }
    }

    private val _lifecycleState = MutableStateFlow<DeviceLifecycleState>(DeviceLifecycleState.NoDevice)
    override val lifecycleState: StateFlow<DeviceLifecycleState> = _lifecycleState.asStateFlow()

    private val _activePump = MutableStateFlow<InsulinPump?>(null)
    override val activePump: StateFlow<InsulinPump?> = _activePump.asStateFlow()

    override fun initialize(context: PluginContext) {
        this.pluginContext = context
    }

    override suspend fun initState() {
        val pc = pluginContext ?: return
        val danaPrefs = DanaPumpPreferences(pc.preferences)
        val address = danaPrefs.getLastAddress()
        val name = danaPrefs.getLastName().ifEmpty { "Dana-i" }

        if (address.isNotEmpty()) {
            _lifecycleState.value = DeviceLifecycleState.DeviceBound(
                deviceId = address,
                displayName = UiText.DynamicString(name),
            )
            try {
                val pump = doConnectPump(name, address, pc.appContext, pc.preferences)
                _activePump.value = pump
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect to Dana-i pump during initState", e)
            }
        } else {
            _lifecycleState.value = DeviceLifecycleState.NoDevice
            _activePump.value = null
        }
    }

    /**
     * Renders the driver's custom UI workflow for scanning and pairing with a Dana-i pump.
     */
    @Composable
    override fun SetupScreen(
        onNavigateUp: () -> Unit,
    ) {
        DanaISetupScreen(
            driver = this,
            onNavigateUp = onNavigateUp,
        )
    }

    private suspend fun doConnectPump(
        deviceName: String,
        deviceAddress: String,
        context: Context,
        preferences: PluginPreferences,
    ): InsulinPump {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter ?: throw IllegalStateException("Bluetooth adapter unavailable.")
        val device = adapter.getRemoteDevice(deviceAddress)

        val link = DanaILink(
            device = device,
            deviceName = deviceName,
            preferences = preferences,
            logger = defaultLogger,
            context = context
        )

        val pumpAdapter = DanaIPumpAdapter.create(link, defaultLogger, scope)
        pumpAdapter.controller.connect()

        _lifecycleState.value = DeviceLifecycleState.DeviceBound(
            deviceId = deviceAddress,
            displayName = UiText.DynamicString(deviceName),
        )

        return pumpAdapter
    }

    /**
     * Creates a new connection link to a Dana-i pump during device pairing.
     */
    suspend fun createAndConnectPump(
        deviceName: String,
        deviceAddress: String,
        context: Context,
        preferences: PluginPreferences,
    ): InsulinPump {
        val pump = doConnectPump(deviceName, deviceAddress, context, preferences)
        DanaPumpPreferences(preferences).savePumpAddress(deviceName, deviceAddress)
        _activePump.value = pump
        return pump
    }

    internal fun getContext(): PluginContext? = pluginContext

    companion object {
        const val DRIVER_ID = "de.dh.daps.plugin.danai"
        private const val TAG = "DanaIDriver"
    }
}