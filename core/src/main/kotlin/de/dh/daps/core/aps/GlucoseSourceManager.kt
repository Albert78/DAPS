package de.dh.daps.core.aps

import android.util.Log
import de.dh.daps.common.model.GlucoseSource
import de.dh.daps.common.model.GlucoseSourceStatus
import de.dh.daps.common.model.data.BgReadingsInterval
import de.dh.daps.common.model.data.Minutes
import de.dh.daps.common.model.data.Timestamp
import de.dh.daps.core.repository.GlucoseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Manages the glucose source plugin and its pipeline.
 */
class GlucoseSourceManager(
    private val glucoseRepository: GlucoseRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default.limitedParallelism(1))
    private var glucoseJob: Job? = null
    private var statusJob: Job? = null

    private val _glucoseSource = MutableStateFlow<GlucoseSource?>(null)
    val activeGlucoseSource: StateFlow<GlucoseSource?> = _glucoseSource.asStateFlow()

    private val _glucoseSourceIssues = MutableStateFlow<Set<GlucoseSourceIssue>>(setOf(GlucoseSourceIssue.ConnectionMissing))
    val glucoseSourceIssues: StateFlow<Set<GlucoseSourceIssue>> = _glucoseSourceIssues.asStateFlow()

    private val _lastInputTimestamp = MutableStateFlow(Timestamp.INVALID)
    val lastInputTimestamp: StateFlow<Timestamp> = _lastInputTimestamp.asStateFlow()

    var glucoseSource: GlucoseSource?
        get() = _glucoseSource.value
        set(value) {
            _glucoseSource.value?.stop()
            _glucoseSource.value = value
            updateIssuesAndPipeline(value)
            _glucoseSource.value?.start()
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

    private fun updateIssuesAndPipeline(source: GlucoseSource?) {
        statusJob?.cancel()
        statusJob = null

        if (source == null) {
            _glucoseSourceIssues.value = setOf(GlucoseSourceIssue.ConnectionMissing)
        } else {
            statusJob = scope.launch {
                launch {
                    source.status.collect { status ->
                        when (status) {
                            GlucoseSourceStatus.Ok -> {
                                removeIssue(GlucoseSourceIssue.ConnectionMissing)
                                removeIssue(GlucoseSourceIssue.SensorError)
                                removeIssue(GlucoseSourceIssue.Expired)
                            }
                            GlucoseSourceStatus.Expired -> {
                                removeIssue(GlucoseSourceIssue.ConnectionMissing)
                                setIssue(GlucoseSourceIssue.Expired)
                            }
                            GlucoseSourceStatus.Error -> {
                                removeIssue(GlucoseSourceIssue.ConnectionMissing)
                                setIssue(GlucoseSourceIssue.SensorError)
                            }
                        }
                    }
                }
                launch {
                    source.isExpired.collect { expired ->
                        if (expired) {
                            setIssue(GlucoseSourceIssue.Expired)
                        } else {
                            removeIssue(GlucoseSourceIssue.Expired)
                        }
                    }
                }
            }
        }
        restartGlucosePipeline()
    }

    private fun setIssue(issue: GlucoseSourceIssue) {
        if (issue !in _glucoseSourceIssues.value) {
            _glucoseSourceIssues.value += issue
        }
    }

    private fun removeIssue(issue: GlucoseSourceIssue) {
        if (issue in _glucoseSourceIssues.value) {
            _glucoseSourceIssues.value -= issue
        }
    }

    private fun restartGlucosePipeline() {
        glucoseJob?.cancel()
        glucoseJob = null
        val gs = glucoseSource ?: return

        glucoseJob = scope.launch {
            Log.d(TAG, "Installing glucose pipeline: ${gs.glucoseSourceId}")

            val sensorType = glucoseRepository.getOrCreateSensorTypeByName(gs.sensorType.value)
            val dataProvider = glucoseRepository.getOrCreateDataProvider(gs.glucoseSourceId)

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
        glucoseSource = null
        _lastInputTimestamp.value = Timestamp.INVALID
        _glucoseSourceIssues.value = setOf(GlucoseSourceIssue.ConnectionMissing)
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