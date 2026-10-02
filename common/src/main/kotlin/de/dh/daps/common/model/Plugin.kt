package de.dh.daps.common.model

interface Plugin {
    val pluginId: String
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