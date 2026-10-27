package de.dh.daps.ui.screens.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import de.dh.daps.common.domain.model.therapy.Timestamp
import de.dh.daps.common.domain.model.reminder.Reminder
import de.dh.daps.common.domain.model.reminder.ReminderStatus
import de.dh.daps.core.SystemRegistry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PastRemindersUiState(
    val pastReminders: List<Reminder> = emptyList(),
    val isLoading: Boolean = false
)

class PastRemindersViewModel(
    registry: SystemRegistry
) : ViewModel() {
    private val reminderManager = registry.reminderManager

    val uiState: StateFlow<PastRemindersUiState> = combine(
        reminderManager.allReminders,
        reminderManager.activeReminders
    ) { all, active ->
        val activeIds = active.map { it.id }.toSet()
        val past = all.filter { it.id !in activeIds }
            .sortedByDescending { it.reminderTimestamp }
        PastRemindersUiState(
            pastReminders = past,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PastRemindersUiState(isLoading = true)
    )

    fun reactivateReminder(reminder: Reminder) {
        val now = Timestamp.now()
        if (reminder.reminderTimestamp > now.plusMinutes(1)) {
            viewModelScope.launch {
                reminderManager.scheduleReminder(
                    reminder.copy(status = ReminderStatus.ACTIVE)
                )
            }
        }
    }

    fun deleteReminder(id: Long) {
        viewModelScope.launch {
            reminderManager.deleteReminder(id)
        }
    }

    fun clearAllPastReminders() {
        viewModelScope.launch {
            reminderManager.purgeAllPastReminders()
        }
    }

    companion object {
        class Factory(private val registry: SystemRegistry) : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return PastRemindersViewModel(registry) as T
            }
        }
    }
}