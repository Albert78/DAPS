package de.dh.daps.core.pump

import de.dh.pump.PumpStatus

/**
 * Normalized state of pump-related issues that prevent the system from working correctly.
 */
sealed interface PumpIssue {
    /**
     * No insulin pump is bound in the system.
     */
    data object NoPumpBound : PumpIssue

    /**
     * The pump is connected but in a state where it cannot deliver insulin (e.g. suspended,
     * battery empty, reservoir empty, hardware error).
     */
    data object Inoperative : PumpIssue

    /**
     * The pump connection is missing, the system is not able to do its work.
     * The work will be continued when the connection is available again.
     */
    data object ConnectionMissing : PumpIssue

    /**
     * An occlusion / blockage was detected in the pump or delivery line.
     */
    data object Occlusion : PumpIssue

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
     * The pump or pod has expired.
     */
    data object Expired : PumpIssue

    /**
     * Catheter change is due or expired (durable pump).
     */
    data object CatheterChangeWarn : PumpIssue

    /**
     * Patch pod change is due or expired (patch pump).
     */
    data object PatchChangeWarn : PumpIssue

    /**
     * Communication or command execution with the pump is blocked on software level
     * (e.g. driver in maintenance/blocked mode or coordinator in error state).
     */
    data object Blocked : PumpIssue

    /**
     * The OS-level Bluetooth bond is stale/corrupted and requires manual unbonding in Android Settings.
     */
    data object StaleBluetoothBond : PumpIssue

    /**
     * Any other issue that prevents the pump communication from working.
     */
    data object Other : PumpIssue
}