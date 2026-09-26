package de.dh.daps.common.model

/**
 * Abstraction of functionality which is maintained independently of the main app.
 * Plugins declare their own needed permissions.
 *
 * Plugins are static components of an assembled app.
 * The set of active plugins is determined by build flavors.
 */
interface Plugin {
    val name: String
    val neededPermissions: Collection<String>

    /**
     * Called early during application creation before core system services start initialization.
     * Allows plugins to receive the [PluginContext] reference and configure internal state or handlers.
     */
    fun setup(context: PluginContext) {}

    /**
     * Called after all core system services, managers, and repositories are fully initialized.
     * Allows plugins to start active background processes or execute post-initialization tasks.
     */
    fun initialize(context: PluginContext) {}
}