package de.dh.pump.danai.ui

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.dh.daps.common.model.DeviceLifecycleState
import de.dh.daps.common.model.data.Timestamp
import de.dh.pump.danai.DanaIInsulinPumpDriver
import de.dh.pump.danai.core.DanaIPumpAdapter
import kotlinx.coroutines.launch

/**
 * Stateful setup container for Sooil Dana-i insulin pumps.
 *
 * Manages [DanaISetupViewModel] state and lifecycle, branching between [DeviceLifecycleState.DeviceBound]
 * and the setup/scan workflow, displaying [DanaIMaintenanceScreen] when in maintenance mode.
 */
@Composable
fun DanaISetupScreen(
    driver: DanaIInsulinPumpDriver,
    onDriverReset: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DanaISetupViewModel = viewModel(
        factory = DanaISetupViewModel.Companion.Factory(
            LocalContext.current.applicationContext as Application
        )
    )
) {
    val lifecycleState by driver.lifecycleState.collectAsStateWithLifecycle()
    val scanResults by viewModel.scanResults.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val scanError by viewModel.scanError.collectAsState()
    val pairingAddress by viewModel.pairingAddress.collectAsState()
    val pairError by viewModel.pairError.collectAsState()

    when (val state = lifecycleState) {
        is DeviceLifecycleState.DeviceBound -> {
            val userOptions by viewModel.userOptions.collectAsState()
            val isLoadingOptions by viewModel.isLoadingOptions.collectAsState()
            val isSavingOptions by viewModel.isSavingOptions.collectAsState()
            val optionsError by viewModel.optionsError.collectAsState()

            val activePump by driver.activePump.collectAsStateWithLifecycle()
            val pumpAdapter = activePump as? DanaIPumpAdapter
            val isMaintenanceActive = pumpAdapter?.isMaintenanceMode?.collectAsStateWithLifecycle()?.value ?: false

            if (isMaintenanceActive) {
                val coroutineScope = rememberCoroutineScope()
                DanaIMaintenanceScreen(
                    onEndMaintenance = { resetCatheterWear ->
                        coroutineScope.launch {
                            if (resetCatheterWear) {
                                pumpAdapter.saveCatheterLastChanged(Timestamp.now())
                            }
                            pumpAdapter.setMaintenanceMode(false)
                        }
                    },
                    modifier = modifier
                )
            } else {
                DanaIBoundContent(
                    displayName = state.displayName.asString(),
                    deviceId = state.deviceId,
                    userOptions = userOptions,
                    isLoadingOptions = isLoadingOptions,
                    isSavingOptions = isSavingOptions,
                    optionsError = optionsError,
                    onLoadOptions = { viewModel.loadUserOptions(driver) },
                    onStartMaintenance = {
                        pumpAdapter?.setMaintenanceMode(true)
                    },
                    onSaveAndUnbind = { options ->
                        viewModel.saveAndUnbind(driver, options) {
                            onDriverReset()
                        }
                    },
                    onUnbindOnly = {
                        viewModel.unbind(driver) {
                            onDriverReset()
                        }
                    },
                    modifier = modifier
                )
            }
        }
        else -> {
            // Automatically start BLE scan when screen opens in setup/unbound state
            LaunchedEffect(Unit) {
                viewModel.startDiscovery()
            }

            val isStaleBondError by viewModel.isStaleBondError.collectAsState()

            DanaISetupContent(
                scanResults = scanResults,
                isScanning = isScanning,
                scanError = scanError,
                pairingAddress = pairingAddress,
                pairError = pairError,
                isStaleBondError = isStaleBondError,
                onStartDiscovery = { viewModel.startDiscovery() },
                onPairClick = { scanResult ->
                    viewModel.pairAndBind(
                        driver = driver,
                        scanResult = scanResult
                    )
                },
                modifier = modifier
            )
        }
    }
}