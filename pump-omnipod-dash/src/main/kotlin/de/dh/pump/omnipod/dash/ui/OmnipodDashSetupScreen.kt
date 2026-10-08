package de.dh.pump.omnipod.dash.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.dh.daps.common.model.InsulinPump
import de.dh.daps.common.model.PumpConnectionDescriptor
import de.dh.daps.common.ui.UiText
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
    onConnected: (InsulinPump, PumpConnectionDescriptor) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OmnipodDashSetupViewModel = viewModel()
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

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.omnipod_dash_driver_display_name),
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { (currentStepIndex + 1).toFloat() / steps.size },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (activationError != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        text = activationError,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Text(
                text = currentStep.title.asString(),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = currentStep.description.asString(),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                if (currentStepIndex > 0) {
                    OutlinedButton(
                        onClick = onPreviousStep,
                        enabled = !isActivating,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.omnipod_dash_setup_btn_back))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Button(
                    onClick = onNextStep,
                    enabled = !isActivating,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isActivating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(currentStep.actionButtonText.asString())
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
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
    MaterialTheme {
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
    MaterialTheme {
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
    MaterialTheme {
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
    MaterialTheme {
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
    MaterialTheme {
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