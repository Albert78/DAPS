package de.dh.daps.core.alarms

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
import de.dh.daps.common.model.data.AlarmSignalConfig
import de.dh.daps.common.model.data.SoundConfig
import de.dh.daps.common.model.data.VibrationMode

/**
 * Central engine for audio playback, haptics/vibration, and safety audio focus handling in DAPS.
 */
interface AlarmPlayerManager {
    fun playAlarm(signalConfig: AlarmSignalConfig, isSafetyCritical: Boolean = false)
    fun playAlarm(soundConfig: SoundConfig?, vibrationMode: VibrationMode = VibrationMode.SHORT, overrideDnd: Boolean = false, isSafetyCritical: Boolean = false)
    fun playPreview(signalConfig: AlarmSignalConfig, durationMs: Long = 3000L)
    fun playPreview(soundConfig: SoundConfig?, vibrationMode: VibrationMode = VibrationMode.SHORT, durationMs: Long = 3000L)
    fun updateVolume(volume: Int)
    fun stopAlarm()
    fun isPlaying(): Boolean
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
    private var currentSoundConfig: SoundConfig? = null
    private var currentVibrationMode: VibrationMode = VibrationMode.OFF
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

    override fun playAlarm(signalConfig: AlarmSignalConfig, isSafetyCritical: Boolean) {
        playAlarm(
            soundConfig = signalConfig.soundConfig,
            vibrationMode = signalConfig.vibrationMode,
            overrideDnd = signalConfig.overrideDnd,
            isSafetyCritical = isSafetyCritical
        )
    }

    override fun playAlarm(
        soundConfig: SoundConfig?,
        vibrationMode: VibrationMode,
        overrideDnd: Boolean,
        isSafetyCritical: Boolean
    ) {
        synchronized(mutex) {
            stopAlarmInternal()
            currentSoundConfig = soundConfig
            currentVibrationMode = vibrationMode
            currentIsSafetyCritical = isSafetyCritical
            isPreviewPlaying = false

            startAlarmInternal(soundConfig, vibrationMode, isLooping = true)
        }
    }

    override fun playPreview(signalConfig: AlarmSignalConfig, durationMs: Long) {
        playPreview(
            soundConfig = signalConfig.soundConfig,
            vibrationMode = signalConfig.vibrationMode,
            durationMs = durationMs
        )
    }

    override fun playPreview(soundConfig: SoundConfig?, vibrationMode: VibrationMode, durationMs: Long) {
        synchronized(mutex) {
            stopAlarmInternal()
            currentSoundConfig = soundConfig
            currentVibrationMode = vibrationMode
            currentIsSafetyCritical = false
            isPreviewPlaying = true

            startAlarmInternal(soundConfig, vibrationMode, isLooping = true)

            handler.postDelayed(stopPreviewRunnable, durationMs)
        }
    }

    override fun updateVolume(volume: Int) {
        synchronized(mutex) {
            val roundedVol = volume.coerceIn(0, 100)
            currentSoundConfig = currentSoundConfig?.copy(volume = roundedVol)
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

    private fun startAlarmInternal(soundConfig: SoundConfig?, vibrationMode: VibrationMode, isLooping: Boolean) {
        requestAudioFocus()

        // 1. Audio Playback
        if (soundConfig != null && soundConfig.volume > 0) {
            try {
                val soundUri = resolveSoundUri(soundConfig.soundUri)
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
                        val floatVol = soundConfig.volume.coerceIn(0, 100) / 100f
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
                                val floatVol = soundConfig.volume.coerceIn(0, 100) / 100f
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
        startVibration(vibrationMode)

        isPlayingActive = true
    }

    @SuppressLint("MissingPermission")
    private fun startVibration(mode: VibrationMode) {
        val currentVibrator = vibrator ?: return
        if (!currentVibrator.hasVibrator()) return

        try {
            val pattern = when (mode) {
                VibrationMode.OFF -> null
                VibrationMode.SHORT -> longArrayOf(0, 300, 200, 300)
                VibrationMode.LONG -> longArrayOf(0, 800, 400, 800)
                VibrationMode.CONTINUOUS -> longArrayOf(0, 1000, 500)
            }

            if (pattern == null) {
                currentVibrator.cancel()
                return
            }

            val repeat = if (mode == VibrationMode.CONTINUOUS || mode == VibrationMode.LONG) 0 else -1

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val vibrationEffect = VibrationEffect.createWaveform(pattern, repeat)
                val attributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                currentVibrator.vibrate(vibrationEffect, attributes)
            } else {
                @Suppress("DEPRECATION")
                currentVibrator.vibrate(pattern, repeat)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting vibration", e)
        }
    }

    private fun resolveSoundUri(uriString: String?): Uri? {
        if (uriString.isNullOrEmpty()) {
            return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }
        return try {
            uriString.toUri()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse custom soundUri '$uriString', falling back to system alarm sound", e)
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        }
    }

    private fun requestAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

                val focusReq = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(audioAttributes)
                    .setOnAudioFocusChangeListener(audioFocusChangeListener)
                    .build()

                audioFocusRequest = focusReq
                audioManager.requestAudioFocus(focusReq)
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(
                    audioFocusChangeListener,
                    AudioManager.STREAM_ALARM,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting audio focus", e)
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopAlarmInternal() {
        handler.removeCallbacks(stopPreviewRunnable)

        mediaPlayer?.let {
            try {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping MediaPlayer", e)
            }
        }
        mediaPlayer = null

        vibrator?.let {
            try {
                it.cancel()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping Vibrator", e)
            }
        }

        abandonAudioFocus()

        isPlayingActive = false
        currentSoundConfig = null
        currentVibrationMode = VibrationMode.OFF
        currentIsSafetyCritical = false
        isPreviewPlaying = false
    }

    private fun pauseAudioOnly() {
        mediaPlayer?.let {
            try {
                if (it.isPlaying) {
                    it.pause()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error pausing MediaPlayer", e)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun pauseAudioAndVibration() {
        pauseAudioOnly()
        vibrator?.let {
            try {
                it.cancel()
            } catch (e: Exception) {
                Log.e(TAG, "Error pausing Vibrator", e)
            }
        }
    }

    private fun resumeAudioAndVibration() {
        mediaPlayer?.let {
            try {
                if (!it.isPlaying) {
                    it.start()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error resuming MediaPlayer", e)
            }
        }
        startVibration(currentVibrationMode)
    }

    private fun abandonAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
                audioFocusRequest = null
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(audioFocusChangeListener)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error abandoning audio focus", e)
        }
    }

    companion object {
        private const val TAG = "AlarmPlayerManager"
    }
}