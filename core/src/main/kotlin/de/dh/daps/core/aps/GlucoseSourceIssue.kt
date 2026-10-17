package de.dh.daps.core.aps

/**
 * Normalized state of glucose-source-related issues that prevent the system from receiving blood glucose readings.
 */
sealed interface GlucoseSourceIssue {
    /**
     * No glucose source / CGM is bound in the system.
     */
    data object NoGlucoseSourceBound : GlucoseSourceIssue

    /**
     * The glucose source / sensor has expired.
     */
    data object Expired : GlucoseSourceIssue

    /**
     * The glucose source / sensor reported an error state.
     */
    data object SensorError : GlucoseSourceIssue

    /**
     * Any other issue related to the glucose source.
     */
    data class Other(val message: String? = null) : GlucoseSourceIssue
}