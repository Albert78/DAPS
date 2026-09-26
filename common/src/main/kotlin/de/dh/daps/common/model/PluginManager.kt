package de.dh.daps.common.model

/**
 * Lightweight plugin system for DAPS. Plugin instances are added at the system setup and
 * read during the initialization of SystemRegistry. The list of plugins just acts as a generic
 * container to put anything which needs lifecycle callbacks and/or which is a dynamic component
 * like GlucoseSource- or Pump-Drivers.
 */
interface PluginManager {
    fun addPlugin(plugin: Plugin)
    fun getPlugins(): List<Plugin>

    // TODO: Permissions handling
    fun checkSelfPermissions(androidPermissions: Collection<String>): Collection<String>
    fun triggerUpdatesAfterPermissionsChange()
}