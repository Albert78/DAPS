package de.dh.pump.app

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import de.dh.daps.common.model.PluginPreferences
import de.dh.pump.danai.core.DanaIController
import de.dh.pump.danai.core.connection.DanaILink
import de.dh.pump.danai.core.DanaILogger
import de.dh.pump.danai.core.DanaIPump
import de.dh.pump.danai.core.DanaPumpPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object DanaIPumpManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var _preferences: PluginPreferences? = null
    val preferences: PluginPreferences? get() = _preferences

    private val _danaIPump = MutableStateFlow<DanaIPump?>(null)
    /**
     * The primary high-level interface to the current Dana pump.
     */
    val danaIPump = _danaIPump.asStateFlow()

    private val _controller = MutableStateFlow<DanaIController?>(null)
    /**
     * The orchestrator managing the connection and sync for the current pump.
     */
    val controller = _controller.asStateFlow()

    val appLogger = object : DanaILogger {
        override fun log(message: String) {
            LogManager.log(message)
        }
    }

    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences("pump_prefs", Context.MODE_PRIVATE)
        _preferences = SharedPreferencesPluginPreferences(prefs)
    }

    private fun setPump(link: DanaILink?) {
        scope.launch {
            _controller.value?.stop()
            _controller.value = null
            _danaIPump.value = null

            if (link != null) {
                val newPump = DanaIPump()
                val newController = DanaIController(newPump, link, appLogger, scope)
                _controller.value = newController
                _danaIPump.value = newPump
            }
        }
    }

    private suspend fun tryLoadSavedPumpDescriptor(context: Context): PumpDescriptor? {
        val prefs = _preferences ?: return null
        val danaPrefs = DanaPumpPreferences(prefs)
        val address = danaPrefs.getLastAddress()
        val name = danaPrefs.getLastName()

        if (address.isBlank()) {
            LogManager.log("No saved pump data found.")
            return null
        }

        LogManager.log("Attempting to restore pump descriptor for $name ($address)...")
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter ?: return null

        val device = adapter.getRemoteDevice(address)
        return PumpDescriptor(
            name = name,
            address = address,
            device = device
        )
    }

    private suspend fun saveLink(pump: PumpDescriptor) {
        LogManager.log("Saving pump address of ${pump.name}...")
        _preferences?.let {
            DanaPumpPreferences(it).savePumpAddress(pump.name, pump.address)
        }
    }

    private suspend fun clearLink() {
        LogManager.log("Clearing pump address...")
        _preferences?.clear()
    }

    /**
     * Clears all persistent pairing data and terminates the current connection.
     */
    fun forgetDevice() {
        scope.launch {
            clearLink()
            setPump(null)
        }
    }

    /**
     * Attempts to reconstruct a previously saved pump connection from persistent storage.
     */
    @SuppressLint("MissingPermission")
    fun tryLoadSavedPumpLink(context: Context) {
        val prefs = _preferences ?: return
        scope.launch {
            val pumpDescriptor = tryLoadSavedPumpDescriptor(context) ?: return@launch
            try {
                val link = DanaILink(
                    device = pumpDescriptor.device,
                    deviceName = pumpDescriptor.name,
                    preferences = prefs,
                    logger = appLogger,
                    context = context
                )
                LogManager.log("Pump association restored. System is ready.")

                setPump(link)
            } catch (e: Exception) {
                LogManager.log("Failed to restore saved device ${pumpDescriptor.name}: ${e.message}")
            }
        }
    }

    /**
     * Creates a new pump link and replaces the current link, if present.
     */
    fun createNewPumpLink(pump: PumpDescriptor, context: Context) {
        val prefs = _preferences ?: return
        scope.launch {
            val link = DanaILink(
                device = pump.device,
                deviceName = pump.name,
                preferences = prefs,
                logger = appLogger,
                context = context
            )
            saveLink(pump)
            LogManager.log("Created new link to ${pump.name}. System is ready.")
            setPump(link)
        }
    }
}