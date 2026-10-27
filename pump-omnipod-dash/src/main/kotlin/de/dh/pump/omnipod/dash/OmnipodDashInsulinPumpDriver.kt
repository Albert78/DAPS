package de.dh.pump.omnipod.dash

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.dh.daps.common.device.state.DeviceLifecycleState
import de.dh.daps.common.domain.model.therapy.InsulinPump
import de.dh.daps.common.device.driver.InsulinPumpDriver
import de.dh.daps.common.infrastructure.Plugin
import de.dh.daps.common.infrastructure.PluginContext
import de.dh.daps.common.ui.UiText
import de.dh.daps.ui.common.composables.PrimaryButton
import de.dh.pump.omnipod.dash.activation.OmnipodDashActivationManager
import de.dh.pump.omnipod.dash.db.OmnipodHistoryDatabase
import de.dh.pump.omnipod.dash.db.OmnipodHistoryEntity
import de.dh.pump.omnipod.dash.db.OmnipodHistoryEventType
import de.dh.pump.omnipod.dash.ui.OmnipodDashConfigurationScreen
import de.dh.pump.omnipod.dash.ui.OmnipodDashSetupScreen
import de.dh.pump.omnipod.protocol.ble.OmnipodDashBleManager
import de.dh.pump.omnipod.protocol.definition.ActivationProgress
import de.dh.pump.omnipod.protocol.state.OmnipodDashPodStateManagerImpl
import de.dh.pump.util.BluetoothPermissionUtils
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class OmnipodDashInsulinPumpDriver : InsulinPumpDriver, Plugin {
    override val driverId: String = DRIVER_ID
    override val displayName: UiText = UiText.StringResource(R.string.omnipod_dash_driver_display_name)
    override val pluginId: String = DRIVER_ID
    override val neededPermissions: Collection<String>
        get() = BluetoothPermissionUtils.neededPermissions

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

    override fun getSettingsScreen(pump: InsulinPump, onClose: () -> Unit): (@Composable () -> Unit) = {
        OmnipodDashConfigurationScreen(
            pump = pump,
            driver = this,
            onClose = onClose
        )
    }

    companion object {
        const val DRIVER_ID = "de.dh.daps.plugin.omnipod.dash"
        private const val TAG = "OmnipodDashDriver"
    }
}

@Composable
private fun OmnipodDashSettingsContent(
    onClose: () -> Unit
) {
    var confidenceReminders by remember { mutableStateOf(true) }
    var expirationAlerts by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Omnipod Dash Einstellungen",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        HorizontalDivider()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Bestätigungstöne",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Akustische Bestätigung bei Bolus-Befehlen",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = confidenceReminders,
                onCheckedChange = { confidenceReminders = it }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Pod-Ablaufwarnungen",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Warnmeldungen vor Pod-Ablauf anzeigen",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = expirationAlerts,
                onCheckedChange = { expirationAlerts = it }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            PrimaryButton(onClick = onClose) {
                Text(stringResource(de.dh.daps.common.R.string.action_close))
            }
        }
    }
}