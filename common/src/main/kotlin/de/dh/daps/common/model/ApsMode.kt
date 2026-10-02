package de.dh.daps.common.model

enum class ApsMode(
    val requiresPump: Boolean,
    val requiresGlucoseSource: Boolean,
) {
    /**
     * The APS completely controls the pump.
     */
    AutoCorrection(requiresPump = true, requiresGlucoseSource = true),

    /**
     * The APS runs but doesn't execute automatic treatments. Basal runs as configured in profile.
     * Suggestions are shown but the user has to accept them.
     */
    OnlySuggestions(requiresPump = true, requiresGlucoseSource = true),

    /**
     * The system is suspended, only manual interaction with the pump is possible.
     */
    Suspend(requiresPump = false, requiresGlucoseSource = false);

    /**
     * Checks whether this APS mode is available given the presence of a pump and glucose source.
     */
    fun isAvailable(hasPump: Boolean, hasGlucoseSource: Boolean): Boolean =
        (!requiresPump || hasPump) && (!requiresGlucoseSource || hasGlucoseSource)
}