package de.dh.daps.ui.activities

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import de.dh.daps.core.infrastructure.RegistryProvider
import de.dh.daps.ui.common.theme.AppTheme
import de.dh.daps.ui.screens.fullscreenalerts.FullScreenAlarmScreen

class AlarmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        turnOnScreenAndShowOnLock()

        val registry = (application as RegistryProvider).registry

        setContent {
            val activeAlarmGroup by registry.alarmEvaluator.activeAlarmGroup.collectAsState()
            val glucoseUnit by registry.appPreferencesRepository.glucoseUnit.collectAsState()

            val group = activeAlarmGroup
            if (group == null) {
                finish()
                return@setContent
            }

            AppTheme(darkTheme = true) {
                FullScreenAlarmScreen(
                    alarmGroup = group,
                    glucoseUnit = glucoseUnit,
                    onSnoozeAll = { minutes ->
                        registry.alarmSnoozeManager.snoozeAlarms(group.allAlarmTypes, minutes)
                        finish()
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        val registry = (application as RegistryProvider).registry
        val activeGroup = registry.alarmEvaluator.activeAlarmGroup.value
        val activeConfig = registry.alarmEvaluator.activeFiringConfig.value
        val isMuted = intent?.getBooleanExtra(EXTRA_MUTED, false) == true

        if (isMuted) {
            registry.alarmPlayerManager.stopAlarm()
        } else if (activeGroup != null && activeConfig != null) {
            val primaryAlarm = activeGroup.primaryAlarm
            registry.alarmPlayerManager.playAlarm(activeConfig, isSafetyCritical = primaryAlarm.isSafetyCritical)
        }
    }

    override fun onPause() {
        super.onPause()
        val registry = (application as RegistryProvider).registry
        registry.alarmPlayerManager.stopAlarm()
    }

    private fun turnOnScreenAndShowOnLock() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
    }

    companion object {
        const val EXTRA_MUTED = "de.dh.daps.ui.activities.EXTRA_MUTED"

        fun createStartIntent(context: Context, muted: Boolean = false): Intent {
            return Intent(context, AlarmActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_MUTED, muted)
            }
        }
    }
}