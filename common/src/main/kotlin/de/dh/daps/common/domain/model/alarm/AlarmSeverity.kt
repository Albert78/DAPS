package de.dh.daps.common.domain.model.alarm

/**
 * Severity levels for alarms.
 */
enum class AlarmSeverity {
    CRITICAL, // High urgency (e.g. severe hypoglycemia, occlusion)
    WARNING,  // Moderate urgency (e.g. low/high glucose, signal loss)
    INFO      // Low urgency / informative (e.g. battery low, calibration)
}