package de.dh.raaps.core.alarms

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.net.toUri
import de.dh.raaps.common.model.data.AlarmSoundConfig
import de.dh.raaps.common.model.data.VibrationMode

/**
 * Central engine for audio playback, haptics/vibration, and safety audio focus handling in RAAPS.
 */
interface AlarmPlayerManager {
    /**
     * Starts or updates an alarm sound and vibration based on the provided [config].
     *
     * @param config Sound, volume, vibration mode, and DND settings.
     * @param isSafetyCritical If true, continuous vibration is maintained even if audio focus is lost during phone calls.
     */
    fun playAlarm(config: AlarmSoundConfig, isSafetyCritical: Boolean = false)

    /**
     * Plays a short preview of the sound and vibration for user testing in the profile editor.
     *
     * @param config The sound configuration to test.
     * @param durationMs Duration of preview playback in milliseconds (default 3000ms).
     */
    fun playPreview(config: AlarmSoundConfig, durationMs: Long = 3000L)

    /**
     * Updates the audio playback volume in real-time (0..100%).
     */
    fun updateVolume(volume: Int)

    /**
     * Stops any currently active alarm sound, preview, or vibration.
     */
    fun stopAlarm()

    /**
     * Returns true if an alarm or preview is currently playing audio or vibrating.
     */
    fun isPlaying(): Boolean

    /**
     * Releases all held resources (MediaPlayer, audio focus, vibration).
     */
    fun release()
}

class AlarmPlayerManagerImpl(
    context: Context
) : AlarmPlayerManager {

    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val handler = Handler(Looper.getMainLooper())

    private val mutex = Any()

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    private var isPlayingActive = false
    private var currentIsSafetyCritical = false
    private var currentConfig: AlarmSoundConfig? = null
    private var isPreviewPlaying = false

    private val stopPreviewRunnable = Runnable {
        synchronized(mutex) {
            if (isPreviewPlaying) {
                Log.d(TAG, "Preview playback finished")
                stopAlarmInternal()
            }
        }
    }

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        synchronized(mutex) {
            when (focusChange) {
                AudioManager.AUDIOFOCUS_LOSS,
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                    Log.d(TAG, "Audio focus lost ($focusChange). SafetyCritical=$currentIsSafetyCritical")
                    if (currentIsSafetyCritical) {
                        // Safety-critical medical alarm: Pause audio sound if suppressed by OS call,
                        // but keep vibrating continuously to alert user during phone calls!
                        pauseAudioOnly()
                    } else {
                        pauseAudioAndVibration()
                    }
                }

                AudioManager.AUDIOFOCUS_GAIN -> {
                    Log.d(TAG, "Audio focus gained, resuming sound and vibration")
                    resumeAudioAndVibration()
                }
            }
        }
    }

    init {
        initVibrator()
    }

    private fun initVibrator() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    override fun playAlarm(config: AlarmSoundConfig, isSafetyCritical: Boolean) {
        synchronized(mutex) {
            stopAlarmInternal()
            currentConfig = config
            currentIsSafetyCritical = isSafetyCritical
            isPreviewPlaying = false

            startAlarmInternal(config, isLooping = true)
        }
    }

    override fun playPreview(config: AlarmSoundConfig, durationMs: Long) {
        synchronized(mutex) {
            stopAlarmInternal()
            currentConfig = config
            currentIsSafetyCritical = false
            isPreviewPlaying = true

            startAlarmInternal(config, isLooping = true)

            handler.postDelayed(stopPreviewRunnable, durationMs)
        }
    }

    override fun updateVolume(volume: Int) {
        synchronized(mutex) {
            val roundedVol = volume.coerceIn(0, 100)
            currentConfig = currentConfig?.copy(volume = roundedVol)
            val floatVol = roundedVol / 100f
            try {
                mediaPlayer?.setVolume(floatVol, floatVol)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating volume on MediaPlayer", e)
            }
        }
    }

    override fun stopAlarm() {
        synchronized(mutex) {
            stopAlarmInternal()
        }
    }

    override fun isPlaying(): Boolean {
        synchronized(mutex) {
            return isPlayingActive
        }
    }

    override fun release() {
        synchronized(mutex) {
            stopAlarmInternal()
            vibrator = null
        }
    }

    private fun startAlarmInternal(config: AlarmSoundConfig, isLooping: Boolean) {
        requestAudioFocus()

        // 1. Audio Playback
        if (config.volume > 0) {
            try {
                val soundUri = resolveSoundUri(config.soundUri)
                if (soundUri != null) {
                    val audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()

                    val player = MediaPlayer()
                    var preparedSuccessfully = false
                    try {
                        player.setDataSource(appContext, soundUri)
                        player.setAudioAttributes(audioAttributes)
                        val floatVol = config.volume.coerceIn(0, 100) / 100f
                        player.setVolume(floatVol, floatVol)
                        player.isLooping = isLooping
                        player.prepare()
                        player.start()
                        preparedSuccessfully = true
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to play custom soundUri '$soundUri', falling back to system default alarm sound", e)
                        val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                        if (defaultUri != null) {
                            try {
                                player.reset()
                                player.setDataSource(appContext, defaultUri)
                                player.setAudioAttributes(audioAttributes)
                                val floatVol = config.volume.coerceIn(0, 100) / 100f
                                player.setVolume(floatVol, floatVol)
                                player.isLooping = isLooping
                                player.prepare()
                                player.start()
                                preparedSuccessfully = true
                            } catch (fallbackEx: Exception) {
                                Log.e(TAG, "Fallback to system alarm sound also failed", fallbackEx)
                            }
                        }
                    }

                    if (preparedSuccessfully) {
                        mediaPlayer = player
                    } else {
                        player.release()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting MediaPlayer for alarm sound", e)
            }
        }

        // 2. Haptics / Vibration
        startVibration(config.vibrationMode)

        isPlayingActive = true
    }

    @SuppressLint("MissingPermission")
    private fun startVibration(mode: VibrationMode) {
        val currentVibrator = vibrator ?: return
        if (!currentVibrator.hasVibrator()) return

        try {
            currentVibrator.cancel()
            when (mode) {
                VibrationMode.OFF -> {
                    // No vibration
                }
                VibrationMode.SHORT -> {
                    val pattern = longArrayOf(0, 200, 200, 200)
                    val effect = VibrationEffect.createWaveform(pattern, -1)
                    currentVibrator.vibrate(effect)
                }
                VibrationMode.LONG -> {
                    val pattern = longArrayOf(0, 500, 300, 500)
                    val effect = VibrationEffect.createWaveform(pattern, -1)
                    currentVibrator.vibrate(effect)
                }
                VibrationMode.CONTINUOUS -> {
                    val pattern = longArrayOf(0, 1000, 500, 1000)
                    val effect = VibrationEffect.createWaveform(pattern, 0) // Repeat continuous
                    currentVibrator.vibrate(effect)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting vibration mode: $mode", e)
        }
    }

    private fun resolveSoundUri(uriString: String?): Uri? {
        if (!uriString.isNullOrEmpty()) {
            try {
                return uriString.toUri()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse custom soundUri '$uriString', falling back to system alarm sound", e)
            }
        }
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    }

    private fun pauseAudioOnly() {
        try {
            mediaPlayer?.pause()
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing MediaPlayer", e)
        }
        // Force continuous vibration for safety critical alarms during call/loss
        startVibration(VibrationMode.CONTINUOUS)
    }

    @SuppressLint("MissingPermission")
    private fun pauseAudioAndVibration() {
        try {
            mediaPlayer?.pause()
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing MediaPlayer", e)
        }
        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Error canceling vibrator", e)
        }
    }

    private fun resumeAudioAndVibration() {
        val config = currentConfig ?: return
        try {
            mediaPlayer?.start()
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming MediaPlayer", e)
        }
        startVibration(config.vibrationMode)
    }

    @SuppressLint("MissingPermission")
    private fun stopAlarmInternal() {
        handler.removeCallbacks(stopPreviewRunnable)

        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping/releasing MediaPlayer", e)
        } finally {
            mediaPlayer = null
        }

        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping Vibrator", e)
        }

        abandonAudioFocus()

        isPlayingActive = false
        isPreviewPlaying = false
        currentConfig = null
        currentIsSafetyCritical = false
    }

    private fun requestAudioFocus() {
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(audioAttributes)
                .setWillPauseWhenDucked(true)
                .setAcceptsDelayedFocusGain(false)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()

            audioFocusRequest = request
            val result = audioManager.requestAudioFocus(request)
            if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                Log.w(TAG, "Audio focus request was not granted immediately ($result)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting audio focus", e)
        }
    }

    private fun abandonAudioFocus() {
        audioFocusRequest?.let { request ->
            try {
                audioManager.abandonAudioFocusRequest(request)
            } catch (e: Exception) {
                Log.e(TAG, "Error abandoning audio focus", e)
            }
        }
        audioFocusRequest = null
    }

    companion object {
        private const val TAG = "AlarmPlayerManager"
    }
}