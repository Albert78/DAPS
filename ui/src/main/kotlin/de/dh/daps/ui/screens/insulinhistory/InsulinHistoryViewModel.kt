package de.dh.daps.ui.screens.insulinhistory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.dh.daps.common.MEAL_EDIT_THRESHOLD_HOURS
import de.dh.daps.common.model.InsulinAmount
import de.dh.daps.common.model.InsulinApplication
import de.dh.daps.common.model.InsulinOrigin
import de.dh.daps.common.model.InsulinStatus
import de.dh.daps.common.model.InsulinType
import de.dh.daps.common.model.data.Timestamp
import de.dh.daps.core.SystemRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class InsulinHistoryUiState(
    val insulinEntries: List<InsulinApplication> = emptyList(),
    val insulinTypes: List<InsulinType> = emptyList(),
    val defaultInsulinType: InsulinType? = null,
    val editThresholdHours: Int = MEAL_EDIT_THRESHOLD_HOURS
)

class InsulinHistoryViewModel(
    private val registry: SystemRegistry
) : ViewModel() {

    private val treatmentRepository = registry.treatmentRepository
    private val therapyManager = registry.therapyManager

    private val _insulinTypes = MutableStateFlow<List<InsulinType>>(emptyList())
    private val _defaultInsulinType = MutableStateFlow<InsulinType?>(null)

    init {
        viewModelScope.launch {
            _insulinTypes.value = treatmentRepository.getAllInsulinTypes()
            _defaultInsulinType.value = therapyManager.getPumpInsulinType()
        }
    }

    val uiState: StateFlow<InsulinHistoryUiState> = combine(
        treatmentRepository.observeInsulinApplications(includeCancelled = true),
        _insulinTypes,
        _defaultInsulinType
    ) { entries, types, defaultType ->
        InsulinHistoryUiState(
            insulinEntries = entries.sortedByDescending { it.timestamp },
            insulinTypes = types,
            defaultInsulinType = defaultType,
            editThresholdHours = MEAL_EDIT_THRESHOLD_HOURS
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = InsulinHistoryUiState()
        )

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
        basal: Boolean,
        correction: Boolean,
        meal: Boolean
    ) {
        viewModelScope.launch {
            val application = InsulinApplication(
                timestamp = Timestamp.now(),
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
        basal: Boolean,
        correction: Boolean,
        meal: Boolean
    ) {
        viewModelScope.launch {
            val updated = application.copy(
                dose = application.dose.copy(amount = newAmount, insulinType = newType),
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