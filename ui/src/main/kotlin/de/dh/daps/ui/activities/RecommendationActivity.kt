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
import androidx.lifecycle.lifecycleScope
import de.dh.daps.common.domain.model.alarm.getDefaultAlertProfiles
import de.dh.daps.common.domain.model.data.CarbsUnit
import de.dh.daps.core.alerts.getConfigFor
import de.dh.daps.core.infrastructure.RegistryProvider
import de.dh.daps.ui.common.theme.AppTheme
import de.dh.daps.ui.screens.recommendations.RecommendationScreen
import kotlinx.coroutines.launch

class RecommendationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        turnOnScreenAndShowOnLock()

        val registry = (application as RegistryProvider).registry

        setContent {
            val recommendations by registry.recommendationManager.recommendations.collectAsState()
            val carbsUnit by registry.appPreferencesRepository.carbsUnit.collectAsState(initial = CarbsUnit.GRAMS)

            if (recommendations.isEmpty) {
                finish()
                return@setContent
            }

            AppTheme(darkTheme = true) {
                RecommendationScreen(
                    recommendations = recommendations,
                    carbsUnit = carbsUnit,
                    onApplyRecommendation = { rec ->
                        lifecycleScope.launch {
                            val intent = MainActivity.createRecommendationIntent(this@RecommendationActivity, rec)
                            startActivity(intent)
                            registry.recommendationManager.removeRecommendation(rec)
                            if (registry.recommendationManager.recommendations.value.isEmpty) {
                                finish()
                            }
                        }
                    },
                    onDismissRecommendation = { rec ->
                        lifecycleScope.launch {
                            registry.recommendationManager.removeRecommendation(rec)
                            if (registry.recommendationManager.recommendations.value.isEmpty) {
                                finish()
                            }
                        }
                    },
                    onDismissAll = {
                        finish()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val registry = (application as RegistryProvider).registry
        lifecycleScope.launch {
            val activeProfile = registry.therapyRepository.getCurrentTherapySettingsOrNull()?.effectiveAlertProfile
                ?: getDefaultAlertProfiles(this@RecommendationActivity).first { it.isDefault }

            val recommendations = registry.recommendationManager.recommendations.value
            val firstActiveRec = listOfNotNull(recommendations.carbs, recommendations.bolus, recommendations.tempBasal).firstOrNull()
            if (firstActiveRec != null) {
                val config = activeProfile.getConfigFor(firstActiveRec)
                if (config.enabled) {
                    registry.alarmPlayerManager.playAlarm(config)
                }
            }
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
        fun createStartIntent(context: Context): Intent {
            return Intent(context, RecommendationActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
        }

        fun start(context: Context) {
            context.startActivity(createStartIntent(context))
        }
    }
}