package de.dh.daps

import android.app.Application
import de.dh.daps.common.model.PluginManager
import de.dh.daps.common.navigation.FeatureNavGraph
import de.dh.daps.common.navigation.NavigationViewModel
import de.dh.daps.plugin.glucose.receiver.ReceiverGlucosePlugin
import de.dh.pump.danai.ui.DanaIInsulinPumpDriver

/**
 * Registers all plugins and drivers available for the productive flavor with the [PluginManager].
 */
fun registerPlugins(pluginManager: PluginManager, application: Application) {
    pluginManager.addPlugin(ReceiverGlucosePlugin(application))
    pluginManager.addPlugin(DanaIInsulinPumpDriver())
}

fun getExtraNavGraphs(
    navViewModel: NavigationViewModel,
): List<FeatureNavGraph> {
    return emptyList()
}