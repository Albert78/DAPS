package de.dh.pump.danai.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.dh.daps.common.domain.model.therapy.InsulinConcentration
import de.dh.daps.common.infrastructure.PluginPreferences
import de.dh.pump.danai.DanaIInsulinPumpDriver
import de.dh.pump.danai.core.DanaPumpPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel managing the selection of insulin type and concentration during setup.
 */
class DanaISetupInsulinParametersViewModel(
    private val preferences: PluginPreferences?
) : ViewModel() {

    private val _selectedTypeId = MutableStateFlow<String?>(null)
    val selectedTypeId: StateFlow<String?> = _selectedTypeId.asStateFlow()

    private val _selectedConcentration = MutableStateFlow(InsulinConcentration.U100)
    val selectedConcentration: StateFlow<InsulinConcentration> = _selectedConcentration.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    init {
        preferences?.let { prefs ->
            viewModelScope.launch {
                val danaPrefs = DanaPumpPreferences(prefs)
                val typeId = danaPrefs.getInsulinTypeId()
                if (typeId != null) {
                    _selectedTypeId.value = typeId
                }
                _selectedConcentration.value = danaPrefs.getInsulinConcentration()
            }
        }
    }

    fun selectInsulinType(typeId: String) {
        _selectedTypeId.value = typeId
    }

    fun selectInsulinConcentration(concentration: InsulinConcentration) {
        _selectedConcentration.value = concentration
    }

    fun completeSetup(
        driver: DanaIInsulinPumpDriver,
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                driver.completeSetup(
                    insulinTypeId = _selectedTypeId.value,
                    concentration = _selectedConcentration.value
                )
                onSuccess?.invoke()
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun cancelSetup(
        driver: DanaIInsulinPumpDriver,
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            driver.unbindDevice()
            onSuccess?.invoke()
        }
    }

    companion object {
        class Factory(private val preferences: PluginPreferences?) : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DanaISetupInsulinParametersViewModel(preferences) as T
            }
        }
    }
}