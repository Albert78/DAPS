package de.dh.daps.core.pump

import android.content.Intent
import android.util.Log
import de.dh.daps.common.PUMP_BATTERY_LOW_THRESHOLD
import de.dh.daps.common.PUMP_RESERVOIR_LOW_THRESHOLD
import de.dh.daps.common.domain.model.therapy.BolusStatus
import de.dh.daps.common.domain.model.therapy.InsulinAmount
import de.dh.daps.common.domain.model.therapy.InsulinHistory
import de.dh.daps.common.domain.model.therapy.InsulinPump
import de.dh.daps.common.domain.model.therapy.PumpLifecycleState
import de.dh.daps.common.domain.model.therapy.InsulinProfile
import de.dh.daps.common.domain.model.therapy.Timestamp
import de.dh.daps.common.infrastructure.SystemWakeService
import de.dh.daps.common.infrastructure.WakeupHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlin.time.Duration

/**
 * Implementation of [PumpManager].
 */
class PumpManagerImpl(
    private val scope: CoroutineScope,
    private val wakeService: SystemWakeService
) : PumpManager, WakeupHandler {
    private val _pumpIssues = MutableStateFlow<Set<PumpIssue>>(setOf(PumpIssue.NoPumpBound))
    override val pumpIssues: StateFlow<Set<PumpIssue>> = _pumpIssues.asStateFlow()

    private val _activeInsulinPump = MutableStateFlow<InsulinPump?>(null)
    override val activeInsulinPump: StateFlow<InsulinPump?> = _activeInsulinPump.asStateFlow()

    override var pumpCoordinator: PumpCoordinator? = null
        private set

    private var fallbackRequestedHistoryStart: Timestamp = Timestamp.now().minusHours(InsulinPump.DEFAULT_REQUESTED_HISTORY_HOURS)

    override var requestedHistoryStart: Timestamp
        get() = pumpCoordinator?.requestedHistoryStart ?: fallbackRequestedHistoryStart
        set(value) {
            fallbackRequestedHistoryStart = value
            pumpCoordinator?.requestedHistoryStart = value
        }

    private var pumpMonitorJob: Job? = null
    private var historyUpdateListener: (suspend (InsulinHistory) -> Unit)? = null
    private var bolusStatusUpdateListener: (suspend (BolusStatus) -> Unit)? = null

    init {
        wakeService.registerHandler(WAKE_TAG, this)
    }

    override var insulinPump: InsulinPump?
        get() = _activeInsulinPump.value
        set(value) {
            _activeInsulinPump.value = value
            pumpMonitorJob?.cancel()
            pumpCoordinator?.shutdown()
            pumpCoordinator = if (value == null) {
                _pumpIssues.value = setOf(PumpIssue.NoPumpBound)
                null
            } else {
                removeIssue(PumpIssue.NoPumpBound)
                val pc = PumpCoordinator.create(
                    pump = value,
                    withBusyState = { block -> wakeService.withBusyStateSuspend(WAKE_TAG, block) },
                    onRequestWakeup = { timestamp -> wakeService.scheduleWakeup(WAKE_TAG, WAKEUP_PUMP_COORDINATOR, timestamp) },
                    onJobError = { job, jobErrorCode -> handleJobError(job, jobErrorCode) },
                    scope = scope + SupervisorJob() + Dispatchers.Default
                )
                pc.requestedHistoryStart = fallbackRequestedHistoryStart
                pumpMonitorJob = scope.launch {
                    launch {
                        pc.lastConnectionTime.collect { time ->
                            if (time != Timestamp.INVALID) {
                                removeIssue(PumpIssue.ConnectionMissing)
                            }
                        }
                    }
                    launch {
                        pc.pumpCommunicationErrorSince.collect { time ->
                            if (time != Timestamp.INVALID) {
                                setIssue(PumpIssue.ConnectionMissing)
                            }
                        }
                    }
                    launch {
                        pc.pump.pumpStatus.collect { status ->
                            if (status.pumpSuspended) {
                                setIssue(PumpIssue.Inoperative)
                            } else {
                                removeIssue(PumpIssue.Inoperative)
                            }

                            if (status.batteryRemainingPercent < PUMP_BATTERY_LOW_THRESHOLD) {
                                setIssue(PumpIssue.LowBattery)
                            } else if (!pc.pump.alerts.value.batteryLow) {
                                removeIssue(PumpIssue.LowBattery)
                            }

                            if (status.reservoirRemainingUnits > InsulinAmount.ZERO && status.reservoirRemainingUnits <= PUMP_RESERVOIR_LOW_THRESHOLD) {
                                setIssue(PumpIssue.LowInsulin)
                            } else if (!pc.pump.alerts.value.reservoirLow) {
                                removeIssue(PumpIssue.LowInsulin)
                            }
                        }
                    }
                    launch {
                        pc.pump.alerts.collect { alerts ->
                            if (alerts.batteryLow) {
                                setIssue(PumpIssue.LowBattery)
                            } else if (pc.pump.pumpStatus.value.batteryRemainingPercent > PUMP_BATTERY_LOW_THRESHOLD || pc.pump.pumpStatus.value.batteryRemainingPercent <= 0) {
                                removeIssue(PumpIssue.LowBattery)
                            }

                            if (alerts.reservoirLow) {
                                setIssue(PumpIssue.LowInsulin)
                            } else if (pc.pump.pumpStatus.value.reservoirRemainingUnits > PUMP_RESERVOIR_LOW_THRESHOLD || pc.pump.pumpStatus.value.reservoirRemainingUnits <= InsulinAmount.ZERO) {
                                removeIssue(PumpIssue.LowInsulin)
                            }

                            if (alerts.occlusion) {
                                setIssue(PumpIssue.Occlusion)
                            } else {
                                removeIssue(PumpIssue.Occlusion)
                            }
                        }
                    }
                    launch {
                        pc.pump.pumpStatus.collect { status ->
                            val isExpired = when (val lifecycle = status.lifecycleState) {
                                is PumpLifecycleState.Patch -> lifecycle.isExpired
                                is PumpLifecycleState.Durable -> false
                            }
                            if (isExpired) {
                                setIssue(PumpIssue.Expired)
                            } else {
                                removeIssue(PumpIssue.Expired)
                            }
                        }
                    }
                    launch {
                        combine(pc.pump.isBlocked, pc.syncStatus) { driverBlocked, syncStatus ->
                            driverBlocked || syncStatus is PumpSyncStatus.Error
                        }.collect { isBlocked ->
                            if (isBlocked) {
                                setIssue(PumpIssue.Blocked)
                            } else {
                                removeIssue(PumpIssue.Blocked)
                            }
                        }
                    }
                    launch {
                        pc.pump.requiresStaleBondRemoval.collect { requiresRemoval ->
                            if (requiresRemoval) {
                                setIssue(PumpIssue.StaleBluetoothBond)
                            } else {
                                removeIssue(PumpIssue.StaleBluetoothBond)
                            }
                        }
                    }
                    // Sync history
                    launch {
                        pc.pump.history.collect { history ->
                            history?.let { historyUpdateListener?.invoke(it) }
                        }
                    }
                    // Bolus status
                    launch {
                        pc.pump.bolusStatus.collect { status ->
                            bolusStatusUpdateListener?.invoke(status)
                        }
                    }
                }
                pc
            }
        }

    override fun setTargetTempBasal(percent: Int, durationHours: Int) {
        pumpCoordinator?.setTargetTempBasal(percent, durationHours)
    }

    override fun cancelTempBasal() {
        pumpCoordinator?.cancelTempBasal()
    }

    override fun setTargetSuspend(suspended: Boolean) {
        pumpCoordinator?.setTargetSuspend(suspended)
    }

    override fun setTargetProfile(profile: InsulinProfile) {
        pumpCoordinator?.setTargetProfile(profile)
    }

    override fun deliverBolus(amount: InsulinAmount, bolusId: String?, expiresAt: Timestamp?) {
        pumpCoordinator?.deliverBolus(amount, bolusId, expiresAt)
    }

    override fun stopBolus() {
        pumpCoordinator?.stopBolus()
    }

    override fun requestSync() {
        pumpCoordinator?.triggerSync()
    }

    override fun cancelJobs(predicate: (BolusJob) -> Boolean) {
        pumpCoordinator?.cancelBolusJobs(predicate)
    }

    override suspend fun waitForJobsOrError(timeout: Duration) {
        pumpCoordinator?.waitForJobsOrError(timeout)
    }

    override fun wakeup() {
        pumpCoordinator?.wakeup()
    }

    override fun reset() {
        _pumpIssues.value = if (insulinPump == null) setOf(PumpIssue.NoPumpBound) else emptySet()
        pumpCoordinator?.reset()
    }

    override fun stop() {
        insulinPump = null
        _pumpIssues.value = setOf(PumpIssue.NoPumpBound)
    }

    override fun hasPendingJobs(): Boolean {
        return pumpCoordinator?.hasPendingJobs() ?: false
    }

    override fun getPendingJobsCount(): Int {
        return pumpCoordinator?.getPendingJobsCount() ?: 0
    }

    override fun setOnHistoryUpdateListener(listener: suspend (InsulinHistory) -> Unit) {
        historyUpdateListener = listener
        scope.launch {
            // Call the listener once with the current history value
            pumpCoordinator?.pump?.history?.value?.let { listener(it) }
        }
    }

    override fun setOnBolusStatusUpdateListener(listener: suspend (BolusStatus) -> Unit) {
        bolusStatusUpdateListener = listener
        scope.launch {
            // Call the listener once with the current bolus status value
            pumpCoordinator?.pump?.bolusStatus?.value?.let { listener(it) }
        }
    }

    override fun onWakeup(wakeupId: UInt?, intent: Intent?) {
        if (wakeupId == WAKEUP_PUMP_COORDINATOR) {
            wakeup()
        }
    }

    private fun setIssue(issue: PumpIssue) {
        if (issue !in _pumpIssues.value) {
            _pumpIssues.value += issue
        }
    }

    private fun removeIssue(issue: PumpIssue) {
        if (issue in _pumpIssues.value) {
            _pumpIssues.value -= issue
        }
    }

    private fun handleJobError(job: BolusJob, jobErrorCode: JobErrorCode) {
        when (jobErrorCode) {
            JobErrorCode.Expired,
            is JobErrorCode.ConnectionFailed -> setIssue(PumpIssue.ConnectionMissing)
            is JobErrorCode.CommandFailed -> {
                Log.e(TAG, "Pump bolus command failed with status ${jobErrorCode.status} for job $job")
                setIssue(PumpIssue.CommandFailed(jobErrorCode.status))
            }
            is JobErrorCode.TechnicalError -> {
                Log.e(TAG, "Pump technical error: ${jobErrorCode.message} for job $job")
                setIssue(PumpIssue.Other)
            }
        }
    }

    companion object {
        private const val TAG = "PumpManager"
        private const val WAKE_TAG = "PUMP"
        val WAKEUP_PUMP_COORDINATOR = 0u
    }
}