package de.dh.daps.plugin.glucose.receiver.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Badge
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.dh.daps.common.device.state.DeviceLifecycleState
import de.dh.daps.plugin.glucose.receiver.ExternalSourceType
import de.dh.daps.plugin.glucose.receiver.R
import de.dh.daps.ui.common.composables.AppAlertDialog
import de.dh.daps.ui.common.composables.FramedCard
import de.dh.daps.ui.common.composables.NormalButton
import de.dh.daps.ui.common.composables.NormalTextButton
import de.dh.daps.ui.common.composables.PrimaryButton
import de.dh.daps.common.R as CommonR

@Composable
fun ReceiverGlucoseSetupScreen(
    lifecycleState: DeviceLifecycleState,
    externalSourceType: ExternalSourceType,
    onSelectSourceType: (ExternalSourceType) -> Unit,
    onUnbindDevice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showUnbindDialog by remember { mutableStateOf(false) }

    when (lifecycleState) {
        is DeviceLifecycleState.DeviceBound -> {
            FramedCard(
                modifier = modifier.fillMaxWidth(),
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
                                text = lifecycleState.displayName.asString(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(externalSourceType.displayNameRes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Text(
                                text = stringResource(R.string.receiver_status_bound),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    HorizontalDivider()

                    Text(
                        text = stringResource(R.string.receiver_bound_description),
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
                        Text(stringResource(R.string.receiver_action_unbind))
                    }
                }
            }
        }
        else -> {
            var selectedSourceType by remember { mutableStateOf(externalSourceType) }

            FramedCard(
                modifier = modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.receiver_setup_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.receiver_setup_description),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.source_type_label),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ExternalSourceType.entries.forEach { type ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = (type == selectedSourceType),
                                        onClick = { selectedSourceType = type },
                                        role = Role.RadioButton,
                                    )
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = (type == selectedSourceType),
                                    onClick = null,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(type.displayNameRes),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    PrimaryButton(
                        onClick = {
                            onSelectSourceType(selectedSourceType)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.receiver_action_setup))
                    }
                }
            }
        }
    }

    if (showUnbindDialog) {
        AppAlertDialog(
            onDismissRequest = { showUnbindDialog = false },
            title = { Text(stringResource(R.string.receiver_unbind_dialog_title)) },
            content = { Text(stringResource(R.string.receiver_unbind_dialog_message)) },
            confirmButton = {
                PrimaryButton(
                    onClick = {
                        showUnbindDialog = false
                        onUnbindDevice()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(stringResource(R.string.receiver_unbind_dialog_confirm))
                }
            },
            dismissButton = {
                NormalTextButton(onClick = { showUnbindDialog = false }) {
                    Text(stringResource(CommonR.string.action_cancel))
                }
            }
        )
    }
}