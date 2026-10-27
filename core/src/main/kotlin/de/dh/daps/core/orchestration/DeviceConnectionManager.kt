package de.dh.daps.core.orchestration

import android.util.Log
import de.dh.daps.common.domain.model.cgm.GlucoseSource
import de.dh.daps.common.domain.pump.InsulinPump
import de.dh.daps.core.glucosesource.GlucoseSourceDriverManager
import de.dh.daps.core.glucosesource.GlucoseSourceManager
import de.dh.daps.core.pump.PumpDriverManager
import de.dh.daps.core.pump.PumpManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Central manager responsible for connecting, disconnecting, and restoring saved hardware connections
 * (glucose source and insulin pump) using their respective driver managers and active device flows.
 */
class DeviceConnectionManager(
    private val glucoseSourceDriverManager: GlucoseSourceDriverManager,
    private val pumpDriverManager: PumpDriverManager,
    private val glucoseSourceManager: GlucoseSourceManager,
    private val pumpManager: PumpManager,
    private val scope: CoroutineScope,
) {
    private var observeJob: Job? = null

    /**
     * Synchronously attaches listeners to active glucose source and pump flows.
     * When an active driver emits a new [GlucoseSource] or [InsulinPump], it is automatically assigned to the manager.
     */
    fun startObserving() {
        if (observeJob != null) return

        observeJob = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            launch(start = CoroutineStart.UNDISPATCHED) {
                glucoseSourceDriverManager.activeGlucoseSource.collect { glucoseSource ->
                    glucoseSourceManager.glucoseSource = glucoseSource
                }
            }

            launch(start = CoroutineStart.UNDISPATCHED) {
                pumpDriverManager.activePump.collect { pump ->
                    pumpManager.insulinPump = pump
                }
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