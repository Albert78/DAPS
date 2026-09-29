package de.dh.daps.common.model

enum class ApsMode {
    /**
     * The APS completely controls the pump.
     */
    AutoCorrection,

    /**
     * The APS runs but doesn't execute automatic treatments. Basal runs as configured in profile.
     * Suggestions are shown but the user has to accept them.
     */
    OnlySuggestions,

    /**
     * The pump delivers basal as configured, manual interaction is possible, but APS doesn't calculate
     * nor produces suggestions.
     */
    ApsSuspended
}