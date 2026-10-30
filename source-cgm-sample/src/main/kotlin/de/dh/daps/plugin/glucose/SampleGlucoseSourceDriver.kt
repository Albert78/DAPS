package de.dh.daps.plugin.glucose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import de.dh.daps.ui.common.composables.AppAlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.ButtonDefaults
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.dh.daps.common.device.driver.GlucoseSourceDriver
import de.dh.daps.common.device.state.DeviceLifecycleState
import de.dh.daps.common.domain.model.cgm.GlucoseSource
import de.dh.daps.common.infrastructure.Plugin
import de.dh.daps.common.infrastructure.PluginContext
import de.dh.daps.common.ui.UiText
import de.dh.daps.ui.common.composables.FramedCard
import de.dh.daps.ui.common.composables.NormalButton
import de.dh.daps.ui.common.composables.NormalTextButton
import de.dh.daps.ui.common.composables.PrimaryButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Lightweight sample implementation of [GlucoseSourceDriver].
 * Demonstrates the basic structure of a glucose source driver plugin without complex logic.
 */
class SampleGlucoseSourceDriver : GlucoseSourceDriver, Plugin {
    override val driverId: String = DRIVER_ID
    override val displayName: UiText = UiText.StringResource(R.string.sample_cgm_driver_display_name)
    override val pluginId: String = DRIVER_ID
    override val neededPermissions: Collection<String> = emptyList()

    private var pluginContext: PluginContext? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _lifecycleState = MutableStateFlow<DeviceLifecycleState>(DeviceLifecycleState.NoDevice)
    override val lifecycleState: StateFlow<DeviceLifecycleState> = _lifecycleState.asStateFlow()

    private val _activeGlucoseSource = MutableStateFlow<GlucoseSource?>(null)
    override val activeGlucoseSource: StateFlow<GlucoseSource?> = _activeGlucoseSource.asStateFlow()

    override fun setup(context: PluginContext) {
        this.pluginContext = context
    }

    private fun bindDevice(deviceId: String, deviceName: String) {
        _lifecycleState.value = DeviceLifecycleState.DeviceBound(
            deviceId = deviceId,
            displayName = UiText.DynamicString(deviceName),
        )
        _activeGlucoseSource.value = SampleGlucoseSource()
    }

    suspend fun unbindDevice() {
        pluginContext?.preferences?.let { prefs ->
            prefs.remove(KEY_DEVICE_ID)
            prefs.remove(KEY_DEVICE_NAME)
        }
        _activeGlucoseSource.value = null
        _lifecycleState.value = DeviceLifecycleState.NoDevice
    }

    override suspend fun initState() {
        val pc = pluginContext ?: return
        val deviceId = pc.preferences.getString(KEY_DEVICE_ID).first()
        val deviceName = pc.preferences.getString(KEY_DEVICE_NAME).first() ?: "Sample Glucose Sensor 01"
        if (!deviceId.isNullOrEmpty()) {
            bindDevice(deviceId, deviceName)
        } else {
            _lifecycleState.value = DeviceLifecycleState.NoDevice
            _activeGlucoseSource.value = null
        }
    }

    /**
     * Renders setup screen branching by [DeviceLifecycleState].
     */
    @Composable
    override fun SetupScreen(
        onDriverReset: () -> Unit,
    ) {
        val currentState by lifecycleState.collectAsStateWithLifecycle()
        var showUnbindDialog by remember { mutableStateOf(false) }

        when (val state = currentState) {
            is DeviceLifecycleState.DeviceBound -> {
                FramedCard(
                    modifier = Modifier.fillMaxWidth(),
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
                            text = "Der Sample Glucose-Sensor ist zurzeit als aktive Glukosequelle eingerichtet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        NormalButton(
                            onClick = { showUnbindDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                        ) {
                            Text("Bindung verwerfen")
                        }
                    }
                }
            }
            else -> {
                FramedCard(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Sample Glucose Source Setup",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Bestätige das Einrichten des simulierten Sample-Glukosesensors.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        PrimaryButton(
                            onClick = {
                                val deviceId = "sample-cgm-sensor-01"
                                val deviceName = "Sample Glucose Sensor 01"
                                pluginContext?.preferences?.let { prefs ->
                                    scope.launch {
                                        prefs.putString(KEY_DEVICE_ID, deviceId)
                                        prefs.putString(KEY_DEVICE_NAME, deviceName)
                                    }
                                }
                                bindDevice(deviceId, deviceName)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Einrichten")
                        }
                    }
                }
            }
        }

        if (showUnbindDialog) {
            AppAlertDialog(
                onDismissRequest = { showUnbindDialog = false },
                title = { Text(stringResource(R.string.sample_cgm_unbind_dialog_title)) },
                text = { Text(stringResource(R.string.sample_cgm_unbind_dialog_message)) },
                confirmButton = {
                    PrimaryButton(
                        onClick = {
                            showUnbindDialog = false
                            scope.launch { unbindDevice() }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text(stringResource(R.string.sample_cgm_unbind_dialog_confirm))
                    }
                },
                dismissButton = {
                    NormalTextButton(onClick = { showUnbindDialog = false }) {
                        Text(stringResource(de.dh.daps.common.R.string.action_cancel))
                    }
                }
            )
        }
    }

    override fun getGlucoseSourceControlSection(glucoseSource: GlucoseSource): (@Composable () -> Unit) = {
        FramedCard(
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "This content is provided by the Sample Glucose Source.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Version: 1.0.0-sample",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }

    override fun getSettingsScreen(glucoseSource: GlucoseSource, onClose: () -> Unit): (@Composable () -> Unit) = {
        SampleGlucoseSettingsContent(onClose = onClose)
    }

    companion object {
        const val DRIVER_ID = "de.dh.daps.plugin.cgm.sample"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_DEVICE_NAME = "device_name"
    }
}

@Composable
private fun SampleGlucoseSettingsContent(
    onClose: () -> Unit
) {
    var noiseSimulation by remember { mutableStateOf(false) }
    var highFrequencyMode by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Sample Glukose-Quelle Einstellungen",
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
                    text = "Rausch-Simulation",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Zufällige Schwankungen in Glukosewerten simulieren",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = noiseSimulation,
                onCheckedChange = { noiseSimulation = it }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Hohe Messfrequenz",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Messwerte jede Minute statt alle 5 Minuten generieren",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = highFrequencyMode,
                onCheckedChange = { highFrequencyMode = it }
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