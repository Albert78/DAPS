package de.dh.daps.ui.screens.setupwizard.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.dh.daps.common.model.InsulinPumpDriver
import de.dh.daps.common.model.PumpConnectionDescriptor
import de.dh.daps.ui.R
import de.dh.daps.ui.screens.pumpsetup.PumpSetupContent
import de.dh.daps.ui.screens.pumpsetup.PumpSetupUiState

@Composable
fun PumpStep(
    uiState: PumpSetupUiState,
    onSelectDriver: (InsulinPumpDriver?) -> Unit,
    onConnectPump: (PumpConnectionDescriptor, onSuccess: () -> Unit) -> Unit,
    onClearError: () -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().padding(bottom = 72.dp)) {
            PumpSetupContent(
                uiState = uiState,
                onNavigateUp = onBack,
                onSelectDriver = onSelectDriver,
                onConnectPump = { descriptor ->
                    onConnectPump(descriptor) { onNext() }
                },
                onClearError = onClearError
            )
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(onClick = onBack) {
                Text(stringResource(R.string.setup_wizard_btn_back))
            }
            Button(onClick = onNext) {
                Text(
                    if (uiState.activePumpDescriptor != null)
                        stringResource(R.string.setup_wizard_btn_next)
                    else
                        stringResource(R.string.setup_wizard_btn_skip)
                )
            }
        }
    }
}