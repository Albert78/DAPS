package de.dh.daps.common.model

import android.content.Context
import de.dh.daps.common.model.calculation.CarbsInsulinCalculator
import de.dh.daps.common.service.SystemWakeService
import de.dh.daps.common.service.TimeService

/**
 * Context provided to [Plugin]s during setup and initialization.
 * Exposes core application services, an isolated preferences view, and the system wake service.
 */
interface PluginContext {
    val appContext: Context
    val pluginManager: PluginManager
    val timeService: TimeService
    val carbsInsulinCalculator: CarbsInsulinCalculator
    val preferences: PluginPreferences
    val wakeService: SystemWakeService
}