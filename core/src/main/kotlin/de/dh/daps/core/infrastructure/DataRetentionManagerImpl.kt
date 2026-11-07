package de.dh.daps.core.infrastructure

import de.dh.daps.common.domain.model.retention.DataRetentionCategory
import de.dh.daps.common.domain.model.retention.RetentionPolicySettings
import de.dh.daps.common.infrastructure.PluginManager
import de.dh.daps.common.infrastructure.TimeService
import de.dh.daps.core.repository.SystemMetricsRepository
import de.dh.daps.core.repository.db.AppDatabase
import de.dh.daps.core.repository.db.entities.toEntity
import de.dh.daps.core.repository.db.entities.toModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DataRetentionManagerImpl(
    private val appDatabase: AppDatabase,
    private val systemMetricsRepository: SystemMetricsRepository,
    private val pluginManager: PluginManager,
    private val timeService: TimeService,
    private val scope: CoroutineScope
) : DataRetentionManager {

    private val retentionDao = appDatabase.retentionSettingsDao()
    private val _settings = MutableStateFlow(RetentionPolicySettings())
    override val settings: StateFlow<RetentionPolicySettings> = _settings.asStateFlow()

    init {
        scope.launch {
            val existing = retentionDao.getSettings()
            if (existing != null) {
                _settings.value = existing.toModel()
            } else {
                retentionDao.insertSettings(RetentionPolicySettings().toEntity())
            }
        }
    }

    override suspend fun updateSettings(newSettings: RetentionPolicySettings) {
        val existing = retentionDao.getSettings()
        if (existing != null) {
            retentionDao.updateSettings(newSettings.toEntity(id = existing.id))
        } else {
            retentionDao.insertSettings(newSettings.toEntity())
        }
        _settings.value = newSettings
    }

    override suspend fun executeCleanup() {
        val currentSettings = _settings.value
        val now = timeService.currentTime
        val cutoffs = currentSettings.computeCutoffs(now)

        // 1. Core Cleanup: Reminders
        val remindersCutoff = cutoffs[DataRetentionCategory.REMINDERS]
        if (remindersCutoff != null) {
            appDatabase.reminderDao().deletePastRemindersOlderThan(remindersCutoff.ms)
        }

        // 2. Core Cleanup: System Metrics
        val systemMetricsCutoff = cutoffs[DataRetentionCategory.SYSTEM_METRICS]
        if (systemMetricsCutoff != null) {
            systemMetricsRepository.pruneOldInsights(systemMetricsCutoff)
        }

        // 3. Notify Plugins
        for (plugin in pluginManager.getPlugins()) {
            try {
                plugin.purgeData(cutoffs)
            } catch (_: Exception) {
                // Ignore plugin-level cleanup failures to prevent breaking system maintenance
            }
        }
    }
}