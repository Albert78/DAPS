package de.dh.daps.core.pump

import android.util.Log
import de.dh.daps.common.domain.model.therapy.InsulinAmount
import de.dh.daps.common.domain.model.therapy.InsulinPump
import de.dh.daps.common.domain.model.therapy.InsulinProfile
import de.dh.daps.common.domain.model.therapy.Minutes
import de.dh.daps.common.domain.model.therapy.Timestamp
import de.dh.daps.core.pump.PumpCoordinator.Companion.BUSY_RETRY_DELAYS_MS
import de.dh.daps.core.pump.PumpCoordinator.Companion.MAX_CONNECTION_RETRIES
import de.dh.daps.core.pump.PumpCoordinator.Companion.RETRY_INTERVAL_MS
import de.dh.pump.PumpCommandException
import de.dh.pump.PumpConnectionException
import de.dh.pump.PumpStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Low-level execution concurrency state of the coordinator.
 * Used as a thread guard / mutex lock state for [PumpCoordinator.operate].
 */
enum class PumpCoordinatorState {
    Idle, Running, Error
}

/**
 * High-level synchronization steps during pump hardware state reconciliation.
 */
enum class SyncStep {
    RefreshingStatus,
    ReconcilingSuspend,
    ReconcilingTempBasal,
    ReconcilingProfile,
    ExecutingBolus
}

/**
 * Reasons why a sync step or command is currently pausing and retrying.
 */
enum class RetryReason {
    PUMP_BUSY,
    CONNECTION_FAILED
}

/**
 * Logical operational state of the pump coordinator spanning across execution cycles and delays.
 * Distinct from low-level concurrency gate ([PumpCoordinatorState]).
 */
sealed interface PumpSyncStatus {
    /**
     * Coordinator is in sync. Waiting for the next scheduled wakeup or user action.
     */
    data class InSync(val nextScheduledWakeup: Timestamp?) : PumpSyncStatus

    /**
     * Actively communicating with pump hardware during a reconciliation/sync step.
     */
    data class Reconciling(val step: SyncStep) : PumpSyncStatus

    /**
     * Paused and waiting for backoff retry due to a temporary hardware response or connection issue.
     */
    data class Retrying(
        val reason: RetryReason,
        val currentAttempt: Int,
        val maxAttempts: Int,
        val nextAttemptAt: Timestamp
    ) : PumpSyncStatus

    /**
     * Permanent communication error or critical command error requiring intervention/reset.
     */
    data class Error(
        val errorSince: Timestamp,
        val cause: JobErrorCode? = null
    ) : PumpSyncStatus
}

/**
 * Desired target temporary basal rate.
 */
data class TargetTempBasal(
    val percent: Int,
    val durationHours: Int,
    val createdAt: Timestamp = Timestamp.now()
) {
    fun isExpired(): Boolean {
        return Timestamp.now() > createdAt.plusHours(durationHours)
    }
}

/**
 * Target state configuration for the pump (reconciliation target).
 */
data class DesiredPumpState(
    /**
     * Desired suspend state.
     */
    val targetSuspend: Boolean = false,

    /**
     * Desired temporary basal rate.
     */
    val targetTempBasal: TargetTempBasal? = null,

    /**
     * Desired insulin profile, if we want to change it. A `null` value indicates no change.
     */
    val targetProfile: InsulinProfile? = null
)

/**
 * Type of operation for a queued bolus job.
 */
sealed interface BolusJobType {
    data class Deliver(val amount: InsulinAmount, val bolusId: String? = null) : BolusJobType
    data object Stop : BolusJobType
}

/**
 * Error code describing why a bolus job failed or expired.
 */
sealed interface JobErrorCode {
    data object Expired : JobErrorCode
    data class ConnectionFailed(val message: String?) : JobErrorCode
    data class CommandFailed(val status: PumpStatus, val message: String?) : JobErrorCode
    data class TechnicalError(val message: String?) : JobErrorCode
}

/**
 * A queued bolus request (delivery or stop/cancel).
 */
data class BolusJob(
    val id: String = UUID.randomUUID().toString(),
    val type: BolusJobType,
    val createdAt: Timestamp = Timestamp.now(),
    val expiresAt: Timestamp? = null,
    val retryCount: Int = 0,
    val nextAttemptAt: Timestamp = Timestamp.now(),
    val lastError: JobErrorCode? = null
) {
    fun isExpired(): Boolean {
        val exp = expiresAt ?: return false
        return Timestamp.now() >= exp
    }

    fun isReady(): Boolean {
        return Timestamp.now() >= nextAttemptAt
    }
}

/**
 * The PumpCoordinator is the high-level orchestrator for the insulin pump (abstracted via [InsulinPump]).
 *
 * Key Responsibilities:
 * - **State Reconciliation:** Reconciles the pump's hardware state against the desired target state
 *   ([DesiredPumpState]) on every execution cycle.
 * - **Bolus Queue:** Manages a queue of [BolusJob]s for insulin boluses and bolus cancellations.
 * - **Connectivity & Status Sync:** Automatically refreshes hardware status and syncs history
 *   in every reconciliation cycle.
 * - **Power Management:** Coordinates with the system's power state by acquiring wake-locks
 *   during active communication and scheduling system wakeups for future tasks.
 * - **Lifecycle Status Tracking:** Exposes [PumpSyncStatus] to reflect active reconciliation steps
 *   ([SyncStep]), transient retries ([RetryReason]), and scheduled wakeup timing ([nextWakeupTime]).
 *
 * The coordinator's lifecycle is bound to the connection (Bluetooth bonding) with the pump;
 * it remains active as long as a bonding with the pump exists and is dropped when the bonding is
 * dismissed. It abstracts from the physical connection state of the pump and manages pending jobs.
 */
class PumpCoordinator private constructor(
    val pump: InsulinPump,
    /**
     * Callback to execute work while holding a system busy-state (wake-lock).
     */
    private val withBusyState: suspend (block: suspend () -> Unit) -> Unit,
    /**
     * Callback to request a system wakeup at the specified [Timestamp]. The implementation must
     * ensure [wakeup] is called when that time is reached.
     */
    private val onRequestWakeup: (Timestamp) -> Unit,
    private val onJobError: (job: BolusJob, code: JobErrorCode) -> Unit,
    private val scope: CoroutineScope,
) {
    private val _desiredState = MutableStateFlow(DesiredPumpState())
    val desiredState: StateFlow<DesiredPumpState> = _desiredState.asStateFlow()

    private val _pendingBoluses = MutableStateFlow<List<BolusJob>>(emptyList())
    val pendingBoluses: StateFlow<List<BolusJob>> = _pendingBoluses.asStateFlow()

    private val _reconciliationNeeded = MutableStateFlow(true)
    val reconciliationNeeded: StateFlow<Boolean> = _reconciliationNeeded.asStateFlow()

    /**
     * Current execution state of the coordinator.
     * Used as a concurrency gate to ensure serial execution of pump commands.
     */
    // Must only be set by [operate] or [reset] or called methods
    private val _pumpCoordinatorState = MutableStateFlow(PumpCoordinatorState.Idle)
    val pumpCoordinatorState: StateFlow<PumpCoordinatorState> = _pumpCoordinatorState.asStateFlow()

    /**
     * High-level logical sync status across lifecycle and retries.
     */
    private val _syncStatus = MutableStateFlow<PumpSyncStatus>(PumpSyncStatus.InSync(null))
    val syncStatus: StateFlow<PumpSyncStatus> = _syncStatus.asStateFlow()

    /**
     * Next scheduled wakeup time computed by [calculateNextWakeup].
     */
    private val _nextWakeupTime = MutableStateFlow<Timestamp?>(null)
    val nextWakeupTime: StateFlow<Timestamp?> = _nextWakeupTime.asStateFlow()

    // Must only be set by [operate] or [reset] or called methods
    private val _pumpCommunicationErrorSince = MutableStateFlow(Timestamp.INVALID)
    val pumpCommunicationErrorSince: StateFlow<Timestamp> = _pumpCommunicationErrorSince.asStateFlow()

    // If we're currently connected, lastConnectionTime is the time when the connection was established,
    // if we currently disconnected, lastConnectionTime is the time when the connection was disconnected.
    private val _lastConnectionTime = MutableStateFlow(Timestamp.INVALID)
    val lastConnectionTime: StateFlow<Timestamp> = _lastConnectionTime.asStateFlow()

    /**
     * The start of the requested history range. Set by our clients.
     */
    var requestedHistoryStart: Timestamp = Timestamp.now().minusHours(InsulinPump.DEFAULT_REQUESTED_HISTORY_HOURS)

    init {
        setupPump()
        operate()
    }

    private fun setupPump() {
        scope.launch {
            pump.isConnected.collect { _ ->
                _lastConnectionTime.value = Timestamp.now()
            }
        }
    }

    /**
     * Main execution loop for state reconciliation and bolus job processing.
     *
     * Coordinates communication with the pump hardware while holding a system wake-lock.
     * On each iteration, it invokes [sync] to refresh status and history, reconcile desired
     * pump target states (suspend, temp basal, profile), and execute pending bolus jobs.
     *
     * This method can be safely called from any thread. Internally, it manages serial execution
     * via atomic transitions on [_pumpCoordinatorState]. If a communication error occurs,
     * the loop terminates, setting [pumpCommunicationErrorSince] and relying on scheduled
     * wakeups or manual retries to resume.
     */
    private fun operate() {
        // Early exit if already running or in error state to avoid unnecessary coroutine launches
        if (_pumpCoordinatorState.value != PumpCoordinatorState.Idle) {
            return
        }
        scope.launch {
            withBusyState {
                do {
                    try {
                        // Atomically transition to Running. If another coroutine won the race, exit.
                        if (!_pumpCoordinatorState.compareAndSet(PumpCoordinatorState.Idle, PumpCoordinatorState.Running)) {
                            return@withBusyState
                        }
                        val success = sync()
                        if (success) {
                            _pumpCommunicationErrorSince.value = Timestamp.INVALID
                        } else {
                            if (_pumpCommunicationErrorSince.value == Timestamp.INVALID) {
                                _pumpCommunicationErrorSince.value = Timestamp.now()
                            } // Else keep former error time
                            // On communication error, stop the loop and rely on the next
                            // scheduled wakeup, reset or manual trigger to retry.
                            return@withBusyState
                        }
                    } finally {
                        scheduleNextWakeup()
                        if (_pumpCommunicationErrorSince.value.isValid()) {
                            _syncStatus.value = PumpSyncStatus.Error(_pumpCommunicationErrorSince.value)
                        } else {
                            _syncStatus.value = PumpSyncStatus.InSync(_nextWakeupTime.value)
                        }
                        // Reset to Idle only if we were Running (don't overwrite Error state if set by job)
                        _pumpCoordinatorState.compareAndSet(PumpCoordinatorState.Running, PumpCoordinatorState.Idle)
                    }
                    // Catch-up check: To avoid race conditions with requests or jobs added while processing,
                    // we verify if reconciliation is still needed or new bolus jobs were added.
                } while (_reconciliationNeeded.value || _pendingBoluses.value.any { it.isReady() })
            }
        }
    }

    suspend fun waitForJobsOrError(timeout: Duration = DEFAULT_WAIT_FOR_JOBS_TIMEOUT) {
        // Wait for Idle AND all status reconciliation + bolus jobs processed (or error in communication)
        val completed = withTimeoutOrNull(timeout) {
            combine(
                pumpCoordinatorState,
                pendingBoluses,
                pumpCommunicationErrorSince,
                _reconciliationNeeded
            ) { state, jobs, errorSince, reconNeeded ->
                (state == PumpCoordinatorState.Idle || state == PumpCoordinatorState.Error) &&
                        ((!reconNeeded && jobs.none { it.isReady() }) || errorSince.isValid())
            }.first { it }
        }
        if (completed == null) {
            Log.w(TAG, "waitForJobsOrError timed out after $timeout while waiting for pump jobs to complete.")
        }
    }

    private suspend fun sync(): Boolean {
        _reconciliationNeeded.value = false

        // Step 1: Always refresh status and history
        _syncStatus.value = PumpSyncStatus.Reconciling(SyncStep.RefreshingStatus)
        val statusHistorySuccess = refreshStatusAndHistoryWithRetries()
        if (!statusHistorySuccess) {
            _reconciliationNeeded.value = true
            return false
        }

        // Step 2: Reconcile Suspend
        _syncStatus.value = PumpSyncStatus.Reconciling(SyncStep.ReconcilingSuspend)
        val suspendSuccess = reconcileSuspendWithRetries()
        if (!suspendSuccess) {
            _reconciliationNeeded.value = true
            return false
        }

        // Check if pump is in suspend mode (reported after Step 1 status refresh or set during Step 2).
        // If suspended, steps 3-5 (temp basal, profile, boluses) cannot be executed on the pump hardware
        // and are preserved in desiredState / pendingBoluses until the pump is unsuspended.
        val isSuspended = _desiredState.value.targetSuspend ||
                pump.basalStatus.value.isSuspended ||
                pump.pumpStatus.value.pumpSuspended

        if (isSuspended) {
            return true
        }

        // Step 3: Reconcile TempBasal
        _syncStatus.value = PumpSyncStatus.Reconciling(SyncStep.ReconcilingTempBasal)
        val tempBasalSuccess = reconcileTempBasalWithRetries()
        if (!tempBasalSuccess) {
            _reconciliationNeeded.value = true
            return false
        }

        // Step 4: Reconcile Profile
        _syncStatus.value = PumpSyncStatus.Reconciling(SyncStep.ReconcilingProfile)
        val profileSuccess = reconcileProfileWithRetries()
        if (!profileSuccess) {
            _reconciliationNeeded.value = true
            return false
        }

        // Step 5: Process Bolus Queue
        _syncStatus.value = PumpSyncStatus.Reconciling(SyncStep.ExecutingBolus)
        val bolusSuccess = processBolusQueue()
        if (!bolusSuccess) return false

        return true
    }

    private suspend fun refreshStatusAndHistoryWithRetries(): Boolean {
        var connectionRetryCount = 0
        while (true) {
            try {
                pump.refreshStatus()
                pump.syncHistory(requestedHistoryStart)
                _lastConnectionTime.value = Timestamp.now()
                return true
            } catch (e: PumpConnectionException) {
                if (connectionRetryCount < MAX_CONNECTION_RETRIES) {
                    connectionRetryCount++
                    _syncStatus.value = PumpSyncStatus.Retrying(
                        reason = RetryReason.CONNECTION_FAILED,
                        currentAttempt = connectionRetryCount,
                        maxAttempts = MAX_CONNECTION_RETRIES,
                        nextAttemptAt = Timestamp.now().plusMs(RETRY_INTERVAL_MS)
                    )
                    delay(RETRY_INTERVAL_MS.milliseconds)
                } else {
                    Log.w(TAG, "Failed to refresh pump status and history due to connection error", e)
                    return false
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error refreshing pump status and history", e)
                return false
            }
        }
    }

    private suspend fun reconcileSuspendWithRetries(): Boolean {
        val targetSuspend = _desiredState.value.targetSuspend
        val actualSuspended = pump.basalStatus.value.isSuspended || pump.pumpStatus.value.pumpSuspended

        if (actualSuspended == targetSuspend) {
            return true
        }

        return tryExecuteActionWithRetries {
            pump.setSuspend(targetSuspend)
        }
    }

    private suspend fun reconcileTempBasalWithRetries(): Boolean {
        val targetSuspend = _desiredState.value.targetSuspend
        if (targetSuspend) {
            return true
        }

        val targetTemp = _desiredState.value.targetTempBasal
        val basalStatus = pump.basalStatus.value

        if (targetTemp != null && !targetTemp.isExpired()) {
            val isSameRate = basalStatus.isTempBasal && basalStatus.tempBasalPercent == targetTemp.percent
            if (isSameRate) {
                return true
            }
            return tryExecuteActionWithRetries {
                pump.tempBasal(targetTemp.percent, targetTemp.durationHours)
            }
        } else {
            if (basalStatus.isTempBasal) {
                return tryExecuteActionWithRetries {
                    pump.cancelTempBasal()
                }
            }
            return true
        }
    }

    private suspend fun reconcileProfileWithRetries(): Boolean {
        val targetProfile = _desiredState.value.targetProfile ?: return true
        val success = tryExecuteActionWithRetries {
            pump.setProfile(targetProfile)
        }
        if (success) {
            _desiredState.update { it.copy(targetProfile = null) }
        }
        return success
    }

    private suspend fun tryExecuteActionWithRetries(action: suspend () -> Unit): Boolean {
        var connectionRetryCount = 0
        var busyRetryCount = 0

        while (true) {
            try {
                action()
                _lastConnectionTime.value = Timestamp.now()
                return true
            } catch (e: PumpConnectionException) {
                if (connectionRetryCount < MAX_CONNECTION_RETRIES) {
                    connectionRetryCount++
                    _syncStatus.value = PumpSyncStatus.Retrying(
                        reason = RetryReason.CONNECTION_FAILED,
                        currentAttempt = connectionRetryCount,
                        maxAttempts = MAX_CONNECTION_RETRIES,
                        nextAttemptAt = Timestamp.now().plusMs(RETRY_INTERVAL_MS)
                    )
                    delay(RETRY_INTERVAL_MS.milliseconds)
                } else {
                    return false
                }
            } catch (e: PumpCommandException) {
                when (e.status) {
                    PumpStatus.BUSY -> {
                        if (busyRetryCount < BUSY_RETRY_DELAYS_MS.size) {
                            val delayMs = BUSY_RETRY_DELAYS_MS[busyRetryCount]
                            busyRetryCount++
                            _syncStatus.value = PumpSyncStatus.Retrying(
                                reason = RetryReason.PUMP_BUSY,
                                currentAttempt = busyRetryCount,
                                maxAttempts = BUSY_RETRY_DELAYS_MS.size,
                                nextAttemptAt = Timestamp.now().plusMs(delayMs)
                            )
                            delay(delayMs.milliseconds)
                        } else {
                            return false
                        }
                    }
                    else -> {
                        _pumpCoordinatorState.value = PumpCoordinatorState.Error
                        _syncStatus.value = PumpSyncStatus.Error(
                            errorSince = Timestamp.now(),
                            cause = JobErrorCode.CommandFailed(e.status, e.message)
                        )
                        return false
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Technical error during pump action", e)
                _pumpCoordinatorState.value = PumpCoordinatorState.Error
                _syncStatus.value = PumpSyncStatus.Error(
                    errorSince = Timestamp.now(),
                    cause = JobErrorCode.TechnicalError(e.message)
                )
                return false
            }
        }
    }

    /**
     * Processes pending bolus jobs in FIFO order.
     *
     * - Filters out and reports expired jobs ([JobErrorCode.Expired]).
     * - Selects the earliest ready job and attempts execution via [tryExecuteBolusJobWithRetries].
     * - On non-fatal failures, increments the job's retry count and reschedules its next attempt.
     * - Stops processing if an error state or communication failure occurs.
     *
     * @return `true` if all ready jobs were successfully processed or if the queue is empty; `false` otherwise.
     */
    private suspend fun processBolusQueue(): Boolean {
        val expired = _pendingBoluses.value.filter { it.isExpired() }
        if (expired.isNotEmpty()) {
            _pendingBoluses.update { it - expired.toSet() }
            expired.forEach { job ->
                val expiredJob = job.copy(lastError = JobErrorCode.Expired)
                onJobError(expiredJob, JobErrorCode.Expired)
            }
        }

        while (true) {
            val job = _pendingBoluses.value
                .filter { it.isReady() }
                .minByOrNull { it.nextAttemptAt } ?: return true

            val success = tryExecuteBolusJobWithRetries(job)

            if (success) {
                _pendingBoluses.update { pending -> pending.filterNot { it.id == job.id } }
            } else {
                if (_pumpCoordinatorState.value == PumpCoordinatorState.Error) {
                    return false
                }
                _pendingBoluses.update { pending ->
                    pending.map { j ->
                        if (j.id == job.id) {
                            j.copy(
                                retryCount = j.retryCount + 1,
                                nextAttemptAt = Timestamp.now().plusMinutes(1)
                            )
                        } else {
                            j
                        }
                    }
                }
                // Return with pending jobs in the queue
                return false
            }
        }
    }

    /**
     * Tries to execute a single [BolusJob] with retries for connection issues and busy pump status.
     *
     * Execution and Error Handling Behavior:
     * - **Success:** Updates [_lastConnectionTime] and returns `true`. The job will subsequently be removed from [_pendingBoluses].
     * - **Connection Failures ([PumpConnectionException]):** Retries up to [MAX_CONNECTION_RETRIES] times with [RETRY_INTERVAL_MS] delay.
     *   If retries are exhausted, updates the job with [JobErrorCode.ConnectionFailed], triggers [onJobError], and returns `false`.
     * - **Pump Busy ([PumpStatus.BUSY]):** Pauses execution according to the backoff schedule in [BUSY_RETRY_DELAYS_MS].
     *   If all retries are exhausted, updates the job with [JobErrorCode.CommandFailed], triggers [onJobError], and returns `false`.
     * - **Unexpected / Technical Errors:** Sets [_pumpCoordinatorState] to [PumpCoordinatorState.Error] and triggers [onJobError].
     *   Since this represents an unexpected or critical situation, the job is NOT removed from the queue automatically
     *   and must be acknowledged or manually removed in the management interface after user intervention.
     */
    private suspend fun tryExecuteBolusJobWithRetries(job: BolusJob): Boolean {
        var connectionRetryCount = 0
        var busyRetryCount = 0

        while (true) {
            try {
                when (val type = job.type) {
                    is BolusJobType.Deliver -> pump.bolus(type.amount, type.bolusId)
                    is BolusJobType.Stop ->
                        try {
                            pump.stopBolus()
                        } catch (_: Exception) {
                            // Ignore. If we cannot stop a bolus, it might already be finished or something else. Shit happens.
                        }
                }
                _lastConnectionTime.value = Timestamp.now()
                return true
            } catch (e: PumpConnectionException) {
                if (connectionRetryCount < MAX_CONNECTION_RETRIES) {
                    connectionRetryCount++
                    _syncStatus.value = PumpSyncStatus.Retrying(
                        reason = RetryReason.CONNECTION_FAILED,
                        currentAttempt = connectionRetryCount,
                        maxAttempts = MAX_CONNECTION_RETRIES,
                        nextAttemptAt = Timestamp.now().plusMs(RETRY_INTERVAL_MS)
                    )
                    delay(RETRY_INTERVAL_MS.milliseconds)
                } else {
                    val errorCode = JobErrorCode.ConnectionFailed(e.message)
                    val failedJob = job.copy(lastError = errorCode)
                    _pendingBoluses.update { pending -> pending.map { if (it.id == job.id) failedJob else it } }
                    onJobError(failedJob, errorCode)
                    return false
                }
            } catch (e: PumpCommandException) {
                when (val status = e.status) {
                    PumpStatus.BUSY -> {
                        if (busyRetryCount < BUSY_RETRY_DELAYS_MS.size) {
                            val delayMs = BUSY_RETRY_DELAYS_MS[busyRetryCount]
                            busyRetryCount++
                            _syncStatus.value = PumpSyncStatus.Retrying(
                                reason = RetryReason.PUMP_BUSY,
                                currentAttempt = busyRetryCount,
                                maxAttempts = BUSY_RETRY_DELAYS_MS.size,
                                nextAttemptAt = Timestamp.now().plusMs(delayMs)
                            )
                            delay(delayMs.milliseconds)
                        } else {
                            val errorCode = JobErrorCode.CommandFailed(status, e.message)
                            val failedJob = job.copy(lastError = errorCode)
                            _pendingBoluses.update { pending -> pending.map { if (it.id == job.id) failedJob else it } }
                            onJobError(failedJob, errorCode)
                            return false
                        }
                    }
                    else -> {
                        _pumpCoordinatorState.value = PumpCoordinatorState.Error
                        val errorCode = JobErrorCode.CommandFailed(status, e.message)
                        val failedJob = job.copy(lastError = errorCode)
                        _pendingBoluses.update { pending -> pending.map { if (it.id == job.id) failedJob else it } }
                        _syncStatus.value = PumpSyncStatus.Error(
                            errorSince = Timestamp.now(),
                            cause = errorCode
                        )
                        onJobError(failedJob, errorCode)
                        return false
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Unknown exception executing bolus job", e)
                _pumpCoordinatorState.value = PumpCoordinatorState.Error
                val errorCode = JobErrorCode.TechnicalError(e.message)
                val failedJob = job.copy(lastError = errorCode)
                _pendingBoluses.update { pending -> pending.map { if (it.id == job.id) failedJob else it } }
                _syncStatus.value = PumpSyncStatus.Error(
                    errorSince = Timestamp.now(),
                    cause = errorCode
                )
                onJobError(failedJob, errorCode)
                return false
            }
        }
    }

    fun setTargetTempBasal(percent: Int, durationHours: Int) {
        _desiredState.update { it.copy(targetTempBasal = TargetTempBasal(percent, durationHours)) }
        _reconciliationNeeded.value = true
        operate()
    }

    fun cancelTempBasal() {
        _desiredState.update { it.copy(targetTempBasal = null) }
        _reconciliationNeeded.value = true
        operate()
    }

    fun setTargetSuspend(suspended: Boolean) {
        _desiredState.update { it.copy(targetSuspend = suspended) }
        _reconciliationNeeded.value = true
        operate()
    }

    fun setTargetProfile(profile: InsulinProfile) {
        _desiredState.update { it.copy(targetProfile = profile) }
        _reconciliationNeeded.value = true
        operate()
    }

    fun deliverBolus(amount: InsulinAmount, bolusId: String? = null, expiresAt: Timestamp? = null) {
        _pendingBoluses.update { pending ->
            pending + BolusJob(
                type = BolusJobType.Deliver(amount = amount, bolusId = bolusId),
                expiresAt = expiresAt
            )
        }
        operate()
    }

    fun stopBolus() {
        _pendingBoluses.update {
            listOf(
                BolusJob(
                    type = BolusJobType.Stop
                )
            )
        }
        operate()
    }

    fun cancelBolusJobs(predicate: (BolusJob) -> Boolean = { true }) {
        _pendingBoluses.update { it.filterNot(predicate) }
    }

    fun triggerSync() {
        _reconciliationNeeded.value = true
        operate()
    }

    fun wakeup() {
        operate()
    }

    fun reset() {
        _pumpCoordinatorState.value = PumpCoordinatorState.Idle
        _pumpCommunicationErrorSince.value = Timestamp.INVALID
        _syncStatus.value = PumpSyncStatus.InSync(calculateNextWakeup())
        _reconciliationNeeded.value = true
        operate()
    }

    fun hasPendingJobs() = _pendingBoluses.value.isNotEmpty()

    fun getPendingJobsCount() = _pendingBoluses.value.size

    fun clearPendingJobs() {
        _pendingBoluses.update { emptyList() }
    }

    private fun scheduleNextWakeup() {
        val nextWakeup = calculateNextWakeup()
        _nextWakeupTime.value = nextWakeup
        if (nextWakeup != null) {
            onRequestWakeup(nextWakeup)
        }
    }

    private fun calculateNextWakeup(): Timestamp? {
        val now = Timestamp.now()
        val minWakeup = now.plusMinutes(1)

        val nextBolusTime = _pendingBoluses.value
            .filter { !it.isExpired() }
            .minOfOrNull { it.nextAttemptAt }

        val lastConn = _lastConnectionTime.value
        val baseTime = if (lastConn.isValid()) lastConn else now
        val nextHeartbeatTime = baseTime + HEARTBEAT_INTERVAL

        val retryTime = if (_pumpCommunicationErrorSince.value.isValid() || _reconciliationNeeded.value) {
            now.plusMinutes(1)
        } else null

        val earliest = listOfNotNull(nextBolusTime, nextHeartbeatTime, retryTime).minOrNull() ?: return null
        return if (earliest < minWakeup) minWakeup else earliest
    }

    /**
     * Shuts down the coordinator, cancelling all internal coroutines.
     */
    fun shutdown() {
        scope.cancel()
    }

    companion object {
        val TAG = PumpCoordinator::class.simpleName
        val DEFAULT_WAIT_FOR_JOBS_TIMEOUT: Duration = 30.seconds
        private val HEARTBEAT_INTERVAL = Minutes(15)
        private const val RETRY_INTERVAL_MS: Long = 10_000
        private const val MAX_CONNECTION_RETRIES = 3
        private val BUSY_RETRY_DELAYS_MS = listOf(15_000L, 30_000L, 60_000L)

        fun create(
            pump: InsulinPump,
            /** Callback to execute work while holding a system busy-state (wake-lock). */
            withBusyState: suspend (block: suspend () -> Unit) -> Unit,
            /** Callback to request a system wakeup at the specified [Timestamp]. The implementation must ensure [wakeup] is called when that time is reached. */
            onRequestWakeup: (Timestamp) -> Unit,
            onJobError: (BolusJob, JobErrorCode) -> Unit,
            scope: CoroutineScope
        ): PumpCoordinator {
            return PumpCoordinator(
                pump = pump,
                withBusyState = withBusyState,
                onRequestWakeup = onRequestWakeup,
                onJobError = onJobError,
                scope = scope
            )
        }
    }
}