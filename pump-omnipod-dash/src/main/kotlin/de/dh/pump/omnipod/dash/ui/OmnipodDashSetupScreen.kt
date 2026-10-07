package de.dh.pump.omnipod.dash.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

@Composable
fun OmnipodDashSetupScreen(
    driver: OmnipodDashInsulinPumpDriver,
    onConnected: (InsulinPump, PumpConnectionDescriptor) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
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
            Text(
                text = "Pod Activation & Setup Wizard. Fill insulin, prime pod, apply and insert cannula.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    val context = driver.getContext()?.appContext
                    if (context != null) {
                        val podStateManager = OmnipodDashPodStateManagerImpl(driver.getContext()?.preferences)
                        val bleManager = OmnipodDashBleManager(context, podStateManager)
                        val pump = OmnipodDashPump(podStateManager, bleManager)
                        val descriptor = PumpConnectionDescriptor(
                            driverId = OmnipodDashInsulinPumpDriver.DRIVER_ID,
                            deviceId = "POD-DASH-DEMO",
                            displayName = "Omnipod Dash Pod"
                        )
                        onConnected(pump, descriptor)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Confirm Pod Activation")
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel")
            }
        }
    }
}