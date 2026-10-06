package de.dh.daps.core.plugin

import android.content.Context
import de.dh.daps.common.model.PluginContext
import de.dh.daps.common.model.PluginManager
import de.dh.daps.common.model.PluginPreferences
import de.dh.daps.common.model.calculation.CarbsInsulinCalculator
import de.dh.daps.common.service.SystemWakeService
import de.dh.daps.common.service.TimeService

/**
 * Concrete implementation of [PluginContext] passed to individual plugins.
 * Holds references to common services, an isolated [PluginPreferences] instance, and the [SystemWakeService].
 */
class PluginContextImpl(
    override val appContext: Context,
    override val pluginManager: PluginManager,
    override val timeService: TimeService,
    override val carbsInsulinCalculator: CarbsInsulinCalculator,
    override val preferences: PluginPreferences,
    override val wakeService: SystemWakeService,
) : PluginContext