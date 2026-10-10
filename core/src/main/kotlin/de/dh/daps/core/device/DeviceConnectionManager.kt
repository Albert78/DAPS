package de.dh.daps.core.device

import android.util.Log
import de.dh.daps.common.model.DeviceLifecycleState
import de.dh.daps.core.aps.GlucoseSourceDriverManager
import de.dh.daps.core.aps.GlucoseSourceManager
import de.dh.daps.core.pump.PumpDriverManager
import de.dh.daps.core.pump.PumpManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/**
 * Central manager responsible for connecting, disconnecting, and restoring saved hardware connections
 * (glucose source and insulin pump) using their respective driver managers and driver lifecycle states.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DeviceConnectionManager(
    private val glucoseSourceDriverManager: GlucoseSourceDriverManager,
    private val pumpDriverManager: PumpDriverManager,
    private val glucoseSourceManager: GlucoseSourceManager,
    private val pumpManager: PumpManager,
    private val scope: CoroutineScope,
) {
    private var observeJob: Job? = null

    /**
     * Synchronously attaches listeners to active glucose source and pump driver lifecycle flows.
     * When a driver transitions to [DeviceLifecycleState.DeviceBound], it is automatically attached.
     */
    fun startObserving() {
        if (observeJob != null) return

        observeJob = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            launch(start = CoroutineStart.UNDISPATCHED) {
                glucoseSourceDriverManager.activeDriver.flatMapLatest { driver ->
                    driver?.lifecycleState ?: flowOf(DeviceLifecycleState.NoDevice)
                }.collect { state ->
                    handleGlucoseSourceState(state)
                }
            }

            launch(start = CoroutineStart.UNDISPATCHED) {
                pumpDriverManager.activeDriver.flatMapLatest { driver ->
                    driver?.lifecycleState ?: flowOf(DeviceLifecycleState.NoDevice)
                }.collect { state ->
                    handlePumpState(state)
                }
            }
        }
    }

    private suspend fun handleGlucoseSourceState(state: DeviceLifecycleState) {
        when (state) {
            is DeviceLifecycleState.DeviceBound -> {
                val activeDriver = glucoseSourceDriverManager.getActiveDriver()
                if (activeDriver != null) {
                    glucoseSourceDriverManager.attachToGlucoseSource(activeDriver.driverId)
                        .onSuccess { glucoseSource ->
                            glucoseSourceManager.glucoseSource = glucoseSource
                        }
                        .onFailure { error ->
                            Log.e(TAG, "Failed to attach glucose source '${activeDriver.driverId}': ${error.message}", error)
                        }
                }
            }
            is DeviceLifecycleState.NoDevice -> {
                glucoseSourceManager.glucoseSource = null
            }
            is DeviceLifecycleState.SetupInProgress -> {
                // Skip auto-connect until setup finishes
            }
        }
    }

    private suspend fun handlePumpState(state: DeviceLifecycleState) {
        when (state) {
            is DeviceLifecycleState.DeviceBound -> {
                val activeDriver = pumpDriverManager.getActiveDriver()
                if (activeDriver != null) {
                    pumpDriverManager.attachToPump(activeDriver.driverId)
                        .onSuccess { insulinPump ->
                            pumpManager.insulinPump = insulinPump
                        }
                        .onFailure { error ->
                            Log.e(TAG, "Failed to attach insulin pump '${activeDriver.driverId}': ${error.message}", error)
                        }
                }
            }
            is DeviceLifecycleState.NoDevice -> {
                pumpManager.insulinPump = null
            }
            is DeviceLifecycleState.SetupInProgress -> {
                // Skip auto-connect until setup finishes
            }
        }
    }

    /**
     * Initializes state across all registered drivers by invoking `initState()` on each driver.
     */
    suspend fun initDriverStates() {
        glucoseSourceDriverManager.drivers.forEach { driver ->
            try {
                driver.initState()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize state for glucose driver '${driver.driverId}'", e)
            }
        }
        pumpDriverManager.drivers.forEach { driver ->
            try {
                driver.initState()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize state for pump driver '${driver.driverId}'", e)
            }
        }
    }

    /**
     * Attempts to re-establish connections to the active glucose source and insulin pump.
     * Ensures driver observers are attached synchronously before initializing driver states.
     */
    suspend fun restoreConnections() {
        Log.d(TAG, "Restoring saved device connections...")
        startObserving()
        initDriverStates()
    }

    /**
     * Unbinds the active glucose source.
     */
    fun unbindGlucoseSource() {
        Log.d(TAG, "Unbinding active glucose source")
        glucoseSourceManager.glucoseSource = null
    }

    /**
     * Unbinds the active insulin pump.
     */
    fun unbindPump() {
        Log.d(TAG, "Unbinding active insulin pump")
        pumpManager.insulinPump = null
    }

    companion object {
        private val TAG = DeviceConnectionManager::class.simpleName
    }
}