package de.dh.pump.omnipod.dash

import android.Manifest
import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.runtime.Composable
import de.dh.daps.common.model.InsulinPump
import de.dh.daps.common.model.InsulinPumpDriver
import de.dh.daps.common.model.Plugin
import de.dh.daps.common.model.PluginContext
import de.dh.daps.common.model.PumpConnectionDescriptor
import de.dh.daps.common.ui.UiText
import de.dh.pump.omnipod.dash.ui.OmnipodDashSetupScreen
import de.dh.pump.omnipod.protocol.ble.OmnipodDashBleManager
import de.dh.pump.omnipod.protocol.state.OmnipodDashPodStateManagerImpl

class OmnipodDashInsulinPumpDriver : InsulinPumpDriver, Plugin {
    override val driverId: String = DRIVER_ID
    override val displayName: UiText = UiText.StringResource(R.string.omnipod_dash_driver_display_name)
    override val pluginId: String = DRIVER_ID
    override val neededPermissions: Collection<String> = listOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    private var pluginContext: PluginContext? = null

    override fun setup(context: PluginContext) {
        this.pluginContext = context
    }

    override fun initialize(context: PluginContext) {
        this.pluginContext = context
    }

    @Composable
    override fun SetupScreen(
        onConnected: (InsulinPump, PumpConnectionDescriptor) -> Unit,
        onCancel: () -> Unit
    ) {
        OmnipodDashSetupScreen(
            driver = this,
            onConnected = onConnected,
            onCancel = onCancel
        )
    }

    @SuppressLint("MissingPermission")
    override suspend fun connect(descriptor: PumpConnectionDescriptor): Result<InsulinPump> {
        if (descriptor.driverId != DRIVER_ID) {
            return Result.failure(
                IllegalArgumentException("Invalid driver ID for Omnipod Dash driver: ${descriptor.driverId}")
            )
        }
        val appContext = pluginContext?.appContext
            ?: return Result.failure(IllegalStateException("PluginContext not initialized. Call setup/initialize first."))

        return try {
            val podStateManager = OmnipodDashPodStateManagerImpl(pluginContext?.preferences)
            val bleManager = OmnipodDashBleManager(appContext, podStateManager)
            val pump = OmnipodDashPump(podStateManager, bleManager)
            Result.success(pump)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to Omnipod Dash pod", e)
            Result.failure(e)
        }
    }

    fun getContext(): PluginContext? = pluginContext

    companion object {
        const val DRIVER_ID = "de.dh.daps.plugin.omnipod.dash"
        private const val TAG = "OmnipodDashDriver"
    }
}