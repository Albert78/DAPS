package de.dh.daps.common.infrastructure

import android.content.Context
import de.dh.daps.common.domain.calculation.CarbsInsulinCalculator

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