package de.dh.daps.common.model

import de.dh.daps.common.model.data.Timestamp

/**
 * Interface to be implemented by plugins that connect to replaceable or disposable hardware components,
 * such as [GlucoseSource] plugins for replaceable CGM sensors or [InsulinPump] plugins for replaceable pods.
 */
interface ReplaceableComponent {
    /**
     * The date and time of the next scheduled replacement or change,
     * or `null` if no change date is currently available or set.
     */
    val nextChangeDate: Timestamp?
}