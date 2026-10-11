package de.dh.pump.omnipod.dash.ui

import android.app.Application
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Router
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.dh.daps.common.model.DeviceLifecycleState
import de.dh.daps.common.R as CommonR
import de.dh.daps.common.ui.UiText
import de.dh.daps.ui.common.composables.FramedCard
import de.dh.daps.ui.common.composables.NormalButton
import de.dh.daps.ui.common.composables.NormalTextButton
import de.dh.daps.ui.common.composables.PrimaryButton
import de.dh.daps.ui.common.composables.WarningBanner
import de.dh.daps.ui.common.theme.AppPreview
import de.dh.pump.omnipod.dash.OmnipodDashInsulinPumpDriver
import de.dh.pump.omnipod.dash.R

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
    val isActivating by viewModel.isActivating.collectAsStateWithLifecycle()
    val activationError by viewModel.activationError.collectAsStateWithLifecycle()

    when (val state = lifecycleState) {
        is DeviceLifecycleState.DeviceBound -> {
            OmnipodDashBoundContent(
                displayName = state.displayName.asString(),
                deviceId = state.deviceId,
                onUnbind = {
                    viewModel.unbind(driver)
                    onDriverReset()
                },
                modifier = modifier
            )
        }
        else -> {
            LaunchedEffect(Unit) {
                driver.startSetup()
                viewModel.loadSavedState(driver)
            }

            OmnipodDashSetupContent(
                currentStepIndex = currentStepIndex,
                isActivating = isActivating,
                activationError = activationError,
                onNextStep = { viewModel.nextStep(driver) },
                onCancel = {
                    driver.cancelSetup()
                    onDriverReset()
                },
                modifier = modifier
            )
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

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        FramedCard(
            modifier = Modifier.fillMaxWidth()
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
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
        AlertDialog(
            onDismissRequest = { showUnbindDialog = false },
            title = { Text(stringResource(R.string.omnipod_dash_unbind_dialog_title)) },
            text = { Text(stringResource(R.string.omnipod_dash_unbind_dialog_message)) },
            confirmButton = {
                PrimaryButton(
                    onClick = {
                        showUnbindDialog = false
                        onUnbind()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(stringResource(R.string.omnipod_dash_unbind_dialog_confirm))
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

/**
 * Stateless presentation composable for the Omnipod Dash setup screen.
 */
@Composable
fun OmnipodDashSetupContent(
    currentStepIndex: Int,
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
                        text = "Schritt ${currentStepIndex + 1} von ${steps.size}",
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

@Preview(showBackground = true)
@Composable
private fun OmnipodDashBoundContentPreview() {
    AppPreview {
        OmnipodDashBoundContent(
            displayName = "Omnipod Dash Pod",
            deviceId = "POD-DASH-1234",
            onUnbind = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OmnipodDashSetupContentStep1Preview() {
    AppPreview {
        OmnipodDashSetupContent(
            currentStepIndex = 0,
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