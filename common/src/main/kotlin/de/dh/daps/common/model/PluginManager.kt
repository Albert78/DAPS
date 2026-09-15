package de.dh.daps.common.model

interface PluginManager {
    fun addPlugin(plugin: Plugin)
    fun getPlugins(): List<Plugin>
    fun checkSelfPermissions(androidPermissions: Collection<String>): Collection<String>
    fun triggerUpdatesAfterPermissionsChange()
}