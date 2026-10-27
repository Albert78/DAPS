package de.dh.daps.ui.activities

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import de.dh.daps.core.infrastructure.RegistryProvider
import de.dh.daps.ui.common.theme.AppTheme
import de.dh.daps.ui.screens.alerts.FullScreenAlarmScreen

class AlarmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        turnOnScreenAndShowOnLock()

        val registry = (application as RegistryProvider).registry

        setContent {
            val activeAlarmGroup by registry.alarmEvaluator.activeAlarmGroup.collectAsState()
            val currentBgReading by registry.glucoseRepository.currentBg.collectAsState()
            val glucoseUnit by registry.appPreferencesRepository.glucoseUnit.collectAsState()

            val group = activeAlarmGroup
            if (group == null) {
                finish()
                return@setContent
            }

            AppTheme(darkTheme = true) {
                FullScreenAlarmScreen(
                    alarmGroup = group,
                    bgValue = currentBgReading?.value,
                    glucoseUnit = glucoseUnit,
                    onSnoozeAll = { minutes ->
                        registry.alarmSnoozeManager.snoozeAlarms(group.allAlarms, minutes)
                        finish()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val registry = (application as RegistryProvider).registry
        val activeGroup = registry.alarmEvaluator.activeAlarmGroup.value
        val activeConfig = registry.alarmEvaluator.activeFiringConfig.value
        if (activeGroup != null && activeConfig != null) {
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
}