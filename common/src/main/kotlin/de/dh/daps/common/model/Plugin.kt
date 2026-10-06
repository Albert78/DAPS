package de.dh.daps.common.model

/**
 * Generic interface bridging the hardcoded core system and dynamically added components and aspects.
 *
 * ### Overview & Architecture
 * A plugin encapsulates dynamic capabilities — such as hardware drivers (e.g., CGM sensors or insulin pumps),
 * simulators, external data receivers, or custom UI providers — without requiring the core system
 * to have hardcoded knowledge of specific implementations.
 *
 * ### Plugin Registration & Querying
 * - The [PluginManager] maintains a generic list of registered [Plugin] instances during system setup.
 * - The [PluginManager] itself acts as an agnostic container and does not directly know what specific tasks
 *   individual plugins perform.
 * - The core system queries plugin functionality at specific extension points by filtering registered
 *   plugins for specialized role interfaces, for example:
 *   ```kotlin
 *   val pumpDrivers = pluginManager.getPlugins().filterIsInstance<InsulinPumpDriver>()
 *   val glucoseDrivers = pluginManager.getPlugins().filterIsInstance<GlucoseSourceDriver>()
 *   ```
 *
 * ### Lifecycle & Features
 * Plugins can participate in application lifecycle events:
 * - [setup]: Called early to provide a [PluginContext] (granting access to isolated preferences, time service, etc.).
 * - [initialize]: Called once core repositories, managers, and services are fully started.
 * - Reset and seeding hooks ([onResetToFactorySettings], [onSeedDefaultData]).
 * - Backup and restore hooks ([exportBackupData], [importBackupData]).
 */
import de.dh.daps.common.ui.UiText

interface Plugin {
    val pluginId: String
    val pluginDisplayName: UiText
        get() = (this as? InsulinPumpDriver)?.driverDisplayName
            ?: (this as? GlucoseSourceDriver)?.driverDisplayName
            ?: UiText.DynamicString(pluginId)

    val neededPermissions: Collection<String>
        get() = emptyList()

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

    /**
     * Called when application permissions have changed (e.g. granted by the user).
     * Allows plugins to restart background services or retry tasks that depend on permissions.
     */
    fun onPermissionsChanged() {}

    /**
     * Called when the system is reset to factory settings or data is cleared.
     * Allows plugins to clear internal databases, caches, and reset state.
     */
    suspend fun onResetToFactorySettings() {}

    /**
     * Called when the system seeds default or demo data during initial setup.
     * Allows plugins to populate initial default values or seed demo state.
     */
    suspend fun onSeedDefaultData() {}

    /**
     * Called during backup export. Returns a map of relative file keys (e.g., "simbody.json")
     * to JSON content strings, or null if the plugin has no backup data to export.
     */
    suspend fun exportBackupData(includeHistory: Boolean, includeDiagnostics: Boolean): Map<String, String>? = null

    /**
     * Called during backup import. Receives a map of relative file keys to JSON content strings
     * exported by this plugin.
     */
    suspend fun importBackupData(
        backupData: Map<String, String>,
        includeHistory: Boolean,
        includeDiagnostics: Boolean
    ) {}
}