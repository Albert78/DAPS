package de.dh.pump.danai.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.dh.daps.common.device.state.DeviceLifecycleState
import de.dh.pump.danai.DanaIInsulinPumpDriver

/**
 * Stateful setup container for Sooil Dana-i insulin pumps.
 *
 * Top-level state-router that delegates to:
 * - [DanaIBoundScreen] when in [DeviceLifecycleState.DeviceBound]
 * - [DanaISetupInsulinParametersScreen] when in [DeviceLifecycleState.SetupInProgress]
 * - [DanaISetupScanScreen] during setup/scanning (e.g. [DeviceLifecycleState.NoDevice]).
 */
@Composable
fun DanaISetupScreen(
    driver: DanaIInsulinPumpDriver,
    onDriverReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lifecycleState by driver.lifecycleState.collectAsStateWithLifecycle()

    when (val state = lifecycleState) {
        is DeviceLifecycleState.DeviceBound -> {
            DanaIBoundScreen(
                driver = driver,
                state = state,
                onDriverReset = onDriverReset,
                modifier = modifier
            )
        }
        is DeviceLifecycleState.SetupInProgress -> {
            DanaISetupInsulinParametersScreen(
                driver = driver,
                modifier = modifier
            )
        }
        else -> {
            DanaISetupScanScreen(
                driver = driver,
                modifier = modifier
            )
        }
    }
}