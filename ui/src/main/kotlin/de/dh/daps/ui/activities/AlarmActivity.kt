package de.dh.daps.ui.activities

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import de.dh.daps.common.model.data.AlarmType
import de.dh.daps.common.model.data.GlucoseUnit
import de.dh.daps.core.system.RegistryProvider
import de.dh.daps.ui.common.theme.AppTheme
import de.dh.daps.ui.screens.alarm.FullScreenAlarmScreen

class AlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        turnOnScreenAndShowOnLock()

        val registry = (application as RegistryProvider).registry

        setContent {
            val activeFiringAlarm by registry.alarmEvaluator.activeFiringAlarm.collectAsState()
            val currentBgReading by registry.glucoseRepository.currentBg.collectAsState()
            val glucoseUnit by registry.appPreferencesRepository.glucoseUnit.collectAsState()

            val alarmType = activeFiringAlarm ?: AlarmType.CRITICAL_LOW_BG

            AppTheme(darkTheme = true) {
                FullScreenAlarmScreen(
                    alarmType = alarmType,
                    bgValue = currentBgReading?.value,
                    glucoseUnit = glucoseUnit ?: GlucoseUnit.MG_DL,
                    onSnooze = { minutes ->
                        registry.alarmSnoozeManager.snoozeAlarm(alarmType, minutes)
                        registry.alarmPlayerManager.stopAlarm()
                        finish()
                    },
                    onDismiss = {
                        registry.alarmSnoozeManager.snoozeAlarm(alarmType, 15)
                        registry.alarmPlayerManager.stopAlarm()
                        finish()
                    }
                )
            }
        }
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