package de.dh.daps

import android.app.Application
import de.dh.daps.common.model.GlucoseSourceConnectionDescriptor
import de.dh.daps.common.model.PluginManager
import de.dh.daps.common.model.PumpConnectionDescriptor
import de.dh.daps.common.navigation.FeatureNavGraph
import de.dh.daps.common.navigation.NavigationViewModel
import de.dh.daps.core.SystemRegistry
import de.dh.daps.plugin.glucose.receiver.ExternalSourceType
import de.dh.daps.plugin.glucose.receiver.ReceiverGlucosePlugin
import de.dh.daps.plugin.pump.SampleInsulinPumpDriver
import kotlinx.coroutines.runBlocking

/**
 * Registers all plugins and drivers available for the productive flavor with the [PluginManager].
 */
fun registerPlugins(pluginManager: PluginManager, application: Application) {
    pluginManager.addPlugin(ReceiverGlucosePlugin(application))
    pluginManager.addPlugin(SampleInsulinPumpDriver())
}

fun getExtraNavGraphs(
    navViewModel: NavigationViewModel,
): List<FeatureNavGraph> {
    return emptyList()
}