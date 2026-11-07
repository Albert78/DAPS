package de.dh.daps.ui.screens.retention

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import de.dh.daps.common.domain.model.retention.DataRetentionCategory
import de.dh.daps.common.domain.model.retention.RetentionOption
import de.dh.daps.common.domain.model.retention.RetentionPolicySettings
import de.dh.daps.core.SystemRegistry
import de.dh.daps.core.infrastructure.DataRetentionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DataRetentionUiState(
    val settings: RetentionPolicySettings = RetentionPolicySettings(),
    val isExecutingCleanup: Boolean = false,
    val userMessage: String? = null
)

class DataRetentionViewModel(
    private val dataRetentionManager: DataRetentionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DataRetentionUiState())
    val uiState: StateFlow<DataRetentionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            dataRetentionManager.settings.collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
    }

    fun updateRetentionOption(category: DataRetentionCategory, option: RetentionOption) {
        val currentSettings = _uiState.value.settings
        val newSettings = when (category) {
            DataRetentionCategory.GLUCOSE_READINGS -> currentSettings.copy(glucoseReadings = option)
            DataRetentionCategory.THERAPY_HISTORY -> currentSettings.copy(therapyHistory = option)
            DataRetentionCategory.DEVICE_HARDWARE_LOGS -> currentSettings.copy(deviceHardwareLogs = option)
            DataRetentionCategory.REMINDERS -> currentSettings.copy(reminders = option)
            DataRetentionCategory.SYSTEM_METRICS -> currentSettings.copy(systemMetrics = option)
            DataRetentionCategory.OTHER -> currentSettings.copy(other = option)
        }
        viewModelScope.launch {
            dataRetentionManager.updateSettings(newSettings)
        }
    }

    fun executeManualCleanup() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExecutingCleanup = true) }
            try {
                dataRetentionManager.executeCleanup()
                _uiState.update { it.copy(isExecutingCleanup = false, userMessage = "CLEANUP_SUCCESS") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isExecutingCleanup = false, userMessage = e.localizedMessage ?: "ERROR") }
            }
        }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    companion object {
        class Factory(private val registry: SystemRegistry) : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return DataRetentionViewModel(registry.dataRetentionManager) as T
            }
        }
    }
}