package de.dh.pump.danai

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
import de.dh.daps.common.ui.UiText
import de.dh.pump.danai.core.DanaILogger
import de.dh.pump.danai.core.DanaIPumpAdapter
import de.dh.pump.danai.core.DanaPumpPreferences
import de.dh.pump.danai.core.connection.DanaILink
import de.dh.pump.danai.ui.DanaIPumpControl
import de.dh.pump.danai.ui.DanaISetupScreen
import de.dh.pump.util.BluetoothBondUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

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

    override fun setup(context: PluginContext) {
        this.pluginContext = context
    }

    override suspend fun initState() {
        val pc = pluginContext ?: return
        val danaPrefs = DanaPumpPreferences(pc.preferences)
        val address = danaPrefs.getLastAddress()
        val name = danaPrefs.getLastName().ifEmpty { "Dana-i" }

        if (address.isNotEmpty()) {
            try {
                bindPump(name, address)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to bind Dana-i pump during initState", e)
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
        onDriverReset: () -> Unit,
    ) {
        DanaISetupScreen(
            driver = this,
            onDriverReset = onDriverReset,
        )
    }

    override fun getPumpControlSection(pump: InsulinPump): @Composable (() -> Unit) = {
        DanaIPumpControl(pump = pump)
    }

    private suspend fun bindPump(
        deviceName: String,
        deviceAddress: String,
    ): InsulinPump {
        val pc = pluginContext ?: throw IllegalStateException("PluginContext not initialized")
        val bluetoothManager = pc.appContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter ?: throw IllegalStateException("Bluetooth adapter unavailable.")
        val device = adapter.getRemoteDevice(deviceAddress)

        val link = DanaILink(
            device = device,
            deviceName = deviceName,
            preferences = pc.preferences,
            logger = defaultLogger,
            context = pc.appContext,
        )

        val pumpAdapter = DanaIPumpAdapter.create(link, defaultLogger, scope)
        pumpAdapter.controller.connect()

        _lifecycleState.value = DeviceLifecycleState.DeviceBound(
            deviceId = deviceAddress,
            displayName = UiText.DynamicString(deviceName),
        )
        _activePump.value = pumpAdapter

        return pumpAdapter
    }

    /**
     * Binds a Dana-i pump during device pairing and saves the configuration preferences.
     */
    suspend fun bindPumpDevice(
        deviceName: String,
        deviceAddress: String,
    ): InsulinPump {
        val pc = pluginContext ?: throw IllegalStateException("PluginContext not initialized")
        DanaPumpPreferences(pc.preferences).savePumpAddress(deviceName, deviceAddress)
        return bindPump(deviceName, deviceAddress)
    }

    /**
     * Unbinds the currently bound Dana-i pump, clears stored preferences,
     * disconnects active connection, removes the Bluetooth bond, and resets the driver lifecycle state to NoDevice.
     */
    suspend fun unbindDevice() = withContext(NonCancellable) {
        val pc = pluginContext
        val pumpAdapter = _activePump.value as? DanaIPumpAdapter
        var deviceToUnbond = pumpAdapter?.controller?.device

        if ((deviceToUnbond == null) && (pc != null)) {
            val address = DanaPumpPreferences(pc.preferences).getLastAddress()
            if (address.isNotEmpty()) {
                val bluetoothManager = pc.appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                deviceToUnbond = bluetoothManager?.adapter?.getRemoteDevice(address)
            }
        }

        pc?.let {
            DanaPumpPreferences(it.preferences).clear()
        }

        pumpAdapter?.controller?.disconnect()

        deviceToUnbond?.let { device ->
            BluetoothBondUtils.removeBond(device)
        }

        _activePump.value = null
        _lifecycleState.value = DeviceLifecycleState.NoDevice
    }

    internal fun getContext(): PluginContext? = pluginContext

    companion object {
        const val DRIVER_ID = "de.dh.daps.plugin.danai"
        private const val TAG = "DanaIDriver"
    }
}