package de.dh.daps.common.model

import android.content.Context
import de.dh.daps.common.model.calculation.CarbsInsulinCalculator
import de.dh.daps.common.service.ReminderManager
import de.dh.daps.common.service.SystemWakeService
import de.dh.daps.common.service.TimeService

/**
 * Context provided to [Plugin]s during setup and initialization.
 * Exposes core application services, an isolated preferences view, system wake service, and reminder manager.
 */
interface PluginContext {
    val appContext: Context
    val pluginManager: PluginManager
    val timeService: TimeService
    val carbsInsulinCalculator: CarbsInsulinCalculator
    val preferences: PluginPreferences
    val wakeService: SystemWakeService
    val reminderManager: ReminderManager
}