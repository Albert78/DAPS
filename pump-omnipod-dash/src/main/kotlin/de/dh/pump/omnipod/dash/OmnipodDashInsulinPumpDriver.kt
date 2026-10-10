package de.dh.pump.omnipod.dash

import android.Manifest
import android.annotation.SuppressLint
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
import de.dh.pump.omnipod.protocol.state.OmnipodDashPodStateManagerImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class OmnipodDashInsulinPumpDriver : InsulinPumpDriver, Plugin {
    override val driverId: String = DRIVER_ID
    override val displayName: UiText = UiText.StringResource(R.string.omnipod_dash_driver_display_name)
    override val pluginId: String = DRIVER_ID
    override val neededPermissions: Collection<String> = listOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    private val _lifecycleState = MutableStateFlow<DeviceLifecycleState>(DeviceLifecycleState.NoDevice)
    override val lifecycleState: StateFlow<DeviceLifecycleState> = _lifecycleState.asStateFlow()

    private var pluginContext: PluginContext? = null

    override fun setup(context: PluginContext) {
        this.pluginContext = context
    }

    override fun initialize(context: PluginContext) {
        this.pluginContext = context
    }

    override suspend fun initState() {
        val prefs = pluginContext?.preferences ?: return
        val podStateManager = OmnipodDashPodStateManagerImpl(prefs)
        if (podStateManager.isActivationCompleted || !podStateManager.bluetoothAddress.isNullOrEmpty()) {
            _lifecycleState.value = DeviceLifecycleState.DeviceBound(
                deviceId = podStateManager.bluetoothAddress ?: "POD-DASH-ACTIVE",
                displayName = UiText.StringResource(R.string.omnipod_dash_pump_display_name),
            )
        } else {
            _lifecycleState.value = DeviceLifecycleState.NoDevice
        }
    }

    @Composable
    override fun SetupScreen(
        onNavigateUp: () -> Unit
    ) {
        OmnipodDashSetupScreen(
            driver = this,
            onNavigateUp = onNavigateUp
        )
    }

    @SuppressLint("MissingPermission")
    override suspend fun attach(): Result<InsulinPump> {
        val appContext = pluginContext?.appContext
            ?: return Result.failure(IllegalStateException("PluginContext not initialized. Call setup/initialize first."))

        return try {
            val podStateManager = OmnipodDashPodStateManagerImpl(pluginContext?.preferences)
            val bleManager = OmnipodDashBleManager(appContext, podStateManager)
            val pump = OmnipodDashPump(podStateManager, bleManager, appContext)
            _lifecycleState.value = DeviceLifecycleState.DeviceBound(
                deviceId = podStateManager.bluetoothAddress ?: "POD-DASH-ACTIVE",
                displayName = UiText.StringResource(R.string.omnipod_dash_pump_display_name),
            )
            Result.success(pump)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to Omnipod Dash pod", e)
            Result.failure(e)
        }
    }

    fun updateLifecycleState(state: DeviceLifecycleState) {
        _lifecycleState.value = state
    }

    fun getContext(): PluginContext? = pluginContext

    companion object {
        const val DRIVER_ID = "de.dh.daps.plugin.omnipod.dash"
        private const val TAG = "OmnipodDashDriver"
    }
}