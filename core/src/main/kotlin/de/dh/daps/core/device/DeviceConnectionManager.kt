package de.dh.daps.core.device

import android.util.Log
import de.dh.daps.common.model.DeviceLifecycleState
import de.dh.daps.common.model.GlucoseSource
import de.dh.daps.common.model.InsulinPump
import de.dh.daps.core.aps.GlucoseSourceDriverManager
import de.dh.daps.core.aps.GlucoseSourceManager
import de.dh.daps.core.pump.PumpDriverManager
import de.dh.daps.core.pump.PumpManager
import de.dh.daps.core.repository.DeviceManagementRepository

/**
 * Container holding the connection results when restoring device connections on application startup.
 */
data class DeviceConnectionResult(
    val glucoseSourceResult: Result<GlucoseSource>?,
    val pumpResult: Result<InsulinPump>?
)

/**
 * Central manager responsible for connecting, disconnecting, and restoring saved hardware connections
 * (glucose source and insulin pump) using their respective driver managers, active driver IDs, and driver lifecycle states.
 */
class DeviceConnectionManager(
    private val deviceManagementRepository: DeviceManagementRepository,
    private val glucoseSourceDriverManager: GlucoseSourceDriverManager,
    private val pumpDriverManager: PumpDriverManager,
    private val glucoseSourceManager: GlucoseSourceManager,
    private val pumpManager: PumpManager
) {
    /**
     * Attempts to re-establish connections to the active glucose source and insulin pump.
     * Should be called during application startup.
     */
    suspend fun restoreConnections(): DeviceConnectionResult {
        Log.d(TAG, "Restoring saved device connections...")

        val activeGlucoseDriverId = deviceManagementRepository.getActiveGlucoseSourceDriverId()

        val glucoseSourceResult = if (activeGlucoseDriverId != null) {
            val driver = glucoseSourceDriverManager.getDriver(activeGlucoseDriverId)
            if (driver != null) {
                val state = driver.lifecycleState.value
                Log.d(TAG, "Active glucose source driver '$activeGlucoseDriverId' state: $state")
                when (state) {
                    is DeviceLifecycleState.DeviceBound -> {
                        glucoseSourceDriverManager.connect(activeGlucoseDriverId).onSuccess { glucoseSource ->
                            glucoseSourceManager.glucoseSource = glucoseSource
                        }.onFailure { error ->
                            Log.e(TAG, "Failed to reconnect glucose source driver '$activeGlucoseDriverId': ${error.message}", error)
                        }
                    }
                    is DeviceLifecycleState.SetupInProgress -> {
                        Log.i(TAG, "Glucose source setup in progress for driver '$activeGlucoseDriverId'. Skipping auto-connect until setup finishes.")
                        null
                    }
                    is DeviceLifecycleState.NoDevice -> {
                        Log.d(TAG, "Glucose source driver '$activeGlucoseDriverId' is unconfigured.")
                        null
                    }
                }
            } else {
                Log.w(TAG, "No driver registered for active glucose driver ID '$activeGlucoseDriverId'")
                null
            }
        } else {
            Log.d(TAG, "No active glucose source driver ID found.")
            null
        }

        val activePumpDriverId = deviceManagementRepository.getActivePumpDriverId()

        val pumpResult = if (activePumpDriverId != null) {
            val driver = pumpDriverManager.getDriver(activePumpDriverId)
            if (driver != null) {
                val state = driver.lifecycleState.value
                Log.d(TAG, "Active pump driver '$activePumpDriverId' state: $state")
                when (state) {
                    is DeviceLifecycleState.DeviceBound -> {
                        pumpDriverManager.connect(activePumpDriverId).onSuccess { insulinPump ->
                            pumpManager.insulinPump = insulinPump
                        }.onFailure { error ->
                            Log.e(TAG, "Failed to reconnect insulin pump driver '$activePumpDriverId': ${error.message}", error)
                        }
                    }
                    is DeviceLifecycleState.SetupInProgress -> {
                        Log.i(TAG, "Pump setup in progress for driver '$activePumpDriverId'. Skipping auto-connect until setup finishes.")
                        null
                    }
                    is DeviceLifecycleState.NoDevice -> {
                        Log.d(TAG, "Pump driver '$activePumpDriverId' is unconfigured.")
                        null
                    }
                }
            } else {
                Log.w(TAG, "No driver registered for active pump driver ID '$activePumpDriverId'")
                null
            }
        } else {
            Log.d(TAG, "No active pump driver ID found.")
            null
        }

        return DeviceConnectionResult(
            glucoseSourceResult = glucoseSourceResult,
            pumpResult = pumpResult
        )
    }

    /**
     * Connects to a glucose source using [driverId], updates [GlucoseSourceManager],
     * and persists the active driver ID upon successful connection.
     */
    suspend fun connectGlucoseSource(driverId: String): Result<GlucoseSource> {
        Log.d(TAG, "Connecting to glucose source driver: $driverId")
        return glucoseSourceDriverManager.connect(driverId).onSuccess { glucoseSource ->
            setConnectedGlucoseSource(driverId, glucoseSource)
        }
    }

    /**
     * Updates the active [GlucoseSourceManager] with a connected [glucoseSource]
     * and persists the active driver ID.
     */
    suspend fun setConnectedGlucoseSource(driverId: String, glucoseSource: GlucoseSource) {
        glucoseSourceManager.glucoseSource = glucoseSource
        deviceManagementRepository.setActiveGlucoseSourceDriverId(driverId)
    }

    /**
     * Connects to an insulin pump using [driverId], updates [PumpManager],
     * and persists the active driver ID upon successful connection.
     */
    suspend fun connectPump(driverId: String): Result<InsulinPump> {
        Log.d(TAG, "Connecting to insulin pump driver: $driverId")
        return pumpDriverManager.connect(driverId).onSuccess { insulinPump ->
            setConnectedPump(driverId, insulinPump)
        }
    }

    /**
     * Updates the active [PumpManager] with a connected [insulinPump]
     * and persists the active driver ID.
     */
    suspend fun setConnectedPump(driverId: String, insulinPump: InsulinPump) {
        pumpManager.insulinPump = insulinPump
        deviceManagementRepository.setActivePumpDriverId(driverId)
    }

    /**
     * Disconnects the active glucose source and clears the persisted driver ID.
     */
    suspend fun disconnectGlucoseSource() {
        Log.d(TAG, "Disconnecting active glucose source")
        glucoseSourceManager.glucoseSource = null
        deviceManagementRepository.setActiveGlucoseSourceDriverId(null)
    }

    /**
     * Disconnects the active insulin pump and clears the persisted driver ID.
     */
    suspend fun disconnectPump() {
        Log.d(TAG, "Disconnecting active insulin pump")
        pumpManager.insulinPump = null
        deviceManagementRepository.setActivePumpDriverId(null)
    }

    companion object {
        private val TAG = DeviceConnectionManager::class.simpleName
    }
}