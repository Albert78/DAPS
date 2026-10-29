package de.dh.daps.ui.screens.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import de.dh.daps.common.ID_UNDEFINED
import de.dh.daps.common.domain.model.data.Minutes
import de.dh.daps.common.domain.model.data.Timestamp
import de.dh.daps.common.domain.model.reminder.Reminder
import de.dh.daps.common.domain.model.reminder.ReminderStatus
import de.dh.daps.common.domain.model.reminder.ReminderType
import de.dh.daps.core.SystemRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReminderEditorUiState(
    val id: Long? = null,
    val type: ReminderType = ReminderType.CUSTOM,
    val title: String = "",
    val description: String = "",
    val reminderTimestamp: Timestamp = Timestamp.now() + Minutes(15),
    val sourceId: String = "",
    val status: ReminderStatus = ReminderStatus.ACTIVE,
    val isSaving: Boolean = false,
    val isLoading: Boolean = false
) {
    val isValid: Boolean
        get() {
            val titleValid = title.isNotBlank()
            return titleValid
        }
}

class ReminderEditorViewModel(
    registry: SystemRegistry,
    private val reminderId: Long?
) : ViewModel() {

    private val reminderManager = registry.reminderManager
    private val _uiState = MutableStateFlow(ReminderEditorUiState())
    val uiState: StateFlow<ReminderEditorUiState> = _uiState.asStateFlow()

    init {
        if (reminderId != null) {
            loadReminder(reminderId)
        }
    }

    private fun loadReminder(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val reminder = reminderManager.allReminders.value.find { it.id == id }
                ?: reminderManager.activeReminders.value.find { it.id == id }
            if (reminder != null) {
                _uiState.update {
                    it.copy(
                        id = reminder.id,
                        type = reminder.type,
                        title = reminder.title,
                        description = reminder.description,
                        reminderTimestamp = reminder.reminderTimestamp,
                        sourceId = reminder.sourceId,
                        status = reminder.status,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onTitleChange(title: String) {
        _uiState.update { it.copy(title = title) }
    }

    fun onDescriptionChange(description: String) {
        _uiState.update { it.copy(description = description) }
    }

    fun onReminderTimestampChange(timestamp: Timestamp) {
        _uiState.update { it.copy(reminderTimestamp = timestamp) }
    }

    fun save(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (!state.isValid) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val reminder = Reminder(
                id = state.id ?: ID_UNDEFINED,
                type = state.type,
                sourceId = state.sourceId.ifBlank { "custom" },
                title = state.title.trim(),
                description = state.description.trim(),
                reminderTimestamp = state.reminderTimestamp,
                status = state.status
            )
            reminderManager.scheduleReminder(reminder)
            onSuccess()
        }
    }

    companion object {
        class Factory(private val registry: SystemRegistry, private val reminderId: Long?) : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return ReminderEditorViewModel(registry, reminderId) as T
            }
        }
    }
}