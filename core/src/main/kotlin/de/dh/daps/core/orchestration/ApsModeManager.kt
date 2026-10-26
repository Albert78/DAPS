package de.dh.daps.core.orchestration

import android.util.Log
import de.dh.daps.common.model.ApsMode
import de.dh.daps.common.model.data.Timestamp
import de.dh.daps.core.cgm.GlucoseSourceManager
import de.dh.daps.core.therapy.TherapyManager
import de.dh.daps.core.pump.PumpManager
import de.dh.daps.core.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

/**
 * Manages APS Mode resolution, hardware capability validation, settings persistence,
 * and resume grace period tracking.
 */
class ApsModeManager(
    private val glucoseSourceManager: GlucoseSourceManager,
    private val pumpManager: PumpManager,
    private val settingsRepository: SettingsRepository,
    private val scope: CoroutineScope
) {
    private val _apsMode = MutableStateFlow(ApsMode.Suspend)
    val apsMode: StateFlow<ApsMode> = _apsMode.asStateFlow()

    private val _resumeRequestedAt = MutableStateFlow<Timestamp?>(null)
    val resumeRequestedAt: StateFlow<Timestamp?> = _resumeRequestedAt.asStateFlow()

    val availableApsModes: StateFlow<List<ApsMode>> = combine(
        glucoseSourceManager.activeGlucoseSource,
        pumpManager.activeInsulinPump
    ) { glucoseSource, pump ->
        getAvailableApsModes(
            hasGlucoseSource = glucoseSource != null,
            hasPump = pump != null
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = ApsMode.entries
    )

    private fun getAvailableApsModes(hasGlucoseSource: Boolean, hasPump: Boolean): List<ApsMode> =
        if (hasGlucoseSource && hasPump) {
            ApsMode.entries
        } else {
            ApsMode.entries.filter { it != ApsMode.AutoCorrection }
        }

    fun setApsMode(mode: ApsMode, therapyManager: TherapyManager?) {
        val allowed = availableApsModes.value
        val validMode = if (mode in allowed) {
            mode
        } else {
            val fallback = allowed.lastOrNull() ?: ApsMode.Suspend
            Log.w(TAG, "Attempted to set unavailable APS mode $mode. Using $fallback instead.")
            fallback
        }

        val previousMode = _apsMode.value
        _apsMode.value = validMode

        if (previousMode == ApsMode.Suspend && validMode != ApsMode.Suspend) {
            _resumeRequestedAt.value = Timestamp.now()
        } else if (validMode == ApsMode.Suspend) {
            _resumeRequestedAt.value = null
        }

        val shouldBeSuspended = (validMode == ApsMode.Suspend)
        therapyManager?.setSuspend(shouldBeSuspended)

        scope.launch {
            val currentSettings = settingsRepository.getCurrentSettings()
            if (currentSettings != null) {
                settingsRepository.updateCurrentSettings(currentSettings.copy(apsMode = validMode))
            }
        }
    }

    fun startObservingSettingsAndAvailability(
        initScope: CoroutineScope,
        deviceInitializationTimeoutMs: Long
    ) {
        initScope.launch {
            val currentSettings = runCatching { settingsRepository.getCurrentSettings() }.getOrNull()
            if (currentSettings != null) {
                val targetMode = currentSettings.apsMode
                val allowed = withTimeoutOrNull(deviceInitializationTimeoutMs.milliseconds) {
                    availableApsModes.first { modes -> targetMode in modes }
                } ?: availableApsModes.value

                val initialMode = if (targetMode in allowed) {
                    targetMode
                } else {
                    val fallback = allowed.lastOrNull() ?: ApsMode.Suspend
                    Log.w(TAG, "Restored APS mode $targetMode is not allowed with current devices. Falling back to $fallback.")
                    settingsRepository.updateCurrentSettings(currentSettings.copy(apsMode = fallback))
                    fallback
                }
                _apsMode.value = initialMode
            }

            settingsRepository.observeCurrentSettings().drop(1).collect { settings ->
                if (settings != null) {
                    val allowed = availableApsModes.value
                    if (settings.apsMode in allowed) {
                        _apsMode.value = settings.apsMode
                    } else {
                        val fallback = allowed.lastOrNull() ?: ApsMode.Suspend
                        if (_apsMode.value != fallback) {
                            setApsMode(fallback, null)
                        }
                    }
                }
            }
        }

        initScope.launch {
            availableApsModes.collect { allowed ->
                val current = _apsMode.value
                if (current !in allowed) {
                    val fallback = allowed.lastOrNull() ?: ApsMode.Suspend
                    Log.w(TAG, "Current APS mode $current is no longer available. Downgrading to $fallback.")
                    setApsMode(fallback, null)
                }
            }
        }
    }

    fun reset() {
        _apsMode.value = ApsMode.Suspend
        _resumeRequestedAt.value = null
    }

    companion object {
        private val TAG = ApsModeManager::class.simpleName
    }
}