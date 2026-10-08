package de.dh.pump.omnipod.dash.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.dh.daps.common.model.InsulinPump
import de.dh.daps.common.model.PumpConnectionDescriptor
import de.dh.daps.common.ui.UiText
import de.dh.pump.omnipod.dash.OmnipodDashInsulinPumpDriver
import de.dh.pump.omnipod.dash.OmnipodDashPump
import de.dh.pump.omnipod.dash.R
import de.dh.pump.omnipod.protocol.ble.OmnipodDashBleManager
import de.dh.pump.omnipod.protocol.definition.ActivationProgress
import de.dh.pump.omnipod.protocol.state.OmnipodDashPodStateManagerImpl
import kotlinx.coroutines.launch

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

@Composable
fun OmnipodDashSetupScreen(
    driver: OmnipodDashInsulinPumpDriver,
    onConnected: (InsulinPump, PumpConnectionDescriptor) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentStepIndex by remember { mutableIntStateOf(0) }
    val steps = ActivationStep.entries
    val currentStep = steps[currentStepIndex]
    val scope = rememberCoroutineScope()

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
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))

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
                        onClick = { currentStepIndex-- },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.omnipod_dash_setup_btn_back))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Button(
                    onClick = {
                        val preferences = driver.getContext()?.preferences
                        val podStateManager = OmnipodDashPodStateManagerImpl(preferences)
                        when (currentStep) {
                            ActivationStep.FILL_AND_PRIME -> {
                                podStateManager.activationProgress = ActivationProgress.PRIME_COMPLETED
                                currentStepIndex++
                            }
                            ActivationStep.APPLY_AND_INSERT -> {
                                podStateManager.activationProgress = ActivationProgress.CANNULA_INSERTED
                                currentStepIndex++
                            }
                            ActivationStep.CONFIRM_AND_ACTIVATE -> {
                                val context = driver.getContext()?.appContext
                                if (context != null) {
                                    podStateManager.activatedAtTimestampMs = System.currentTimeMillis()
                                    podStateManager.activationProgress = ActivationProgress.COMPLETED
                                    scope.launch {
                                        podStateManager.saveToPreferences()
                                    }
                                    val bleManager = OmnipodDashBleManager(context, podStateManager)
                                    val pump = OmnipodDashPump(podStateManager, bleManager, context)
                                    val descriptor = PumpConnectionDescriptor(
                                        driverId = OmnipodDashInsulinPumpDriver.DRIVER_ID,
                                        deviceId = podStateManager.bluetoothAddress ?: "POD-DASH-ACTIVE",
                                        displayName = "Omnipod Dash Pod"
                                    )
                                    onConnected(pump, descriptor)
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(currentStep.actionButtonText.asString())
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.omnipod_dash_setup_btn_cancel))
            }
        }
    }
}