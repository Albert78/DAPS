package de.dh.pump.omnipod.dash.ui

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import de.dh.daps.common.model.InsulinPump
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
    onConnected: (InsulinPump) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OmnipodDashSetupViewModel = viewModel(
        factory = OmnipodDashSetupViewModel.Companion.Factory(
            LocalContext.current.applicationContext as Application
        )
    )
) {
    val currentStepIndex by viewModel.currentStepIndex.collectAsStateWithLifecycle()
    val isActivating by viewModel.isActivating.collectAsStateWithLifecycle()
    val activationError by viewModel.activationError.collectAsStateWithLifecycle()

    OmnipodDashSetupContent(
        currentStepIndex = currentStepIndex,
        isActivating = isActivating,
        activationError = activationError,
        onNextStep = { viewModel.nextStep(driver, onConnected) },
        onPreviousStep = { viewModel.previousStep() },
        onCancel = onCancel,
        modifier = modifier
    )
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
    onPreviousStep: () -> Unit,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (currentStepIndex > 0) {
                    NormalButton(
                        onClick = onPreviousStep,
                        enabled = !isActivating,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.omnipod_dash_setup_btn_back))
                    }
                }

                PrimaryButton(
                    onClick = onNextStep,
                    enabled = !isActivating,
                    modifier = Modifier.weight(1f)
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
private fun OmnipodDashSetupContentStep1Preview() {
    AppPreview {
        OmnipodDashSetupContent(
            currentStepIndex = 0,
            isActivating = false,
            activationError = null,
            onNextStep = {},
            onPreviousStep = {},
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
            onPreviousStep = {},
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
            onPreviousStep = {},
            onCancel = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OmnipodDashSetupContentActivatingPreview() {
    AppPreview {
        OmnipodDashSetupContent(
            currentStepIndex = 2,
            isActivating = true,
            activationError = null,
            onNextStep = {},
            onPreviousStep = {},
            onCancel = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OmnipodDashSetupContentErrorPreview() {
    AppPreview {
        OmnipodDashSetupContent(
            currentStepIndex = 2,
            isActivating = false,
            activationError = "Aktivierung fehlgeschlagen. Bitte erneut versuchen.",
            onNextStep = {},
            onPreviousStep = {},
            onCancel = {}
        )
    }
}