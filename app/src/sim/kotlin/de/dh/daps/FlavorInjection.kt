package de.dh.daps

import android.app.Application
import de.dh.daps.common.model.CgmConnectionDescriptor
import de.dh.daps.common.model.PluginManager
import de.dh.daps.common.model.PumpConnectionDescriptor
import de.dh.daps.common.navigation.FeatureNavGraph
import de.dh.daps.common.navigation.NavigationViewModel
import de.dh.daps.core.SystemRegistry
import de.dh.daps.plugin.simbody.SimBodyPlugin
import de.dh.daps.plugin.simbody.ui.SimBodyNavGraph
import kotlinx.coroutines.runBlocking

private var simBodyPlugin: SimBodyPlugin? = null

/**
 * Registers all plugins provided by the [SimBodyPlugin] package with the [PluginManager].
 */
fun registerPlugins(pluginManager: PluginManager, application: Application) {
    val plugin = SimBodyPlugin(application)
    simBodyPlugin = plugin
    pluginManager.addPlugin(plugin)
    pluginManager.addPlugin(plugin.cgmDriver)
    pluginManager.addPlugin(plugin.pumpDriver)
}

/**
 * Connects initial simulation devices if no device configuration exists yet in [DeviceManagementRepository].
 */
// TODO: This method will be removed once we have a proper initial system setup
fun setupInitialDevices(registry: SystemRegistry) {
    val plugin = simBodyPlugin
    runBlocking {
        val deviceRepository = registry.deviceManagementRepository
        val connectionManager = registry.deviceConnectionManager

        if (deviceRepository.getGlucoseSourceDescriptor() == null && plugin != null) {
            val cgmDescriptor = CgmConnectionDescriptor(
                driverId = plugin.cgmDriver.driverId,
                sourceId = "sim_body_glucose",
                displayName = "SimBody Glucose Source"
            )
            connectionManager.connectGlucoseSource(cgmDescriptor)
        }

        if (deviceRepository.getPumpDescriptor() == null && plugin != null) {
            val pumpDescriptor = PumpConnectionDescriptor(
                driverId = plugin.pumpDriver.driverId,
                deviceId = "sim_body_pump",
                displayName = "SimBody Insulin Pump"
            )
            connectionManager.connectPump(pumpDescriptor)
        }
    }
}

fun getExtraNavGraphs(
    navViewModel: NavigationViewModel,
): List<FeatureNavGraph> {
    val bodyModel = simBodyPlugin?.bodyModel
    return listOf(SimBodyNavGraph(navViewModel, bodyModel))
}