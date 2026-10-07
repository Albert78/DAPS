package de.dh.daps.plugin.simbody

import android.content.Intent
import android.util.Log
import de.dh.daps.common.model.data.BgReading
import de.dh.daps.common.model.data.BgSampleKind
import de.dh.daps.common.model.data.BgValue
import de.dh.daps.common.model.data.Timestamp
import de.dh.daps.common.model.data.max
import de.dh.daps.common.service.SystemWakeService
import de.dh.daps.common.service.WakeupHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Random
import kotlin.time.Duration.Companion.milliseconds

/**
 * Handles the periodic wakeup of the SimBody simulation using the central [SystemWakeService].
 * This component acts as the external CGM trigger by advancing the simulation state
 * and emitting glucose values at regular intervals.
 */
class SimBodyHeartbeat(
    private val wakeService: SystemWakeService,
    private val bodyModel: BodyModel,
    private val pumpDevice: SimBodyPumpDevice,
    private val onBgReading: (BgReading) -> Unit
) : WakeupHandler {

    private val scope = CoroutineScope(Dispatchers.Main)
    private var started = false
    private var lastStepTimestamp: Timestamp = Timestamp.INVALID

    init {
        wakeService.registerHandler(WAKE_TAG, this)
    }

    /**
     * Starts the heartbeat. If already started, this does nothing.
     */
    fun start() {
        if (started) return
        started = true
        Log.d(TAG, "SimBody Heartbeat waiting for loading...")

        scope.launch {
            bodyModel.isLoadedFlow.first { it }
            // Small delay to allow the system to settle before the first emission
            delay(1000.milliseconds)
            Log.d(TAG, "SimBody Heartbeat starting (model loaded)")

            val now = Timestamp.now()
            val nextEmission = bodyModel.lastSimulationTimestamp.plusMinutes(SIMULATION_INTERVAL_MINUTES)

            if (now >= nextEmission) {
                // We are past the next expected emission, or it's the first run
                performSimulationStep()
                scheduleNext()
            } else {
                // It's not time yet, wait for the next regular turnus
                Log.d(TAG, "Resuming rhythm, next emission at $nextEmission")
                scheduleNext(nextEmission)
            }
        }
    }

    /**
     * Stops the heartbeat.
     */
    fun stop() {
        started = false
        Log.d(TAG, "SimBody Heartbeat stopped")
    }

    /**
     * Triggers an immediate simulation step and reschedules the next step.
     * Starts the heartbeat if it was not started yet.
     */
    fun triggerImmediateStep() {
        if (!started) {
            start()
        } else {
            scope.launch {
                performSimulationStep()
                scheduleNext()
            }
        }
    }

    override fun onWakeup(wakeupId: UInt?, intent: Intent?) {
        if (!started) return
        if (wakeupId == WAKEUP_ID_SIMULATION) {
            scope.launch {
                performSimulationStep()
                scheduleNext()
            }
        }
    }

    private val random = Random()

    private suspend fun performSimulationStep() {
        val now = Timestamp.now()
        val lastExecuted = max(lastStepTimestamp, bodyModel.lastSimulationTimestamp)
        val minNextAllowed = lastExecuted.plusMinutes(MIN_STEP_INTERVAL_MINUTES)

        if (now < minNextAllowed) {
            Log.d(TAG, "SimBody Heartbeat Simulation Step debounced ($now < $minNextAllowed)")
            return
        }

        lastStepTimestamp = now
        Log.d(TAG, "SimBody Heartbeat Simulation Step at $now")

        // Ensure the system stays awake during emission
        wakeService.acquireBusyState(WAKE_TAG)
        try {
            // First advance pump device and body model to current timestamp
            pumpDevice.advanceTo(now)
            bodyModel.advanceTo(now)

            if (!bodyModel.isSensorEnabled) {
                Log.d(TAG, "Sensor is disabled, skipping BG reading emission")
                return
            }

            val baseBg = bodyModel.getDelayedBloodGlucose(
                SimBodyGlucoseSource.DEFAULT_READINGS_DELAY.value.toInt(),
                now
            )
            val noiseFactor = bodyModel.sensorNoiseFactor

            val finalBg = if (noiseFactor > 0) {
                // Apply Gaussian noise with the noise factor as standard deviation
                val noise = random.nextGaussian() * noiseFactor
                (baseBg + noise).coerceIn(20.0, 500.0)
            } else {
                baseBg
            }

            val reading = BgReading(
                value = BgValue.fromMgDl(finalBg.toInt()),
                sampleKind = BgSampleKind.Value,
                timestamp = now
            )
            onBgReading(reading)
        } finally {
            wakeService.releaseBusyState(WAKE_TAG)
        }
    }

    private fun scheduleNext(targetTime: Timestamp? = null) {
        if (!started) return

        val nextStep = if (targetTime != null) {
            targetTime
        } else {
            val now = Timestamp.now()
            var next = bodyModel.lastSimulationTimestamp.plusMinutes(SIMULATION_INTERVAL_MINUTES)
            val bufferTime = now.plusSeconds(5)

            while (next <= bufferTime) { // 5s buffer
                next = next.plusMinutes(SIMULATION_INTERVAL_MINUTES)
            }
            next
        }

        Log.d(TAG, "Scheduling next simulation step at $nextStep")
        wakeService.scheduleWakeup(WAKE_TAG, WAKEUP_ID_SIMULATION, nextStep)
    }

    companion object {
        private const val TAG = "SimBodyHeartbeat"

        /**
         * Tag for the [SystemWakeService] to identify SimBody wakeups.
         */
        const val WAKE_TAG = "SIM_BODY"

        private val WAKEUP_ID_SIMULATION = 1u
        private const val SIMULATION_INTERVAL_MINUTES = 5
        private const val MIN_STEP_INTERVAL_MINUTES = 1
    }
}