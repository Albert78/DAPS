package de.dh.daps.ui.screens.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import de.dh.daps.common.model.data.Minutes
import de.dh.daps.common.model.data.Timestamp
import de.dh.daps.common.model.reminder.Reminder
import de.dh.daps.core.SystemRegistry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RemindersUiState(
    val activeReminders: List<Reminder> = emptyList(),
    val historicalReminders: List<Reminder> = emptyList(),
    val isLoading: Boolean = false
)

class RemindersViewModel(
    registry: SystemRegistry
) : ViewModel() {

    private val reminderManager = registry.reminderManager

    val uiState: StateFlow<RemindersUiState> = reminderManager.activeReminders
        .map { active ->
            RemindersUiState(
                activeReminders = active.sortedBy { it.reminderTimestamp },
                isLoading = false
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = RemindersUiState(isLoading = true)
        )

    fun snoozeReminder(id: Long, minutes: Int) {
        viewModelScope.launch {
            val newTimestamp = Timestamp.now() + Minutes(minutes.toShort())
            reminderManager.snoozeReminder(id, newTimestamp)
        }
    }

    fun dismissReminder(id: Long) {
        viewModelScope.launch {
            reminderManager.dismissReminder(id)
        }
    }

    fun deleteReminder(id: Long) {
        viewModelScope.launch {
            reminderManager.deleteReminder(id)
        }
    }

    companion object {
        class Factory(private val registry: SystemRegistry) : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return RemindersViewModel(registry) as T
            }
        }
    }
}