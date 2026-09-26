package de.dh.daps.core.aps

import android.content.Context
import android.util.Log
import de.dh.daps.common.model.GlucoseSource
import de.dh.daps.common.model.data.BgReadingsInterval
import de.dh.daps.common.model.data.Minutes
import de.dh.daps.common.model.data.Timestamp
import de.dh.daps.core.repository.GlucoseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Manages the glucose source plugin and its pipeline.
 */
class GlucoseSourceManager(
    private val context: Context,
    private val glucoseRepository: GlucoseRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default.limitedParallelism(1))
    private var glucoseJob: Job? = null

    private val _glucoseSource = MutableStateFlow<GlucoseSource?>(null)
    val activeGlucoseSource: StateFlow<GlucoseSource?> = _glucoseSource.asStateFlow()

    private val _lastInputTimestamp = MutableStateFlow<Timestamp>(Timestamp.INVALID)
    val lastInputTimestamp: StateFlow<Timestamp> = _lastInputTimestamp.asStateFlow()

    var glucoseSource: GlucoseSource?
        get() = _glucoseSource.value
        set(value) {
            _glucoseSource.value?.stop()
            _glucoseSource.value = value
            _glucoseSource.value?.start()
            restartGlucosePipeline()
        }

    /**
     * Time delay between a glucose value in blood and the given Timestamp of the bg reading.
     * Typically, the bg reading timestamp represents the time of measure of the CGM system, which
     * is about 5 minutes behind blood glucose.
     */
    var readingsTimeDelay: Minutes = Minutes(5)
        private set

    var readingsInterval: BgReadingsInterval = BgReadingsInterval.FiveMinutes
        private set

    private fun restartGlucosePipeline() {
        glucoseJob?.cancel()
        val gs = glucoseSource ?: return

        glucoseJob = scope.launch {
            val sourceNameStr = gs.glucoseSourceName.asString(context)
            Log.d(TAG, "Installing glucose pipeline: $sourceNameStr")

            val sensorType = glucoseRepository.getOrCreateSensorTypeByName(gs.getSensorTypeName())
            val dataProvider = glucoseRepository.getOrCreateDataProviderByName(sourceNameStr, gs.dataProviderType)

            readingsTimeDelay = gs.readingsTimeDelay
            readingsInterval = gs.readingsInterval

            gs.getValues()
                .collect { reading ->
                    _lastInputTimestamp.value = Timestamp.now()
                    glucoseRepository.addReading(reading, dataProvider, sensorType)
                }
        }
    }

    fun stop() {
        glucoseSource?.stop()
        glucoseSource = null
        scope.cancel()
    }

    fun predictNextValueTimestamp(): Timestamp {
        val lastTime = lastInputTimestamp.value
        if (glucoseSource == null || lastTime.isInvalid()) {
            return Timestamp.INVALID
        }
        val intervalMinutes = when (readingsInterval) {
            BgReadingsInterval.OneMinute -> 1
            BgReadingsInterval.FiveMinutes -> 5
            else -> readingsTimeDelay.value.toInt()
        }
        return lastTime.plusMinutes(intervalMinutes)
    }

    companion object {
        private val TAG = GlucoseSourceManager::class.simpleName
    }
}