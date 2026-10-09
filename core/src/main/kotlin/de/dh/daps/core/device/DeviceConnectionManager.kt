package de.dh.daps.core.device

import android.util.Log
import de.dh.daps.common.model.DeviceLifecycleState
import de.dh.daps.common.model.GlucoseSource
import de.dh.daps.common.model.GlucoseSourceConnectionDescriptor
import de.dh.daps.common.model.InsulinPump
import de.dh.daps.common.model.PumpConnectionDescriptor
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
        val glucoseSourceDescriptor = deviceManagementRepository.getGlucoseSourceDescriptor()
        val glucoseDriverId = activeGlucoseDriverId ?: glucoseSourceDescriptor?.driverId

        val glucoseSourceResult = if (glucoseDriverId != null) {
            val driver = glucoseSourceDriverManager.getDriver(glucoseDriverId)
            if (driver != null) {
                val state = driver.lifecycleState.value
                Log.d(TAG, "Active glucose source driver '$glucoseDriverId' state: $state")
                when (state) {
                    is DeviceLifecycleState.Configured -> {
                        val descriptorToUse = glucoseSourceDescriptor ?: GlucoseSourceConnectionDescriptor(
                            driverId = driver.driverId,
                            sourceId = state.deviceId,
                            displayName = state.displayName,
                            connectionParameters = state.connectionParameters
                        )
                        glucoseSourceDriverManager.connect(descriptorToUse).onSuccess { glucoseSource ->
                            glucoseSourceManager.glucoseSource = glucoseSource
                        }.onFailure { error ->
                            Log.e(TAG, "Failed to reconnect glucose source '${descriptorToUse.displayName}': ${error.message}", error)
                        }
                    }
                    is DeviceLifecycleState.SetupInProgress -> {
                        Log.i(TAG, "Glucose source setup in progress for driver '$glucoseDriverId'. Skipping auto-connect until setup finishes.")
                        null
                    }
                    is DeviceLifecycleState.Unconfigured -> {
                        Log.d(TAG, "Glucose source driver '$glucoseDriverId' is unconfigured.")
                        null
                    }
                }
            } else {
                Log.w(TAG, "No driver registered for active glucose driver ID '$glucoseDriverId'")
                null
            }
        } else {
            Log.d(TAG, "No active glucose source driver ID found.")
            null
        }

        val activePumpDriverId = deviceManagementRepository.getActivePumpDriverId()
        val pumpDescriptor = deviceManagementRepository.getPumpDescriptor()
        val pumpDriverId = activePumpDriverId ?: pumpDescriptor?.driverId

        val pumpResult = if (pumpDriverId != null) {
            val driver = pumpDriverManager.getDriver(pumpDriverId)
            if (driver != null) {
                val state = driver.lifecycleState.value
                Log.d(TAG, "Active pump driver '$pumpDriverId' state: $state")
                when (state) {
                    is DeviceLifecycleState.Configured -> {
                        val descriptorToUse = pumpDescriptor ?: PumpConnectionDescriptor(
                            driverId = driver.driverId,
                            deviceId = state.deviceId,
                            displayName = state.displayName,
                            connectionParameters = state.connectionParameters
                        )
                        pumpDriverManager.connect(descriptorToUse).onSuccess { insulinPump ->
                            pumpManager.insulinPump = insulinPump
                        }.onFailure { error ->
                            Log.e(TAG, "Failed to reconnect insulin pump '${descriptorToUse.displayName}': ${error.message}", error)
                        }
                    }
                    is DeviceLifecycleState.SetupInProgress -> {
                        Log.i(TAG, "Pump setup in progress for driver '$pumpDriverId'. Skipping auto-connect until setup finishes.")
                        null
                    }
                    is DeviceLifecycleState.Unconfigured -> {
                        Log.d(TAG, "Pump driver '$pumpDriverId' is unconfigured.")
                        null
                    }
                }
            } else {
                Log.w(TAG, "No driver registered for active pump driver ID '$pumpDriverId'")
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
     * Connects to a glucose source using the provided [descriptor], updates the active [GlucoseSourceManager],
     * and persists the active driver ID and descriptor upon successful connection.
     */
    suspend fun connectGlucoseSource(descriptor: GlucoseSourceConnectionDescriptor): Result<GlucoseSource> {
        Log.d(TAG, "Connecting to glucose source: ${descriptor.displayName}")
        return glucoseSourceDriverManager.connect(descriptor).onSuccess { glucoseSource ->
            glucoseSourceManager.glucoseSource = glucoseSource
            deviceManagementRepository.setActiveGlucoseSourceDriverId(descriptor.driverId)
            deviceManagementRepository.saveGlucoseSourceDescriptor(descriptor)
        }
    }

    /**
     * Connects to an insulin pump using the provided [descriptor], updates the active [PumpManager],
     * and persists the active driver ID and descriptor upon successful connection.
     */
    suspend fun connectPump(descriptor: PumpConnectionDescriptor): Result<InsulinPump> {
        Log.d(TAG, "Connecting to insulin pump: ${descriptor.displayName}")
        return pumpDriverManager.connect(descriptor).onSuccess { insulinPump ->
            pumpManager.insulinPump = insulinPump
            deviceManagementRepository.setActivePumpDriverId(descriptor.driverId)
            deviceManagementRepository.savePumpDescriptor(descriptor)
        }
    }

    /**
     * Disconnects the active glucose source and clears the persisted driver ID and descriptor.
     */
    suspend fun disconnectGlucoseSource() {
        Log.d(TAG, "Disconnecting active glucose source")
        glucoseSourceManager.glucoseSource = null
        deviceManagementRepository.setActiveGlucoseSourceDriverId(null)
        deviceManagementRepository.saveGlucoseSourceDescriptor(null)
    }

    /**
     * Disconnects the active insulin pump and clears the persisted driver ID and descriptor.
     */
    suspend fun disconnectPump() {
        Log.d(TAG, "Disconnecting active insulin pump")
        pumpManager.insulinPump = null
        deviceManagementRepository.setActivePumpDriverId(null)
        deviceManagementRepository.savePumpDescriptor(null)
    }

    companion object {
        private val TAG = DeviceConnectionManager::class.simpleName
    }
}