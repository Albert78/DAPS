package de.dh.pump.danai.ui

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import de.dh.daps.common.model.InsulinPump
import de.dh.daps.common.model.InsulinPumpDriver
import de.dh.daps.common.model.Plugin
import de.dh.daps.common.model.PluginContext
import de.dh.daps.common.model.PluginPreferences
import de.dh.daps.common.model.PumpConnectionDescriptor
import de.dh.daps.common.ui.UiText
import de.dh.pump.danai.core.DanaILogger
import de.dh.pump.danai.core.DanaIPumpAdapter
import de.dh.pump.danai.core.DanaPumpPreferences
import de.dh.pump.danai.core.connection.DanaILink
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

import de.dh.pump.danai.R

/**
 * Driver implementation for the Sooil Dana-i insulin pump.
 * Implements [InsulinPumpDriver] and [Plugin] to integrate the Dana-i pump into DAPS.
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
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val defaultLogger = object : DanaILogger {
        override fun log(message: String) {
            Log.d(TAG, message)
        }
    }

    override fun setup(context: PluginContext) {
        this.pluginContext = context
    }

    override fun initialize(context: PluginContext) {
        this.pluginContext = context
    }

    /**
     * Renders the driver's custom UI workflow for scanning and pairing with a Dana-i pump.
     */
    @Composable
    override fun SetupScreen(
        onConnected: (InsulinPump, PumpConnectionDescriptor) -> Unit,
        onCancel: () -> Unit,
    ) {
        DanaISetupScreen(
            driver = this,
            onConnected = onConnected,
            onCancel = onCancel,
        )
    }

    /**
     * Re-establishes a connection with a previously paired Dana-i pump using its stored [descriptor].
     */
    @SuppressLint("MissingPermission")
    override suspend fun connect(descriptor: PumpConnectionDescriptor): Result<InsulinPump> {
        if (descriptor.driverId != DRIVER_ID) {
            return Result.failure(
                IllegalArgumentException("Invalid driver ID for Dana-i driver: ${descriptor.driverId}")
            )
        }
        val context = pluginContext?.appContext
            ?: return Result.failure(IllegalStateException("PluginContext not initialized. Call setup/initialize first."))
        val prefs = pluginContext?.preferences
            ?: return Result.failure(IllegalStateException("PluginPreferences not initialized."))

        return try {
            val address = descriptor.deviceId
            val name = descriptor.connectionParameters["name"] ?: descriptor.displayName

            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                ?: return Result.failure(IllegalStateException("BluetoothManager unavailable."))
            val adapter = bluetoothManager.adapter
                ?: return Result.failure(IllegalStateException("BluetoothAdapter unavailable."))

            val device = adapter.getRemoteDevice(address)
            val link = DanaILink(
                device = device,
                deviceName = name,
                preferences = prefs,
                logger = defaultLogger,
                context = context
            )

            val pumpAdapter = DanaIPumpAdapter.create(link, defaultLogger, scope)
            pumpAdapter.controller.connect()

            Result.success(pumpAdapter)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to Dana-i pump", e)
            Result.failure(e)
        }
    }

    /**
     * Creates a new connection link to a Dana-i pump during device pairing.
     */
    suspend fun createAndConnectPump(
        deviceName: String,
        deviceAddress: String,
        context: Context,
        preferences: PluginPreferences,
    ): Pair<InsulinPump, PumpConnectionDescriptor> {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter ?: throw IllegalStateException("Bluetooth adapter unavailable.")
        val device = adapter.getRemoteDevice(deviceAddress)

        // Save pump address in preferences
        DanaPumpPreferences(preferences).savePumpAddress(deviceName, deviceAddress)

        val link = DanaILink(
            device = device,
            deviceName = deviceName,
            preferences = preferences,
            logger = defaultLogger,
            context = context
        )

        val pumpAdapter = DanaIPumpAdapter.create(link, defaultLogger, scope)
        pumpAdapter.controller.connect()

        val descriptor = PumpConnectionDescriptor(
            driverId = DRIVER_ID,
            deviceId = deviceAddress,
            displayName = deviceName,
            connectionParameters = mapOf("name" to deviceName)
        )

        return Pair(pumpAdapter, descriptor)
    }

    internal fun getContext(): PluginContext? = pluginContext

    companion object {
        const val DRIVER_ID = "de.dh.daps.plugin.danai"
        private const val TAG = "DanaIDriver"
    }
}