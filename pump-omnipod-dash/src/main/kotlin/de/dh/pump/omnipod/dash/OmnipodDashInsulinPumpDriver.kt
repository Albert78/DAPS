package de.dh.pump.omnipod.dash

import android.Manifest
import android.os.Build
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
import de.dh.pump.omnipod.dash.activation.OmnipodDashActivationManager
import de.dh.pump.omnipod.dash.db.OmnipodHistoryDatabase
import de.dh.pump.omnipod.dash.db.OmnipodHistoryEntity
import de.dh.pump.omnipod.dash.db.OmnipodHistoryEventType
import de.dh.pump.omnipod.dash.ui.OmnipodDashSetupScreen
import de.dh.pump.omnipod.protocol.ble.OmnipodDashBleManager
import de.dh.pump.omnipod.protocol.definition.ActivationProgress
import de.dh.pump.omnipod.protocol.state.OmnipodDashPodStateManagerImpl
import de.dh.pump.util.BluetoothBondUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class OmnipodDashInsulinPumpDriver : InsulinPumpDriver, Plugin {
    override val driverId: String = DRIVER_ID
    override val displayName: UiText = UiText.StringResource(R.string.omnipod_dash_driver_display_name)
    override val pluginId: String = DRIVER_ID
    override val neededPermissions: Collection<String>
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            listOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _lifecycleState = MutableStateFlow<DeviceLifecycleState>(DeviceLifecycleState.NoDevice)
    override val lifecycleState: StateFlow<DeviceLifecycleState> = _lifecycleState.asStateFlow()

    private val _activePump = MutableStateFlow<InsulinPump?>(null)
    override val activePump: StateFlow<InsulinPump?> = _activePump.asStateFlow()

    private var pluginContext: PluginContext? = null
    private var podStateManager: OmnipodDashPodStateManagerImpl? = null
    private var bleManager: OmnipodDashBleManager? = null

    fun getPodStateManager(): OmnipodDashPodStateManagerImpl {
        val pc = pluginContext ?: throw IllegalStateException("PluginContext not initialized")
        return podStateManager ?: OmnipodDashPodStateManagerImpl(pc.preferences).also {
            podStateManager = it
        }
    }

    fun getBleManager(): OmnipodDashBleManager {
        val stateManager = getPodStateManager()
        val pc = pluginContext ?: throw IllegalStateException("PluginContext not initialized")
        return bleManager ?: OmnipodDashBleManager(pc.appContext, stateManager).also {
            bleManager = it
        }
    }

    fun getActivationManager(): OmnipodDashActivationManager {
        val pc = pluginContext ?: throw IllegalStateException("PluginContext not initialized")
        val dao = OmnipodHistoryDatabase.getInstance(pc.appContext).historyDao()
        return OmnipodDashActivationManager(getPodStateManager(), getBleManager(), dao)
    }

    override fun setup(context: PluginContext) {
        this.pluginContext = context
    }

    override fun initialize(context: PluginContext) {
        this.pluginContext = context
    }

    override suspend fun initState() {
        if (pluginContext == null) return
        val stateManager = getPodStateManager()
        stateManager.loadFromPreferences()
        when {
            stateManager.isActivationCompleted -> {
                try {
                    bindPod()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to connect to Omnipod Dash in initState", e)
                }
            }
            stateManager.activationProgress != ActivationProgress.NOT_STARTED -> {
                _lifecycleState.value = DeviceLifecycleState.SetupInProgress
                _activePump.value = null
            }
            else -> {
                _lifecycleState.value = DeviceLifecycleState.NoDevice
                _activePump.value = null
            }
        }
    }

    /**
     * Marks the setup flow as currently in progress.
     */
    fun startSetup() {
        if (_lifecycleState.value !is DeviceLifecycleState.DeviceBound) {
            _lifecycleState.value = DeviceLifecycleState.SetupInProgress
        }
    }

    /**
     * Cancels the current setup flow, resets internal pod state and preferences,
     * and sets driver lifecycle state to NoDevice.
     */
    suspend fun cancelSetup() {
        resetDriverState()
    }

    @Composable
    override fun SetupScreen(
        onDriverReset: () -> Unit
    ) {
        OmnipodDashSetupScreen(
            driver = this,
            onDriverReset = onDriverReset
        )
    }

    /**
     * Instantiates the pump hardware stack and updates driver lifecycle and active pump state.
     */
    private fun bindPod(): OmnipodDashPump {
        val pc = pluginContext ?: throw IllegalStateException("PluginContext not initialized")
        val stateManager = getPodStateManager()
        val ble = getBleManager()
        val pump = OmnipodDashPump(stateManager, ble, pc.appContext)
        _lifecycleState.value = DeviceLifecycleState.DeviceBound(
            deviceId = stateManager.bluetoothAddress ?: "POD-DASH-ACTIVE",
            displayName = UiText.StringResource(R.string.omnipod_dash_pump_display_name),
        )
        _activePump.value = pump
        return pump
    }

    /**
     * Completes pod activation and instantiates the active [OmnipodDashPump].
     */
    fun completeActivation() {
        bindPod()
    }

    /**
     * Unbinds the currently bound Omnipod Dash pod, clears pod state and preferences,
     * removes the Bluetooth bond, and resets the driver lifecycle state to NoDevice.
     */
    suspend fun unbindDevice() = withContext(NonCancellable) {
        resetDriverState()
    }

    private suspend fun resetDriverState() {
        val address = podStateManager?.bluetoothAddress
        pluginContext?.appContext?.let { ctx ->
            try {
                OmnipodHistoryDatabase.getInstance(ctx).historyDao().insertEvent(
                    OmnipodHistoryEntity(
                        timestampMs = System.currentTimeMillis(),
                        eventType = OmnipodHistoryEventType.POD_DEACTIVATED,
                        detailText = "Pod deactivated",
                        unitsDelivered = 0.0
                    )
                )
            } catch (_: Exception) {}

            if (!address.isNullOrEmpty()) {
                val bluetoothManager = ctx.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                val device = bluetoothManager?.adapter?.getRemoteDevice(address)
                device?.let { BluetoothBondUtils.removeBond(it) }
            }
        }
        bleManager?.disconnect()
        bleManager = null
        podStateManager?.reset()
        podStateManager?.saveToPreferences()
        podStateManager = null
        pluginContext?.preferences?.clear()
        _activePump.value = null
        _lifecycleState.value = DeviceLifecycleState.NoDevice
    }

    fun getContext(): PluginContext? = pluginContext

    companion object {
        const val DRIVER_ID = "de.dh.daps.plugin.omnipod.dash"
        private const val TAG = "OmnipodDashDriver"
    }
}