package de.dh.daps.common.model.data

enum class VibrationMode {
    OFF,
    SHORT,
    LONG,
    CONTINUOUS
}

/**
 * Audio sound configuration (only active inside FullScreen display mode).
 */
data class SoundConfig(
    val volume: Int = 80, // 0..100 %
    val soundUri: String? = null // null = system default alarm sound
)

/**
 * Defines how an alarm is presented visually and acoustically.
 */
sealed interface AlertDisplayMode {
    /**
     * Standard notification in status bar/heads-up banner.
     * Supports vibration, but cannot play audio melodies.
     */
    data object NotificationOnly : AlertDisplayMode

    /**
     * Full-screen alarm screen (`AlarmActivity`).
     * Displays visual full-screen window and supports optional audio sound playback.
     */
    data class FullScreen(
        val sound: SoundConfig? = null
    ) : AlertDisplayMode
}

/**
 * Signal settings for an alarm or severity level.
 */
data class AlarmSignalConfig(
    val displayMode: AlertDisplayMode = AlertDisplayMode.NotificationOnly,
    val vibrationMode: VibrationMode = VibrationMode.SHORT,
    val overrideDnd: Boolean = false
) {
    val isFullScreen: Boolean
        get() = displayMode is AlertDisplayMode.FullScreen

    val soundConfig: SoundConfig?
        get() = (displayMode as? AlertDisplayMode.FullScreen)?.sound
}