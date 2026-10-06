package de.dh.daps.core.plugin

import android.content.Context
import de.dh.daps.common.model.PluginContext
import de.dh.daps.common.model.PluginManager
import de.dh.daps.common.model.PluginPreferences
import de.dh.daps.common.model.calculation.CarbsInsulinCalculator
import de.dh.daps.common.model.data.TimeService
import de.dh.daps.core.SystemRegistry

/**
 * Concrete implementation of [PluginContext] passed to individual plugins.
 * Holds references to common services, an isolated [PluginPreferences] instance, and the [SystemRegistry].
 */
class PluginContextImpl(
    override val appContext: Context,
    override val pluginManager: PluginManager,
    override val timeService: TimeService,
    override val carbsInsulinCalculator: CarbsInsulinCalculator,
    override val preferences: PluginPreferences,
    override val registry: SystemRegistry,
) : PluginContext