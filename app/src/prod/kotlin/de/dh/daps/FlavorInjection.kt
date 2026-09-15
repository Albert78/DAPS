package de.dh.daps

import android.app.Application
import de.dh.daps.common.model.PluginManager
import de.dh.daps.common.navigation.FeatureNavGraph
import de.dh.daps.common.navigation.NavigationViewModel
import de.dh.daps.core.SystemRegistry
import de.dh.daps.plugin.glucose.receiver.ExternalSourceType
import de.dh.daps.plugin.glucose.receiver.ReceiverGlucosePlugin
import de.dh.daps.plugin.pump.SampleInsulinPumpPlugin

fun setupSystem(registry: SystemRegistry, pluginManager: PluginManager, application: Application) {
    val pumpManager = registry.pumpManager
    val glucosePlugin = ReceiverGlucosePlugin(
        application,
        ExternalSourceType.xDrip5Min
    )
    pluginManager.addPlugin(glucosePlugin)
    registry.glucoseSourceManager.glucoseSource = glucosePlugin
    val pumpPlugin = SampleInsulinPumpPlugin()
    pumpManager.insulinPump = pumpPlugin
}

fun getExtraNavGraphs(
    navViewModel: NavigationViewModel
): List<FeatureNavGraph> {
    return emptyList()
}