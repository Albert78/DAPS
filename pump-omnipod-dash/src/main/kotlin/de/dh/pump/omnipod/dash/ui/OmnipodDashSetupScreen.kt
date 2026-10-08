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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.dh.daps.common.model.InsulinPump
import de.dh.daps.common.model.PumpConnectionDescriptor
import de.dh.pump.omnipod.dash.OmnipodDashInsulinPumpDriver
import de.dh.pump.omnipod.dash.OmnipodDashPump
import de.dh.pump.omnipod.dash.R
import de.dh.pump.omnipod.protocol.ble.OmnipodDashBleManager
import de.dh.pump.omnipod.protocol.state.OmnipodDashPodStateManagerImpl

enum class ActivationStep(val title: String, val description: String) {
    FILL_AND_PRIME(
        "Step 1: Fill Pod & Prime",
        "Fill the Pod with at least 85 Units of U-100 insulin until you hear 2 beeps. Place the Pod next to your device and tap 'Prime'."
    ),
    APPLY_AND_INSERT(
        "Step 2: Apply & Insert Cannula",
        "Remove the clear needle cap. Apply the Pod adhesive to clean, dry skin. Tap 'Insert Cannula' to insert automatically."
    ),
    CONFIRM_AND_ACTIVATE(
        "Step 3: Confirm & Activate",
        "Verify cannula insertion through the transparent window. Tap 'Activate' to start basal insulin delivery."
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
                text = currentStep.title,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = currentStep.description,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                if (currentStepIndex > 0) {
                    OutlinedButton(
                        onClick = { currentStepIndex-- },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Back")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Button(
                    onClick = {
                        if (currentStepIndex < steps.size - 1) {
                            currentStepIndex++
                        } else {
                            val context = driver.getContext()?.appContext
                            if (context != null) {
                                val podStateManager = OmnipodDashPodStateManagerImpl(driver.getContext()?.preferences)
                                podStateManager.activatedAtTimestampMs = System.currentTimeMillis()
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
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        when (currentStep) {
                            ActivationStep.FILL_AND_PRIME -> "Prime Pod"
                            ActivationStep.APPLY_AND_INSERT -> "Insert Cannula"
                            ActivationStep.CONFIRM_AND_ACTIVATE -> "Activate Pod"
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel Setup")
            }
        }
    }
}