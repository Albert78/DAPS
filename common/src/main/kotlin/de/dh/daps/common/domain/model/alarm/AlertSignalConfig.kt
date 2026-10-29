package de.dh.daps.common.domain.model.alarm

enum class VibrationMode {
    OFF,
    SHORT,
    LONG,
    CONTINUOUS
}

/**
 * Audio sound configuration (active during full screen alerts or separate audio playback).
 */
data class SoundConfig(
    val volume: Int = 80, // 0..100 %
    val soundUri: String? = null // null = System default
)

/**
 * Controls the visual and acoustic presentation of a signal.
 */
sealed interface AlertDisplayMode {
    /** Standard notification in status bar / heads-up banner. */
    data object NotificationOnly : AlertDisplayMode

    /** Full-screen display (Activity/Screen) with optional audio playback. */
    data class FullScreen(
        val sound: SoundConfig? = null
    ) : AlertDisplayMode
}

/**
 * Unified signal configuration for alarms, APS recommendations, and reminders.
 */
data class AlertSignalConfig(
    val enabled: Boolean = true,
    val displayMode: AlertDisplayMode = AlertDisplayMode.NotificationOnly,
    val vibrationMode: VibrationMode = VibrationMode.SHORT,
    val overrideDnd: Boolean = false
) {
    val isFullScreen: Boolean
        get() = enabled && displayMode is AlertDisplayMode.FullScreen

    val soundConfig: SoundConfig?
        get() = if (enabled) (displayMode as? AlertDisplayMode.FullScreen)?.sound else null
}