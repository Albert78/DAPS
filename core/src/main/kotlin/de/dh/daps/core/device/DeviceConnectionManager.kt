package de.dh.daps.core.device

import android.util.Log
import de.dh.daps.common.model.DeviceLifecycleState
import de.dh.daps.common.model.GlucoseSource
import de.dh.daps.common.model.InsulinPump
import de.dh.daps.core.aps.GlucoseSourceDriverManager
import de.dh.daps.core.aps.GlucoseSourceManager
import de.dh.daps.core.pump.PumpDriverManager
import de.dh.daps.core.pump.PumpManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/**
 * Container holding the connection results when restoring device connections on application startup.
 */
data class DeviceConnectionResult(
    val glucoseSourceResult: Result<GlucoseSource>?,
    val pumpResult: Result<InsulinPump>?
)

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
    init {
        observeDrivers()
    }

    private fun observeDrivers() {
        scope.launch {
            glucoseSourceDriverManager.activeDriver.flatMapLatest { driver ->
                driver?.lifecycleState ?: flowOf(DeviceLifecycleState.NoDevice)
            }.collect { state ->
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
        }

        scope.launch {
            pumpDriverManager.activeDriver.flatMapLatest { driver ->
                driver?.lifecycleState ?: flowOf(DeviceLifecycleState.NoDevice)
            }.collect { state ->
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
        }
    }

    /**
     * Attempts to re-establish connections to the active glucose source and insulin pump.
     * Should be called during application startup.
     */
    suspend fun restoreConnections(): DeviceConnectionResult {
        Log.d(TAG, "Restoring saved device connections...")

        val activeGlucoseDriver = glucoseSourceDriverManager.getActiveDriver()
        val glucoseSourceResult = if (activeGlucoseDriver != null) {
            val state = activeGlucoseDriver.lifecycleState.value
            Log.d(TAG, "Active glucose source driver '${activeGlucoseDriver.driverId}' state: $state")
            when (state) {
                is DeviceLifecycleState.DeviceBound -> {
                    glucoseSourceDriverManager.attachToGlucoseSource(activeGlucoseDriver.driverId).onSuccess { glucoseSource ->
                        glucoseSourceManager.glucoseSource = glucoseSource
                    }.onFailure { error ->
                        Log.e(TAG, "Failed to attach glucose source driver '${activeGlucoseDriver.driverId}': ${error.message}", error)
                    }
                }
                is DeviceLifecycleState.SetupInProgress -> {
                    Log.i(TAG, "Glucose source setup in progress for driver '${activeGlucoseDriver.driverId}'. Skipping auto-connect until setup finishes.")
                    null
                }
                is DeviceLifecycleState.NoDevice -> null
            }
        } else {
            Log.d(TAG, "No active glucose source driver found.")
            null
        }

        val activePumpDriver = pumpDriverManager.getActiveDriver()
        val pumpResult = if (activePumpDriver != null) {
            val state = activePumpDriver.lifecycleState.value
            Log.d(TAG, "Active pump driver '${activePumpDriver.driverId}' state: $state")
            when (state) {
                is DeviceLifecycleState.DeviceBound -> {
                    pumpDriverManager.attachToPump(activePumpDriver.driverId).onSuccess { insulinPump ->
                        pumpManager.insulinPump = insulinPump
                    }.onFailure { error ->
                        Log.e(TAG, "Failed to attach insulin pump driver '${activePumpDriver.driverId}': ${error.message}", error)
                    }
                }
                is DeviceLifecycleState.SetupInProgress -> {
                    Log.i(TAG, "Pump setup in progress for driver '${activePumpDriver.driverId}'. Skipping auto-connect until setup finishes.")
                    null
                }
                is DeviceLifecycleState.NoDevice -> null
            }
        } else {
            Log.d(TAG, "No active pump driver found.")
            null
        }

        return DeviceConnectionResult(
            glucoseSourceResult = glucoseSourceResult,
            pumpResult = pumpResult
        )
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