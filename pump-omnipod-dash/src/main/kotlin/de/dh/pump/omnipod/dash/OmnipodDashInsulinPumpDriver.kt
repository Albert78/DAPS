package de.dh.pump.omnipod.dash

import android.Manifest
import android.util.Log
import androidx.compose.runtime.Composable
import de.dh.daps.common.model.DeviceLifecycleState
import de.dh.daps.common.model.InsulinPump
import de.dh.daps.common.model.InsulinPumpDriver
import de.dh.daps.common.model.Plugin
import de.dh.daps.common.model.PluginContext
import de.dh.daps.common.ui.UiText
import de.dh.pump.omnipod.dash.ui.OmnipodDashSetupScreen
import de.dh.pump.omnipod.protocol.ble.OmnipodDashBleManager
import de.dh.pump.omnipod.protocol.definition.ActivationProgress
import de.dh.pump.omnipod.protocol.state.OmnipodDashPodStateManagerImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OmnipodDashInsulinPumpDriver : InsulinPumpDriver, Plugin {
    override val driverId: String = DRIVER_ID
    override val displayName: UiText = UiText.StringResource(R.string.omnipod_dash_driver_display_name)
    override val pluginId: String = DRIVER_ID
    override val neededPermissions: Collection<String> = listOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _lifecycleState = MutableStateFlow<DeviceLifecycleState>(DeviceLifecycleState.NoDevice)
    override val lifecycleState: StateFlow<DeviceLifecycleState> = _lifecycleState.asStateFlow()

    private val _activePump = MutableStateFlow<InsulinPump?>(null)
    override val activePump: StateFlow<InsulinPump?> = _activePump.asStateFlow()

    private var pluginContext: PluginContext? = null

    override fun setup(context: PluginContext) {
        this.pluginContext = context
    }

    override fun initialize(context: PluginContext) {
        this.pluginContext = context
    }

    override suspend fun initState() {
        val pc = pluginContext ?: return
        val podStateManager = OmnipodDashPodStateManagerImpl(pc.preferences)
        podStateManager.loadFromPreferences()
        when {
            podStateManager.isActivationCompleted -> {
                try {
                    bindPod(podStateManager)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to connect to Omnipod Dash pod in initState", e)
                }
            }
            podStateManager.activationProgress != ActivationProgress.NOT_STARTED -> {
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
        val pc = pluginContext
        if (pc != null) {
            val podStateManager = OmnipodDashPodStateManagerImpl(pc.preferences)
            podStateManager.reset()
            podStateManager.saveToPreferences()
            pc.preferences.clear()
        }
        _activePump.value = null
        _lifecycleState.value = DeviceLifecycleState.NoDevice
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
    private fun bindPod(podStateManager: OmnipodDashPodStateManagerImpl): OmnipodDashPump {
        val pc = pluginContext ?: throw IllegalStateException("PluginContext not initialized")
        val bleManager = OmnipodDashBleManager(pc.appContext, podStateManager)
        val pump = OmnipodDashPump(podStateManager, bleManager, pc.appContext)
        _lifecycleState.value = DeviceLifecycleState.DeviceBound(
            deviceId = podStateManager.bluetoothAddress ?: "POD-DASH-ACTIVE",
            displayName = UiText.StringResource(R.string.omnipod_dash_pump_display_name),
        )
        _activePump.value = pump
        return pump
    }

    /**
     * Completes pod activation and instantiates the active [OmnipodDashPump].
     */
    fun completeActivation(podStateManager: OmnipodDashPodStateManagerImpl) {
        bindPod(podStateManager)
    }

    /**
     * Unbinds the currently bound Omnipod Dash pod, clears pod state and preferences,
     * and resets the driver lifecycle state to NoDevice.
     */
    suspend fun unbindDevice() {
        val pc = pluginContext
        if (pc != null) {
            val podStateManager = OmnipodDashPodStateManagerImpl(pc.preferences)
            podStateManager.reset()
            pc.preferences.clear()
        }
        _activePump.value = null
        _lifecycleState.value = DeviceLifecycleState.NoDevice
    }

    fun getContext(): PluginContext? = pluginContext

    companion object {
        const val DRIVER_ID = "de.dh.daps.plugin.omnipod.dash"
        private const val TAG = "OmnipodDashDriver"
    }
}