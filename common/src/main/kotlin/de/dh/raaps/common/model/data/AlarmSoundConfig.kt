package de.dh.raaps.common.model.data

enum class VibrationMode {
    OFF,
    SHORT,
    LONG,
    CONTINUOUS
}

/**
 * Audio, sound, and vibration settings for an alarm or severity level.
 */
data class AlarmSoundConfig(
    val volume: Int = 80, // 0..100 %
    val soundUri: String? = null, // null = system default or silent if volume is 0
    val vibrationMode: VibrationMode = VibrationMode.SHORT,
    val overrideDnd: Boolean = false
)