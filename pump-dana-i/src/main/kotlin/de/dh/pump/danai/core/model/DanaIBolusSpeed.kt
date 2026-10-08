package de.dh.pump.danai.core.model

import de.dh.pump.dana.commands.DanaRsBolusSpeed

/**
 * Represents the bolus delivery speed options in seconds per unit supported by Dana-i pumps.
 * Mirrors [DanaRsBolusSpeed] from the protocol layer, keeping the high-level
 * [DanaIPump] API independent of [DanaRsBolusSpeed].
 */
enum class DanaIBolusSpeed(val wireValue: Int) {
    U12_SECONDS(0),
    U30_SECONDS(1),
    U60_SECONDS(2);

    /**
     * Converts this [DanaIBolusSpeed] to the corresponding protocol-level [DanaRsBolusSpeed].
     */
    internal fun toDanaRsBolusSpeed(): DanaRsBolusSpeed = when (this) {
        U12_SECONDS -> DanaRsBolusSpeed.U12_SECONDS
        U30_SECONDS -> DanaRsBolusSpeed.U30_SECONDS
        U60_SECONDS -> DanaRsBolusSpeed.U60_SECONDS
    }

    companion object {
        fun fromWireValue(value: Int): DanaIBolusSpeed =
            entries.firstOrNull { it.wireValue == value } ?: U12_SECONDS

        /**
         * Converts a protocol-level [DanaRsBolusSpeed] to [DanaIBolusSpeed].
         */
        internal fun fromDanaRsBolusSpeed(speed: DanaRsBolusSpeed): DanaIBolusSpeed = when (speed) {
            DanaRsBolusSpeed.U12_SECONDS -> U12_SECONDS
            DanaRsBolusSpeed.U30_SECONDS -> U30_SECONDS
            DanaRsBolusSpeed.U60_SECONDS -> U60_SECONDS
        }
    }
}

/**
 * Converts a protocol-level [DanaRsBolusSpeed] to driver-level [DanaIBolusSpeed].
 */
internal fun DanaRsBolusSpeed.toDanaIBolusSpeed(): DanaIBolusSpeed = DanaIBolusSpeed.fromDanaRsBolusSpeed(this)