package de.dh.daps.core.device

import android.util.Log
import de.dh.daps.common.model.CgmConnectionDescriptor
import de.dh.daps.common.model.GlucoseSource
import de.dh.daps.common.model.InsulinPump
import de.dh.daps.common.model.PumpConnectionDescriptor
import de.dh.daps.core.aps.CgmDriverManager
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
 * (glucose source and insulin pump) using their respective driver managers and saved connection descriptors.
 */
class DeviceConnectionManager(
    private val deviceManagementRepository: DeviceManagementRepository,
    private val cgmDriverManager: CgmDriverManager,
    private val pumpDriverManager: PumpDriverManager,
    private val glucoseSourceManager: GlucoseSourceManager,
    private val pumpManager: PumpManager
) {
    /**
     * Attempts to re-establish connections to the saved glucose source and insulin pump.
     * Should be called during application startup.
     */
    suspend fun restoreConnections(): DeviceConnectionResult {
        Log.d(TAG, "Restoring saved device connections...")

        val glucoseSourceDescriptor = deviceManagementRepository.getGlucoseSourceDescriptor()
        val glucoseSourceResult = if (glucoseSourceDescriptor != null) {
            Log.d(TAG, "Attempting to reconnect glucose source: ${glucoseSourceDescriptor.displayName}")
            cgmDriverManager.connect(glucoseSourceDescriptor).onSuccess { glucoseSource ->
                glucoseSourceManager.glucoseSource = glucoseSource
            }.onFailure { error ->
                Log.e(TAG, "Failed to reconnect glucose source '${glucoseSourceDescriptor.displayName}': ${error.message}", error)
            }
        } else {
            Log.d(TAG, "No saved glucose source descriptor found.")
            null
        }

        val pumpDescriptor = deviceManagementRepository.getPumpDescriptor()
        val pumpResult = if (pumpDescriptor != null) {
            Log.d(TAG, "Attempting to reconnect insulin pump: ${pumpDescriptor.displayName}")
            pumpDriverManager.connect(pumpDescriptor).onSuccess { insulinPump ->
                pumpManager.insulinPump = insulinPump
            }.onFailure { error ->
                Log.e(TAG, "Failed to reconnect insulin pump '${pumpDescriptor.displayName}': ${error.message}", error)
            }
        } else {
            Log.d(TAG, "No saved pump descriptor found.")
            null
        }

        return DeviceConnectionResult(
            glucoseSourceResult = glucoseSourceResult,
            pumpResult = pumpResult
        )
    }

    /**
     * Connects to a glucose source using the provided [descriptor], updates the active [GlucoseSourceManager],
     * and saves the descriptor upon successful connection.
     */
    suspend fun connectGlucoseSource(descriptor: CgmConnectionDescriptor): Result<GlucoseSource> {
        Log.d(TAG, "Connecting to glucose source: ${descriptor.displayName}")
        return cgmDriverManager.connect(descriptor).onSuccess { glucoseSource ->
            glucoseSourceManager.glucoseSource = glucoseSource
            deviceManagementRepository.saveGlucoseSourceDescriptor(descriptor)
        }
    }

    /**
     * Connects to an insulin pump using the provided [descriptor], updates the active [PumpManager],
     * and saves the descriptor upon successful connection.
     */
    suspend fun connectPump(descriptor: PumpConnectionDescriptor): Result<InsulinPump> {
        Log.d(TAG, "Connecting to insulin pump: ${descriptor.displayName}")
        return pumpDriverManager.connect(descriptor).onSuccess { insulinPump ->
            pumpManager.insulinPump = insulinPump
            deviceManagementRepository.savePumpDescriptor(descriptor)
        }
    }

    /**
     * Disconnects the active glucose source and clears the persisted descriptor.
     */
    suspend fun disconnectGlucoseSource() {
        Log.d(TAG, "Disconnecting active glucose source")
        glucoseSourceManager.glucoseSource = null
        deviceManagementRepository.saveGlucoseSourceDescriptor(null)
    }

    /**
     * Disconnects the active insulin pump and clears the persisted descriptor.
     */
    suspend fun disconnectPump() {
        Log.d(TAG, "Disconnecting active insulin pump")
        pumpManager.insulinPump = null
        deviceManagementRepository.savePumpDescriptor(null)
    }

    companion object {
        private val TAG = DeviceConnectionManager::class.simpleName
    }
}