package de.dh.daps.plugin.pump

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.dh.daps.common.model.DeviceLifecycleState
import de.dh.daps.common.model.InsulinPump
import de.dh.daps.common.model.InsulinPumpDriver
import de.dh.daps.common.model.Plugin
import de.dh.daps.common.model.PluginContext
import de.dh.daps.common.ui.UiText
import de.dh.daps.ui.common.composables.FramedCard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Lightweight sample implementation of [InsulinPumpDriver].
 * Demonstrates the basic structure of a pump driver plugin without complex logic.
 */
class SampleInsulinPumpDriver : InsulinPumpDriver, Plugin {
    override val driverId: String = DRIVER_ID
    override val displayName: UiText = UiText.StringResource(R.string.sample_pump_driver_display_name)
    override val pluginId: String = DRIVER_ID
    override val neededPermissions: Collection<String> = emptyList()

    private var pluginContext: PluginContext? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _lifecycleState = MutableStateFlow<DeviceLifecycleState>(DeviceLifecycleState.NoDevice)
    override val lifecycleState: StateFlow<DeviceLifecycleState> = _lifecycleState.asStateFlow()

    private val _activePump = MutableStateFlow<InsulinPump?>(null)
    override val activePump: StateFlow<InsulinPump?> = _activePump.asStateFlow()

    override fun initialize(context: PluginContext) {
        this.pluginContext = context
    }

    private fun bindDevice(deviceId: String, deviceName: String) {
        _lifecycleState.value = DeviceLifecycleState.DeviceBound(
            deviceId = deviceId,
            displayName = UiText.DynamicString(deviceName),
        )
        _activePump.value = SampleInsulinPump()
    }

    suspend fun unbindDevice() {
        pluginContext?.preferences?.let { prefs ->
            prefs.remove(KEY_DEVICE_ID)
            prefs.remove(KEY_DEVICE_NAME)
        }
        _activePump.value = null
        _lifecycleState.value = DeviceLifecycleState.NoDevice
    }

    override suspend fun initState() {
        val pc = pluginContext ?: return
        val deviceId = pc.preferences.getString(KEY_DEVICE_ID).first()
        val deviceName = pc.preferences.getString(KEY_DEVICE_NAME).first() ?: "Sample Pump 01"
        if (!deviceId.isNullOrEmpty()) {
            bindDevice(deviceId, deviceName)
        } else {
            _lifecycleState.value = DeviceLifecycleState.NoDevice
            _activePump.value = null
        }
    }

    /**
     * Renders setup screen branching by [DeviceLifecycleState].
     */
    @Composable
    override fun SetupScreen(
        onNavigateUp: () -> Unit,
    ) {
        val currentState by lifecycleState.collectAsStateWithLifecycle()

        when (val state = currentState) {
            is DeviceLifecycleState.DeviceBound -> {
                FramedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = state.displayName.asString(),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                state.deviceId?.let { id ->
                                    Text(
                                        text = id,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Badge(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ) {
                                Text(
                                    text = "Gebunden",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        HorizontalDivider()

                        Text(
                            text = "Die Sample-Pumpe ist zurzeit als aktive Insulinpumpe eingerichtet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    scope.launch { unbindDevice() }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error,
                                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                            ) {
                                Text("Bindung verwerfen")
                            }

                            Button(
                                onClick = onNavigateUp,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("Fertig")
                            }
                        }
                    }
                }
            }
            else -> {
                FramedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Sample Pump Setup",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Bestätige das Einrichten der simulierten Sample-Pumpe.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onNavigateUp,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("Abbrechen")
                            }
                            Button(
                                onClick = {
                                    val deviceId = "sample-pump-device-01"
                                    val deviceName = "Sample Pump 01"
                                    pluginContext?.preferences?.let { prefs ->
                                        scope.launch {
                                            prefs.putString(KEY_DEVICE_ID, deviceId)
                                            prefs.putString(KEY_DEVICE_NAME, deviceName)
                                        }
                                    }
                                    bindDevice(deviceId, deviceName)
                                    onNavigateUp()
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("Einrichten")
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val DRIVER_ID = "de.dh.daps.plugin.pump.sample"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_DEVICE_NAME = "device_name"
    }
}