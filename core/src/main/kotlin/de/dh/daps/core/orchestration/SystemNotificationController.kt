package de.dh.daps.core.orchestration

import android.app.Notification
import de.dh.daps.common.repository.AppPreferencesRepository
import de.dh.daps.common.domain.model.therapy.Tick
import de.dh.daps.common.infrastructure.ReminderManager
import de.dh.daps.common.infrastructure.TickHandler
import de.dh.daps.common.infrastructure.TickPriority
import de.dh.daps.common.infrastructure.TimeService
import de.dh.daps.core.repository.GlucoseRepository
import de.dh.daps.core.infrastructure.AndroidNotifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * Coordinates foreground notifications, recommendation popups,
 * and reminder alerts.
 */
class SystemNotificationController(
    private val androidNotifications: AndroidNotifications
) {
    fun createNotificationChannels() {
        androidNotifications.createNotificationChannels()
    }

    fun createForegroundServiceNotification(glucoseRepository: GlucoseRepository): Notification {
        return androidNotifications.createMainAppNotification(glucoseRepository)
    }

    fun updateMainAppNotification(glucoseRepository: GlucoseRepository) {
        androidNotifications.updateMainAppNotification(glucoseRepository)
    }

    fun createTickHandler(glucoseRepository: GlucoseRepository): TickHandler {
        return object : TickHandler {
            override suspend fun onTick(tick: Tick) {
                androidNotifications.updateMainAppNotification(glucoseRepository)
            }
        }
    }

    fun registerTickHandler(timeService: TimeService, handler: TickHandler) {
        timeService.registerTickHandler(TickPriority.UI, handler, "Notifications")
    }

    fun unregisterTickHandler(timeService: TimeService, handler: TickHandler) {
        timeService.unregisterTickHandler(handler)
    }

    fun startObserving(
        scope: CoroutineScope,
        glucoseRepository: GlucoseRepository,
        appPreferencesRepository: AppPreferencesRepository,
        recommendationManager: RecommendationManager,
        reminderManager: ReminderManager
    ) {
        scope.launch {
            appPreferencesRepository.glucoseUnit.drop(1).collect {
                androidNotifications.updateMainAppNotification(glucoseRepository)
            }
        }

        scope.launch {
            recommendationManager.recommendations.collect { recommendations ->
                val carbRec = recommendations.filterIsInstance<ApsRecommendation.Carbs>().firstOrNull()
                if (carbRec != null) {
                    androidNotifications.showRecommendationNotification(carbRec)
                } else {
                    androidNotifications.cancelRecommendationNotification()
                }

                val dueDeferredBoluses = recommendations
                    .filterIsInstance<ApsRecommendation.Bolus>()
                    .flatMap { it.includedDeferredBoluses.orEmpty() }

                if (dueDeferredBoluses.isNotEmpty()) {
                    androidNotifications.showDeferredBolusRecommendationNotification(dueDeferredBoluses)
                }
            }
        }

        scope.launch {
            reminderManager.dueReminders.collect { reminder ->
                androidNotifications.showReminderNotification(reminder)
            }
        }
    }
}