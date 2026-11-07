package de.dh.daps.core.infrastructure

import de.dh.daps.common.domain.model.retention.RetentionPolicySettings
import kotlinx.coroutines.flow.StateFlow

/**
 * Manages system-wide data retention settings and coordinates historical data purge execution
 * across core repositories and registered plugins.
 */
interface DataRetentionManager {
    val settings: StateFlow<RetentionPolicySettings>

    suspend fun updateSettings(newSettings: RetentionPolicySettings)
    suspend fun executeCleanup()
}