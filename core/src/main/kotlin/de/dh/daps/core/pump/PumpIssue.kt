package de.dh.daps.core.pump

import de.dh.pump.PumpStatus

/**
 * Normalized state of pump-related issues that prevent the system from working correctly.
 */
sealed interface PumpIssue {
    /**
     * The pump connection is missing, the system is not able to do its work.
     * The work will be continued when the connection is available again.
     */
    data object ConnectionMissing : PumpIssue

    /**
     * The pump is connected but in a state where it cannot deliver insulin (e.g. suspended,
     * battery empty, reservoir empty, hardware error).
     */
    data object Inoperative : PumpIssue

    /**
     * A command sent to the pump failed with a specific status or error.
     */
    data class CommandFailed(val status: PumpStatus) : PumpIssue

    /**
     * The pump reservoir level is low.
     */
    data object LowInsulin : PumpIssue

    /**
     * The pump battery level is low.
     */
    data object LowBattery : PumpIssue

    /**
     * Any other issue that prevents the pump communication from working.
     */
    data object Other : PumpIssue
}