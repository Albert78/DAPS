package de.dh.pump.dana.commands

/**
 * Glucose units supported by Dana pumps.
 */
enum class DanaGlucoseUnits(val wireValue: Int) {
    /**
     * Milligrams per deciliter.
     */
    MGDL(0),
    /**
     * Millimoles per liter.
     */
    MMOL(1),
    /**
     * Fallback for unknown protocol values.
     */
    UNKNOWN(-1);

    companion object {
        fun fromWireValue(value: Int): DanaGlucoseUnits = entries.firstOrNull { it.wireValue == value } ?: UNKNOWN
    }
}