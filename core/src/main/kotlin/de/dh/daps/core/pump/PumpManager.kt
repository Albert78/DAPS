package de.dh.daps.core.pump

import de.dh.daps.common.model.BolusStatus
import de.dh.daps.common.model.InsulinAmount
import de.dh.daps.common.model.InsulinHistory
import de.dh.daps.common.model.InsulinPump
import de.dh.daps.common.model.data.InsulinProfile
import de.dh.daps.common.model.data.Timestamp
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration

/**
 * Central manager for insulin pump interactions and status monitoring. Pump manager and [PumpCoordinator]
 * have a similar job but [PumpCoordinator] is bound to a pump connection, while [PumpManager]
 * is the system-wide service which even exists if no pump is configured in the system.
 */
interface PumpManager {
    /**
     * Active issues preventing the pump from working correctly.
     */
    val pumpIssues: StateFlow<Set<PumpIssue>>

    /**
     * The current insulin pump device. Setting this will initiate a new connection and a [pumpCoordinator]
     * to be created.
     */
    var insulinPump: InsulinPump?

    /**
     * Observable flow of the current insulin pump device.
     */
    val activeInsulinPump: StateFlow<InsulinPump?>

    /**
     * Provides access to the underlying coordinator. The pump coordinator is null if no pump
     * is present.
     * Note: Prefer using high-level methods of this manager.
     */
    val pumpCoordinator: PumpCoordinator?

    /**
     * The earliest timestamp in the past from which the DAPS system requests history events.
     * Delegated to the active [PumpCoordinator] if available.
     */
    var requestedHistoryStart: Timestamp

    /**
     * Sets the desired target temporary basal rate on the pump.
     */
    fun setTargetTempBasal(percent: Int, durationHours: Int)

    /**
     * Clears any active target temporary basal rate (returns to normal profile rate).
     */
    fun cancelTempBasal()

    /**
     * Sets the desired suspend state on the pump.
     */
    fun setTargetSuspend(suspended: Boolean)

    /**
     * Sets the desired target profile on the pump.
     */
    fun setTargetProfile(profile: InsulinProfile)

    /**
     * Queues an insulin bolus for delivery.
     */
    fun deliverBolus(amount: InsulinAmount, bolusId: String? = null, expiresAt: Timestamp? = null)

    /**
     * Immediately attempts to stop an ongoing bolus delivery on the pump hardware.
     */
    fun stopBolus()

    /**
     * Requests an explicit status refresh and history synchronization.
     */
    fun requestSync()

    /**
     * Cancels pending bolus jobs matching the predicate.
     */
    fun cancelJobs(predicate: (BolusJob) -> Boolean = { true })

    /**
     * Waits until all pending pump jobs are completed or until the timeout expires.
     */
    suspend fun waitForJobsOrError(timeout: Duration = PumpCoordinator.DEFAULT_WAIT_FOR_JOBS_TIMEOUT)

    /**
     * Explicitly wakes up the pump connection to process pending jobs or refresh status.
     */
    fun wakeup()

    /**
     * Returns true if there are pending jobs in the queue.
     */
    fun hasPendingJobs(): Boolean

    /**
     * Returns the number of pending jobs in the queue.
     */
    fun getPendingJobsCount(): Int

    /**
     * Resets the pump coordinator from an error state back to idle and clears command errors.
     */
    fun reset()

    fun stop()

    /**
     * Sets a listener to be notified when new insulin history data is received from the pump.
     */
    fun setOnHistoryUpdateListener(listener: suspend (InsulinHistory) -> Unit)

    /**
     * Sets a listener to be notified when bolus status is updated by the pump.
     */
    fun setOnBolusStatusUpdateListener(listener: suspend (BolusStatus) -> Unit)
}