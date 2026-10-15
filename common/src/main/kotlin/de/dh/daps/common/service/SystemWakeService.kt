package de.dh.daps.common.service

import android.content.Intent
import de.dh.daps.common.model.data.Timestamp

/**
 * Interface for components that want to be notified when a system wakeup occurs.
 */
interface WakeupHandler {
    /**
     * Called when a system wakeup for the registered tag occurs.
     *
     * @param wakeupId The ID of the wakeup that was scheduled, or null if no ID was provided.
     * @param intent The intent that triggered the wakeup, containing potential extras.
     */
    fun onWakeup(wakeupId: UInt?, intent: Intent?)
}

/**
 * Service to manage system wakeups and wake locks across the application.
 * This ensures that the device stays awake when performing critical background tasks
 * and handles the scheduling of future wakeups.
 */
interface SystemWakeService {
    /**
     * Registers a handler for a specific tag.
     */
    fun registerHandler(tag: String, handler: WakeupHandler)

    /**
     * Unregisters the handler for a specific tag.
     */
    fun unregisterHandler(tag: String)

    /**
     * Schedules a system wakeup at the given timestamp for a specific tag.
     *
     * @param tag The tag associated with the handler. This tag must be unique among all
     * wakeup handlers.
     * @param wakeupId An optional non-negative ID to identify this specific wakeup.
     * @param timestamp The absolute time for the wakeup.
     */
    fun scheduleWakeup(tag: String, wakeupId: UInt?, timestamp: Timestamp)

    /**
     * Cancels a previously scheduled system wakeup for a specific tag and wakeupId.
     */
    fun cancelWakeup(tag: String, wakeupId: UInt? = null)

    /**
     * Executes the given synchronous [block] while holding a reference-counted system busy state (wake lock).
     *
     * @param tag Informational tag identifying the component acquiring the busy state.
     * @param block The block of code to execute while holding the busy state.
     */
    fun <T> withBusyState(tag: String, block: () -> T): T

    /**
     * Suspending variant of [withBusyState].
     *
     * @param tag Informational tag identifying the component acquiring the busy state.
     * @param block The suspending block of code to execute while holding the busy state.
     */
    suspend fun <T> withBusyStateSuspend(tag: String, block: suspend () -> T): T

    /**
     * Dispatches an incoming wakeup intent to the appropriate handler.
     * This is intended to be called by the central SystemWakeReceiver.
     */
    fun dispatchWakeup(intent: Intent)
}