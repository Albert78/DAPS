package de.dh.daps.plugin.simbody.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.dh.daps.common.model.DeviceLifecycleState
import de.dh.daps.common.ui.UiText
import de.dh.daps.plugin.simbody.R
import de.dh.daps.plugin.simbody.SimBodyInsulinPumpDriver
import de.dh.daps.ui.common.composables.FramedCard

@Composable
fun SimBodyPumpSetupScreen(
    driver: SimBodyInsulinPumpDriver,
    onNavigateUp: () -> Unit,
) {
    val currentState by driver.lifecycleState.collectAsStateWithLifecycle()

    SimBodyPumpSetupContent(
        lifecycleState = currentState,
        onBind = {
            driver.bindDevice()
            onNavigateUp()
        },
        onUnbind = {
            driver.unbindDevice()
        },
        onNavigateUp = onNavigateUp,
    )
}

@Composable
fun SimBodyPumpSetupContent(
    lifecycleState: DeviceLifecycleState,
    onBind: () -> Unit,
    onUnbind: () -> Unit,
    onNavigateUp: () -> Unit,
) {
    var showUnbindDialog by remember { mutableStateOf(false) }

    when (lifecycleState) {
        is DeviceLifecycleState.DeviceBound -> {
            FramedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = lifecycleState.displayName.asString(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            lifecycleState.deviceId?.let { id ->
                                Text(
                                    text = id,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ) {
                            Text(
                                text = "Gebunden",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }

                    HorizontalDivider()

                    Text(
                        text = "Die virtuelle SimBody-Insulinpumpe ist zurzeit als aktive Pumpe eingerichtet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = { showUnbindDialog = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f),
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
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
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.sim_body_pump_setup_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.sim_body_pump_setup_description),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = onNavigateUp,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.btn_cancel))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onBind,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.btn_connect))
                        }
                    }
                }
            }
        }
    }

    if (showUnbindDialog) {
        AlertDialog(
            onDismissRequest = { showUnbindDialog = false },
            title = { Text(stringResource(R.string.sim_body_pump_unbind_dialog_title)) },
            text = { Text(stringResource(R.string.sim_body_pump_unbind_dialog_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        showUnbindDialog = false
                        onUnbind()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    Text(stringResource(R.string.btn_unbind_confirm))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showUnbindDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            },
        )
    }
}

@Preview
@Composable
private fun SimBodyPumpSetupContentNoDevicePreview() {
    MaterialTheme {
        SimBodyPumpSetupContent(
            lifecycleState = DeviceLifecycleState.NoDevice,
            onBind = {},
            onUnbind = {},
            onNavigateUp = {},
        )
    }
}

@Preview
@Composable
private fun SimBodyPumpSetupContentBoundPreview() {
    MaterialTheme {
        SimBodyPumpSetupContent(
            lifecycleState = DeviceLifecycleState.DeviceBound(
                deviceId = "simbody-virtual-pump-01",
                displayName = UiText.DynamicString("SimBody Virtuelle Insulinpumpe"),
            ),
            onBind = {},
            onUnbind = {},
            onNavigateUp = {},
        )
    }
}