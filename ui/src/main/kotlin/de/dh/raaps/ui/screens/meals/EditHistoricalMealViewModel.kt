package de.dh.raaps.ui.screens.meals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import de.dh.raaps.common.model.DeferredBolus
import de.dh.raaps.common.model.ID_UNDEFINED
import de.dh.raaps.common.model.InsulinAmount
import de.dh.raaps.common.model.MEAL_ADD_THRESHOLD_HOURS
import de.dh.raaps.common.model.MEAL_EDIT_THRESHOLD_HOURS
import de.dh.raaps.common.model.MealEntry
import de.dh.raaps.common.model.MealType
import de.dh.raaps.common.model.data.Minutes
import de.dh.raaps.common.model.data.Timestamp
import de.dh.raaps.core.SystemRegistry
import de.dh.raaps.ui.controls.meal.PlannedBolusUiModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditHistoricalMealUiState(
    val isLoading: Boolean = true,
    val isAddMode: Boolean = false,
    val meal: MealEntry? = null,
    val editedCarbsKe: Double = 0.0,
    val editedTimestamp: Timestamp = Timestamp.now(),
    val editedMealType: MealType? = null,
    val mealTypes: List<MealType> = emptyList(),
    val pendingDeferredBoluses: List<PlannedBolusUiModel> = emptyList(),
    val isBolusPlanSheetOpen: Boolean = false,
    val insulinAdministered: Boolean = false,
    val isSaving: Boolean = false,
    val isFormValid: Boolean = false
)

class EditHistoricalMealViewModel(
    private val registry: SystemRegistry,
    private val mealId: Long
) : ViewModel() {
    private val _uiState = MutableStateFlow(EditHistoricalMealUiState())
    val uiState: StateFlow<EditHistoricalMealUiState> = _uiState.asStateFlow()

    private val treatmentRepository = registry.treatmentRepository
    private val isAddMode = mealId == ID_UNDEFINED
    private var originalDeferredBoluses: List<DeferredBolus> = emptyList()

    init {
        loadMeal()
    }

    private fun loadMeal() {
        viewModelScope.launch {
            val meal = if (isAddMode) null else treatmentRepository.getMeal(mealId)
            val mealTypes = treatmentRepository.getAllMealTypes()

            val allDeferred = treatmentRepository.getDeferredBoluses()
            val mealDeferred = if (meal != null) {
                allDeferred.filter { it.mealId == meal.id }
            } else {
                emptyList()
            }
            originalDeferredBoluses = mealDeferred

            val pendingUiModels = mealDeferred.mapIndexed { index, dbBolus ->
                PlannedBolusUiModel(
                    id = dbBolus.id,
                    amount = dbBolus.amount,
                    timestamp = dbBolus.timestamp,
                    timeFromMeal = Minutes.timeDifference(meal?.timestamp ?: Timestamp.now(), dbBolus.timestamp),
                    label = ""
                )
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    isAddMode = isAddMode,
                    meal = meal,
                    mealTypes = mealTypes,
                    editedCarbsKe = meal?.let { m -> m.carbGrams / 10.0 } ?: 0.0,
                    editedTimestamp = meal?.timestamp ?: (Timestamp.now() - Minutes(15)),
                    editedMealType = meal?.mealType,
                    pendingDeferredBoluses = pendingUiModels,
                    insulinAdministered = meal?.insulinAdministered ?: false
                )
            }
            validateForm()
        }
    }

    fun onCarbsChange(ke: Double) {
        _uiState.update { it.copy(editedCarbsKe = ke) }
        validateForm()
    }

    fun onTimestampChange(timestamp: Timestamp) {
        val now = Timestamp.now()
        val thresholdMinutes = if (isAddMode) MEAL_ADD_THRESHOLD_HOURS * 60 else (MEAL_EDIT_THRESHOLD_HOURS * 60)
        val minTime = now - Minutes(thresholdMinutes.toShort())
        val maxTime = now - Minutes(5)

        val cappedTimestamp = if (timestamp < minTime) minTime else if (timestamp > maxTime) maxTime else timestamp
        _uiState.update { it.copy(editedTimestamp = cappedTimestamp) }
        validateForm()
    }

    fun onMealTypeChange(mealType: MealType) {
        _uiState.update { it.copy(editedMealType = mealType) }
        validateForm()
    }

    fun onOpenBolusPlanSheet() {
        _uiState.update { it.copy(isBolusPlanSheetOpen = true) }
    }

    fun onCloseBolusPlanSheet() {
        _uiState.update { it.copy(isBolusPlanSheetOpen = false) }
    }

    fun onAddDeferredBolus() {
        _uiState.update { s ->
            val now = Timestamp.now()
            val baseTime = s.editedTimestamp
            val lastTime = s.pendingDeferredBoluses.lastOrNull()?.timestamp ?: (if (baseTime < now) now else baseTime)
            val newTimestamp = lastTime + Minutes(30)
            val newBolus = PlannedBolusUiModel(
                id = ID_UNDEFINED,
                amount = InsulinAmount(1.0),
                timestamp = newTimestamp,
                timeFromMeal = Minutes.timeDifference(baseTime, newTimestamp),
                label = ""
            )
            s.copy(pendingDeferredBoluses = s.pendingDeferredBoluses + newBolus)
        }
    }

    fun onUpdateDeferredBolusTime(index: Int, newTimestamp: Timestamp) {
        _uiState.update { s ->
            val list = s.pendingDeferredBoluses.toMutableList()
            if (index in list.indices) {
                list[index] = list[index].copy(
                    timestamp = newTimestamp,
                    timeFromMeal = Minutes.timeDifference(s.editedTimestamp, newTimestamp)
                )
            }
            s.copy(pendingDeferredBoluses = list)
        }
    }

    fun onUpdateDeferredBolusAmount(index: Int, newAmount: InsulinAmount) {
        _uiState.update { s ->
            val list = s.pendingDeferredBoluses.toMutableList()
            if (index in list.indices) {
                list[index] = list[index].copy(amount = newAmount)
            }
            s.copy(pendingDeferredBoluses = list)
        }
    }

    fun onRemoveDeferredBolus(index: Int) {
        _uiState.update { s ->
            val list = s.pendingDeferredBoluses.toMutableList()
            if (index in list.indices) {
                list.removeAt(index)
            }
            s.copy(pendingDeferredBoluses = list)
        }
    }

    private fun validateForm() {
        _uiState.update {
            it.copy(isFormValid = it.editedCarbsKe > 0.0 && it.editedMealType != null)
        }
    }

    fun saveChanges(onSuccess: () -> Unit) {
        val state = _uiState.value
        val mealType = state.editedMealType ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val mealToSave = if (isAddMode) {
                MealEntry(
                    id = ID_UNDEFINED,
                    timestamp = state.editedTimestamp,
                    carbGrams = state.editedCarbsKe * 10.0,
                    mealType = mealType
                )
            } else {
                state.meal?.copy(
                    carbGrams = state.editedCarbsKe * 10.0,
                    timestamp = state.editedTimestamp,
                    mealType = mealType
                ) ?: return@launch
            }

            treatmentRepository.addMealEntry(mealToSave)
            val savedMealId = mealToSave.id

            // Synchronize deferred boluses
            val currentPending = state.pendingDeferredBoluses
            val currentIds = currentPending.map { it.id }.filter { it != ID_UNDEFINED }.toSet()

            // 1. Delete removed items
            val toDelete = originalDeferredBoluses.filter { it.id !in currentIds }
            if (toDelete.isNotEmpty()) {
                treatmentRepository.removeDeferredBoluses(toDelete)
            }

            // 2. Add or update items
            currentPending.forEach { uiModel ->
                val entity = DeferredBolus(
                    id = uiModel.id,
                    amount = uiModel.amount,
                    timestamp = uiModel.timestamp,
                    mealId = savedMealId
                )
                if (uiModel.id == ID_UNDEFINED) {
                    treatmentRepository.addDeferredBolus(entity)
                } else {
                    treatmentRepository.updateDeferredBolus(entity)
                }
            }

            _uiState.update {
                it.copy(
                    isSaving = false,
                    meal = if (isAddMode) mealToSave else it.meal
                )
            }
            onSuccess()
        }
    }

    fun deleteMeal(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value.meal?.let { meal ->
                val mealDeferred = treatmentRepository.getDeferredBoluses().filter { it.mealId == meal.id }
                if (mealDeferred.isNotEmpty()) {
                    treatmentRepository.removeDeferredBoluses(mealDeferred)
                }
                treatmentRepository.removeMealEntry(meal)
                onSuccess()
            }
        }
    }

    companion object {
        class Factory(private val registry: SystemRegistry, private val mealId: Long) : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return EditHistoricalMealViewModel(registry, mealId) as T
            }
        }
    }
}