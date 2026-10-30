package de.dh.pump.omnipod.dash.ui

import android.app.Application
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Router
import androidx.compose.material3.Badge
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.dh.daps.common.device.state.DeviceLifecycleState
import de.dh.daps.common.ui.UiText
import de.dh.daps.ui.common.composables.AppAlertDialog
import de.dh.daps.ui.common.composables.DialogTitle
import de.dh.daps.ui.common.composables.FramedCard
import de.dh.daps.ui.common.composables.NormalButton
import de.dh.daps.ui.common.composables.NormalTextButton
import de.dh.daps.ui.common.composables.PrimaryButton
import de.dh.daps.ui.common.composables.WarningBanner
import de.dh.daps.ui.common.theme.AppPreview
import de.dh.daps.ui.common.theme.ExtendedTheme
import de.dh.pump.omnipod.dash.OmnipodDashInsulinPumpDriver
import de.dh.pump.omnipod.dash.R
import de.dh.pump.omnipod.protocol.ble.PodBleConnectionState
import de.dh.daps.common.R as CommonR

enum class ActivationStep(
    val title: UiText,
    val description: UiText,
    val actionButtonText: UiText
) {
    FILL_AND_PRIME(
        UiText.StringResource(R.string.omnipod_dash_setup_step1_title),
        UiText.StringResource(R.string.omnipod_dash_setup_step1_description),
        UiText.StringResource(R.string.omnipod_dash_setup_btn_prime_pod)
    ),
    APPLY_AND_INSERT(
        UiText.StringResource(R.string.omnipod_dash_setup_step2_title),
        UiText.StringResource(R.string.omnipod_dash_setup_step2_description),
        UiText.StringResource(R.string.omnipod_dash_setup_btn_insert_cannula)
    ),
    CONFIRM_AND_ACTIVATE(
        UiText.StringResource(R.string.omnipod_dash_setup_step3_title),
        UiText.StringResource(R.string.omnipod_dash_setup_step3_description),
        UiText.StringResource(R.string.omnipod_dash_setup_btn_activate_pod)
    )
}

/**
 * Stateful container for the Omnipod Dash setup screen.
 */
@Composable
fun OmnipodDashSetupScreen(
    driver: OmnipodDashInsulinPumpDriver,
    onDriverReset: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OmnipodDashSetupViewModel = viewModel(
        factory = OmnipodDashSetupViewModel.Companion.Factory(
            LocalContext.current.applicationContext as Application
        )
    )
) {
    val lifecycleState by driver.lifecycleState.collectAsStateWithLifecycle()
    val currentStepIndex by viewModel.currentStepIndex.collectAsStateWithLifecycle()
    val bleConnectionState by viewModel.bleConnectionState.collectAsStateWithLifecycle()
    val isActivating by viewModel.isActivating.collectAsStateWithLifecycle()
    val activationError by viewModel.activationError.collectAsStateWithLifecycle()

    when (val state = lifecycleState) {
        is DeviceLifecycleState.DeviceBound -> {
            OmnipodDashBoundContent(
                displayName = state.displayName.asString(),
                deviceId = state.deviceId,
                onUnbind = {
                    viewModel.unbind(driver) {
                        onDriverReset()
                    }
                },
                modifier = modifier
            )
        }
        else -> {
            var showCancelDialog by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                driver.startSetup()
                viewModel.loadSavedState(driver)
            }

            OmnipodDashSetupContent(
                currentStepIndex = currentStepIndex,
                bleConnectionState = bleConnectionState,
                isActivating = isActivating,
                activationError = activationError,
                onNextStep = { viewModel.nextStep(driver) },
                onCancel = {
                    showCancelDialog = true
                },
                modifier = modifier
            )

            if (showCancelDialog) {
                if (currentStepIndex == 0) {
                    OmnipodDashCancelSetupStep1Dialog(
                        onConfirm = {
                            showCancelDialog = false
                            viewModel.cancelSetup(driver) {
                                onDriverReset()
                            }
                        },
                        onDismiss = { showCancelDialog = false }
                    )
                } else {
                    OmnipodDashCancelSetupStep2Dialog(
                        onConfirm = {
                            showCancelDialog = false
                            viewModel.cancelSetup(driver) {
                                onDriverReset()
                            }
                        },
                        onDismiss = { showCancelDialog = false }
                    )
                }
            }
        }
    }
}

/**
 * Stateless view for an active, bound Omnipod Dash Pod.
 */
@Composable
fun OmnipodDashBoundContent(
    displayName: String,
    deviceId: String?,
    onUnbind: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showUnbindDialog by remember { mutableStateOf(false) }

    FramedCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Router,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (!deviceId.isNullOrEmpty()) {
                            Text(
                                text = deviceId,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Badge(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Text(
                        text = stringResource(R.string.omnipod_dash_bound_badge),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            HorizontalDivider()

            Text(
                text = stringResource(R.string.omnipod_dash_bound_subtitle),
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
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.omnipod_dash_btn_unbind))
            }
        }
    }

    if (showUnbindDialog) {
        OmnipodDashUnbindDialog(
            onConfirm = {
                showUnbindDialog = false
                onUnbind()
            },
            onDismiss = { showUnbindDialog = false }
        )
    }
}

/**
 * Stateless presentation composable for the Omnipod Dash setup screen.
 */
@Composable
fun OmnipodDashSetupContent(
    currentStepIndex: Int,
    bleConnectionState: PodBleConnectionState = PodBleConnectionState.DISCONNECTED,
    isActivating: Boolean,
    activationError: String?,
    onNextStep: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val steps = ActivationStep.entries
    val currentStep = steps.getOrElse(currentStepIndex) { steps[0] }
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Driver header & progress info
        FramedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.omnipod_dash_driver_display_name),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(
                            R.string.omnipod_dash_setup_step_progress,
                            currentStepIndex + 1,
                            steps.size
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                LinearProgressIndicator(
                    progress = { (currentStepIndex + 1).toFloat() / steps.size },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            }
        }

        if (currentStepIndex == 0) {
            BleStatusBanner(bleConnectionState = bleConnectionState)
        }

        if (activationError != null) {
            WarningBanner(
                warningText = activationError,
                onActionClick = {}
            )
        }

        // Active Step Details Card
        FramedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = currentStep.title.asString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = currentStep.description.asString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Navigation Action Buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PrimaryButton(
                onClick = onNextStep,
                enabled = !isActivating,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isActivating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(currentStep.actionButtonText.asString())
                }
            }

            NormalTextButton(
                onClick = onCancel,
                enabled = !isActivating,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.omnipod_dash_setup_btn_cancel))
            }
        }
    }
}

@Composable
fun BleStatusBanner(
    bleConnectionState: PodBleConnectionState,
    modifier: Modifier = Modifier
) {
    val (statusTextRes, containerColor, contentColor, icon) = when (bleConnectionState) {
        PodBleConnectionState.CONNECTED -> Quadruple(
            R.string.omnipod_dash_ble_status_connected,
            ExtendedTheme.semanticColors.good.copy(alpha = 0.15f),
            ExtendedTheme.semanticColors.good,
            Icons.Default.BluetoothConnected
        )
        PodBleConnectionState.CONNECTING -> Quadruple(
            R.string.omnipod_dash_ble_status_connecting,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            Icons.AutoMirrored.Filled.BluetoothSearching
        )
        PodBleConnectionState.DISCONNECTED, PodBleConnectionState.FAILED -> Quadruple(
            R.string.omnipod_dash_ble_status_disconnected,
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            Icons.Default.BluetoothDisabled
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = stringResource(statusTextRes),
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

/**
 * Dialog to confirm cancelling the Omnipod Dash setup process at step 1.
 */
@Composable
fun OmnipodDashCancelSetupStep1Dialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppAlertDialog(
        onDismissRequest = onDismiss,
        titleText = stringResource(R.string.omnipod_dash_cancel_dialog_step1_title),
        confirmButtonText = stringResource(R.string.omnipod_dash_cancel_dialog_step1_confirm),
        dismissButtonText = stringResource(R.string.omnipod_dash_cancel_dialog_continue),
        onConfirm = onConfirm,
        modifier = modifier
    ) {
        Text(
            text = stringResource(R.string.omnipod_dash_cancel_dialog_step1_message),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * Dialog to confirm cancelling the Omnipod Dash setup process at step 2+.
 */
@Composable
fun OmnipodDashCancelSetupStep2Dialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val titleText = stringResource(R.string.omnipod_dash_cancel_dialog_step2_title)
    AppAlertDialog(
        onDismissRequest = onDismiss,
        titleText = titleText,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            DialogTitle(text = titleText)

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.omnipod_dash_cancel_dialog_step2_message),
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PrimaryButton(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.omnipod_dash_cancel_dialog_step2_confirm),
                        textAlign = TextAlign.Center
                    )
                }

                NormalButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.omnipod_dash_cancel_dialog_continue),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Dialog to confirm unbinding the Omnipod Dash Pod.
 */
@Composable
fun OmnipodDashUnbindDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.omnipod_dash_unbind_dialog_title)) },
        content = { Text(stringResource(R.string.omnipod_dash_unbind_dialog_message)) },
        confirmButton = {
            PrimaryButton(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(stringResource(R.string.omnipod_dash_unbind_dialog_confirm))
            }
        },
        dismissButton = {
            NormalTextButton(onClick = onDismiss) {
                Text(stringResource(CommonR.string.action_cancel))
            }
        },
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
private fun BleStatusBannerConnectedPreview() {
    AppPreview {
        BleStatusBanner(bleConnectionState = PodBleConnectionState.CONNECTED)
    }
}

@Preview(showBackground = true)
@Composable
private fun BleStatusBannerConnectingPreview() {
    AppPreview {
        BleStatusBanner(bleConnectionState = PodBleConnectionState.CONNECTING)
    }
}

@Preview(showBackground = true)
@Composable
private fun BleStatusBannerDisconnectedPreview() {
    AppPreview {
        BleStatusBanner(bleConnectionState = PodBleConnectionState.DISCONNECTED)
    }
}

@Preview(showBackground = true)
@Composable
private fun OmnipodDashBoundContentPreview() {
    AppPreview {
        OmnipodDashBoundContent(
            displayName = "Omnipod Dash",
            deviceId = "POD-DASH-1234",
            onUnbind = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OmnipodDashSetupContentStep1DisconnectedPreview() {
    AppPreview {
        OmnipodDashSetupContent(
            currentStepIndex = 0,
            bleConnectionState = PodBleConnectionState.DISCONNECTED,
            isActivating = false,
            activationError = null,
            onNextStep = {},
            onCancel = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OmnipodDashSetupContentStep1ConnectedPreview() {
    AppPreview {
        OmnipodDashSetupContent(
            currentStepIndex = 0,
            bleConnectionState = PodBleConnectionState.CONNECTED,
            isActivating = false,
            activationError = null,
            onNextStep = {},
            onCancel = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OmnipodDashSetupContentStep2Preview() {
    AppPreview {
        OmnipodDashSetupContent(
            currentStepIndex = 1,
            isActivating = false,
            activationError = null,
            onNextStep = {},
            onCancel = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OmnipodDashSetupContentStep3Preview() {
    AppPreview {
        OmnipodDashSetupContent(
            currentStepIndex = 2,
            isActivating = false,
            activationError = null,
            onNextStep = {},
            onCancel = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun OmnipodDashCancelSetupDialogStep1Preview() {
    AppPreview {
        OmnipodDashCancelSetupStep1Dialog(
            onConfirm = {},
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun OmnipodDashCancelSetupDialogStep2Preview() {
    AppPreview {
        OmnipodDashCancelSetupStep2Dialog(
            onConfirm = {},
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun OmnipodDashUnbindDialogPreview() {
    AppPreview {
        OmnipodDashUnbindDialog(
            onConfirm = {},
            onDismiss = {}
        )
    }
}