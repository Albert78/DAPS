package de.dh.daps.plugin.simbody

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.dh.daps.common.model.DeviceLifecycleState
import de.dh.daps.common.model.GlucoseSource
import de.dh.daps.common.model.GlucoseSourceDriver
import de.dh.daps.common.model.Plugin
import de.dh.daps.common.ui.UiText
import de.dh.daps.ui.common.composables.FramedCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Driver implementation for the SimBody virtual CGM glucose source.
 * Connects the app to the simulated metabolic engine's blood glucose output.
 */
class SimBodyGlucoseSourceDriver(
    private val simBodyPlugin: SimBodyPlugin,
) : GlucoseSourceDriver, Plugin {
    override val driverId: String = DRIVER_ID
    override val displayName: UiText = UiText.StringResource(R.string.sim_body_cgm_driver_display_name)
    override val pluginId: String = DRIVER_ID
    override val neededPermissions: Collection<String> = emptyList()

    private val _lifecycleState = MutableStateFlow<DeviceLifecycleState>(DeviceLifecycleState.Unconfigured)
    override val lifecycleState: StateFlow<DeviceLifecycleState> = _lifecycleState.asStateFlow()

    /**
     * Renders the setup screen for connecting to the virtual human body simulation CGM.
     */
    @Composable
    override fun SetupScreen(
        onConnected: (GlucoseSource) -> Unit,
        onCancel: () -> Unit,
    ) {
        val sensorDisplayName = stringResource(R.string.sim_body_cgm_sensor_display_name)

        FramedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.sim_body_cgm_setup_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.sim_body_cgm_setup_description),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.btn_cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val source = simBodyPlugin.getGlucoseSource()
                            _lifecycleState.value = DeviceLifecycleState.Configured(
                                deviceId = "simbody-virtual-cgm-01",
                                displayName = sensorDisplayName
                            )
                            onConnected(source)
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.btn_connect))
                    }
                }
            }
        }
    }

    /**
     * Re-connects to the virtual human body simulation CGM using stored driver settings.
     */
    override suspend fun connect(): Result<GlucoseSource> {
        _lifecycleState.value = DeviceLifecycleState.Configured(
            deviceId = "simbody-virtual-cgm-01",
            displayName = "SimBody Virtual CGM"
        )
        return Result.success(simBodyPlugin.getGlucoseSource())
    }

    companion object {
        const val DRIVER_ID = "de.dh.daps.plugin.simbody.cgm"
    }
}