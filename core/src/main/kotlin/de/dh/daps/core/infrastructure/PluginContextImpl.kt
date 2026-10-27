package de.dh.daps.core.infrastructure

import android.content.Context
import de.dh.daps.common.infrastructure.PluginContext
import de.dh.daps.common.infrastructure.PluginManager
import de.dh.daps.common.infrastructure.PluginPreferences
import de.dh.daps.common.domain.calculation.CarbsInsulinCalculator
import de.dh.daps.common.infrastructure.ReminderManager
import de.dh.daps.common.infrastructure.SystemWakeService
import de.dh.daps.common.infrastructure.TimeService

/**
 * Concrete implementation of [PluginContext] passed to individual plugins.
 * Holds references to common services, an isolated [PluginPreferences] instance, the [SystemWakeService], and [ReminderManager].
 */
class PluginContextImpl(
    override val appContext: Context,
    override val pluginManager: PluginManager,
    override val timeService: TimeService,
    override val carbsInsulinCalculator: CarbsInsulinCalculator,
    override val preferences: PluginPreferences,
    override val wakeService: SystemWakeService,
    override val reminderManager: ReminderManager,
) : PluginContext