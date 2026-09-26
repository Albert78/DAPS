package de.dh.daps.common.model

import de.dh.daps.common.model.data.Timestamp

/**
 * Interface to be implemented by plugins that connect to replaceable or disposable hardware components,
 * such as [GlucoseSource] plugins for replaceable CGM sensors or [InsulinPump] plugins for replaceable pods.
 */
interface ReplaceableComponent {
    /**
     * The start date and time when the component was activated or inserted,
     * or `null` if no start date is currently available or set.
     */
    val startDate: Timestamp?

    /**
     * The scheduled end date and time for the component replacement or expiration,
     * or `null` if no end date is currently available or set.
     */
    val endDate: Timestamp?
}