package de.dh.daps

import android.app.Application
import de.dh.daps.common.model.PluginManager
import de.dh.daps.common.navigation.FeatureNavGraph
import de.dh.daps.common.navigation.NavigationViewModel
import de.dh.daps.plugin.simbody.SimBodyPlugin
import de.dh.daps.plugin.simbody.ui.SimBodyNavGraph

private var simBodyPlugin: SimBodyPlugin? = null

/**
 * Registers all plugins provided by the [SimBodyPlugin] package with the [PluginManager].
 */
fun registerPlugins(pluginManager: PluginManager, application: Application) {
    val plugin = SimBodyPlugin(application)
    simBodyPlugin = plugin
    pluginManager.addPlugin(plugin)
    pluginManager.addPlugin(plugin.glucoseSourceDriver)
    pluginManager.addPlugin(plugin.pumpDriver)
}

fun getExtraNavGraphs(
    navViewModel: NavigationViewModel,
): List<FeatureNavGraph> {
    val bodyModel = simBodyPlugin?.bodyModel
    return listOf(SimBodyNavGraph(navViewModel, bodyModel))
}