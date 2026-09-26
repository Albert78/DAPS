package de.dh.daps

import android.app.Application
import de.dh.daps.common.model.CgmConnectionDescriptor
import de.dh.daps.common.model.PluginManager
import de.dh.daps.common.model.PumpConnectionDescriptor
import de.dh.daps.common.navigation.FeatureNavGraph
import de.dh.daps.common.navigation.NavigationViewModel
import de.dh.daps.core.SystemRegistry
import de.dh.daps.plugin.glucose.receiver.ExternalSourceType
import de.dh.daps.plugin.glucose.receiver.ReceiverGlucoseDriver
import de.dh.daps.plugin.glucose.receiver.ReceiverGlucosePlugin
import de.dh.daps.plugin.pump.SampleInsulinPumpDriver
import de.dh.daps.plugin.pump.SampleInsulinPumpPlugin
import kotlinx.coroutines.runBlocking

/**
 * Registers all plugins and drivers available for the productive flavor with the [PluginManager].
 */
fun registerPlugins(pluginManager: PluginManager, application: Application) {
    pluginManager.addPlugin(ReceiverGlucosePlugin(application, ExternalSourceType.xDrip5Min))
    pluginManager.addPlugin(ReceiverGlucoseDriver(application))
    pluginManager.addPlugin(SampleInsulinPumpPlugin())
    pluginManager.addPlugin(SampleInsulinPumpDriver())
}

/**
 * Connects initial default hardware devices if no device configuration exists yet in [DeviceManagementRepository].
 */
fun setupInitialDevices(registry: SystemRegistry) {
    runBlocking {
        val deviceRepository = registry.deviceManagementRepository
        val connectionManager = registry.deviceConnectionManager

        if (deviceRepository.getGlucoseSourceDescriptor() == null) {
            val cgmDescriptor = CgmConnectionDescriptor(
                driverId = "de.dh.daps.plugin.glucose.receiver",
                sourceId = ExternalSourceType.xDrip5Min.name,
                displayName = "xDrip Receiver (5 Min)"
            )
            connectionManager.connectGlucoseSource(cgmDescriptor)
        }

        if (deviceRepository.getPumpDescriptor() == null) {
            val pumpDescriptor = PumpConnectionDescriptor(
                driverId = "de.dh.daps.plugin.pump.sample",
                deviceId = "sample_pump_1",
                displayName = "Sample Insulin Pump"
            )
            connectionManager.connectPump(pumpDescriptor)
        }
    }
}

fun getExtraNavGraphs(
    navViewModel: NavigationViewModel,
): List<FeatureNavGraph> {
    return emptyList()
}