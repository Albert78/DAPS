package de.dh.daps.ui.screens.insulinhistory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.dh.daps.common.MEAL_EDIT_THRESHOLD_HOURS
import de.dh.daps.common.domain.model.data.Timestamp
import de.dh.daps.common.domain.model.therapy.InsulinAmount
import de.dh.daps.common.domain.model.therapy.InsulinApplication
import de.dh.daps.common.domain.model.therapy.InsulinOrigin
import de.dh.daps.common.domain.model.therapy.InsulinStatus
import de.dh.daps.common.domain.model.therapy.InsulinType
import de.dh.daps.core.SystemRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class InsulinCategoryFilter {
    BASAL, CORRECTION, MEAL
}

data class InsulinHistoryUiState(
    val insulinEntries: List<InsulinApplication> = emptyList(),
    val allInsulinEntries: List<InsulinApplication> = emptyList(),
    val insulinTypes: List<InsulinType> = emptyList(),
    val defaultInsulinType: InsulinType? = null,
    val editThresholdHours: Int = MEAL_EDIT_THRESHOLD_HOURS,
    val selectedCategories: Set<InsulinCategoryFilter> = emptySet(),
    val selectedStatuses: Set<InsulinStatus> = emptySet()
) {
    val isFilterActive: Boolean
        get() = (selectedCategories.isNotEmpty() && selectedCategories.size < 3) ||
                (selectedStatuses.isNotEmpty() && selectedStatuses.size < InsulinStatus.entries.size)
}

class InsulinHistoryViewModel(
    private val registry: SystemRegistry
) : ViewModel() {

    private val treatmentRepository = registry.treatmentRepository
    private val therapyManager = registry.therapyManager

    private val _insulinTypes = MutableStateFlow<List<InsulinType>>(emptyList())
    private val _defaultInsulinType = MutableStateFlow<InsulinType?>(null)
    private val _selectedCategories = MutableStateFlow<Set<InsulinCategoryFilter>>(emptySet())
    private val _selectedStatuses = MutableStateFlow<Set<InsulinStatus>>(emptySet())

    init {
        viewModelScope.launch {
            _insulinTypes.value = treatmentRepository.getAllInsulinTypes()
            _defaultInsulinType.value = therapyManager.getPumpInsulinType()
        }
    }

    val uiState: StateFlow<InsulinHistoryUiState> = combine(
        treatmentRepository.observeInsulinApplications(includeCancelled = true),
        _insulinTypes,
        _defaultInsulinType,
        _selectedCategories,
        _selectedStatuses
    ) { entries, types, defaultType, categories, statuses ->
        val sortedEntries = entries.sortedByDescending { it.timestamp }
        val filteredEntries = sortedEntries.filter { application ->
            val matchesCategory = if (categories.isEmpty() || categories.size == 3) {
                true
            } else {
                (categories.contains(InsulinCategoryFilter.BASAL) && application.basal) ||
                (categories.contains(InsulinCategoryFilter.CORRECTION) && application.correction) ||
                (categories.contains(InsulinCategoryFilter.MEAL) && application.meal)
            }
            val matchesStatus = if (statuses.isEmpty() || statuses.size == InsulinStatus.entries.size) {
                true
            } else {
                application.status in statuses
            }
            matchesCategory && matchesStatus
        }

        InsulinHistoryUiState(
            insulinEntries = filteredEntries,
            allInsulinEntries = sortedEntries,
            insulinTypes = types,
            defaultInsulinType = defaultType,
            editThresholdHours = MEAL_EDIT_THRESHOLD_HOURS,
            selectedCategories = categories,
            selectedStatuses = statuses
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = InsulinHistoryUiState()
        )

    fun setFilters(categories: Set<InsulinCategoryFilter>, statuses: Set<InsulinStatus>) {
        _selectedCategories.value = categories
        _selectedStatuses.value = statuses
    }

    fun clearFilters() {
        _selectedCategories.value = emptySet()
        _selectedStatuses.value = emptySet()
    }

    companion object {
        class Factory(private val registry: SystemRegistry) : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return InsulinHistoryViewModel(registry) as T
            }
        }
    }

    fun addManualInsulin(
        amount: InsulinAmount,
        insulinType: InsulinType,
        timestamp: Timestamp,
        basal: Boolean,
        correction: Boolean,
        meal: Boolean
    ) {
        viewModelScope.launch {
            val application = InsulinApplication(
                timestamp = timestamp,
                amount = amount,
                insulinType = insulinType,
                origin = InsulinOrigin.Manual,
                basal = basal,
                correction = correction,
                meal = meal
            )
            treatmentRepository.addInsulinApplication(application)
        }
    }

    fun updateManualInsulin(
        application: InsulinApplication,
        newAmount: InsulinAmount,
        newType: InsulinType,
        timestamp: Timestamp,
        basal: Boolean,
        correction: Boolean,
        meal: Boolean
    ) {
        viewModelScope.launch {
            val updated = application.copy(
                dose = application.dose.copy(timestamp = timestamp, amount = newAmount, insulinType = newType),
                basal = basal,
                correction = correction,
                meal = meal
            )
            treatmentRepository.updateInsulinApplication(updated)
        }
    }

    fun deleteInsulin(application: InsulinApplication) {
        viewModelScope.launch {
            treatmentRepository.removeInsulinApplication(application)
        }
    }

    fun toggleInvalidated(application: InsulinApplication) {
        viewModelScope.launch {
            val newStatus = if (application.status == InsulinStatus.Invalidated) {
                InsulinStatus.Confirmed
            } else {
                InsulinStatus.Invalidated
            }
            treatmentRepository.setInsulinApplicationStatus(application.id, newStatus)
        }
    }
}