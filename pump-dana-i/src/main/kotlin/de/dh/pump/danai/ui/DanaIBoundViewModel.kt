package de.dh.pump.danai.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.dh.pump.dana.commands.options.DanaRsUserOptions
import de.dh.pump.danai.DanaIInsulinPumpDriver
import de.dh.pump.danai.R
import de.dh.pump.danai.core.DanaIPumpAdapter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel managing user options and unbinding operations for a bound Dana-i insulin pump.
 */
class DanaIBoundViewModel(application: Application) : AndroidViewModel(application) {
    private val _userOptions = MutableStateFlow<DanaRsUserOptions?>(null)
    val userOptions: StateFlow<DanaRsUserOptions?> = _userOptions.asStateFlow()

    private val _isLoadingOptions = MutableStateFlow(false)
    val isLoadingOptions: StateFlow<Boolean> = _isLoadingOptions.asStateFlow()

    private val _isSavingOptions = MutableStateFlow(false)
    val isSavingOptions: StateFlow<Boolean> = _isSavingOptions.asStateFlow()

    private val _optionsError = MutableStateFlow<String?>(null)
    val optionsError: StateFlow<String?> = _optionsError.asStateFlow()

    fun loadUserOptions(driver: DanaIInsulinPumpDriver) {
        viewModelScope.launch {
            _isLoadingOptions.value = true
            _optionsError.value = null
            try {
                val pumpAdapter = driver.activePump.value as? DanaIPumpAdapter
                val pump = pumpAdapter?.pump
                if (pump != null) {
                    val options = pump.getUserOptions()
                    _userOptions.value = options
                } else {
                    _optionsError.value = getApplication<Application>().getString(R.string.danai_unbind_dialog_pump_not_connected)
                }
            } catch (e: Exception) {
                _optionsError.value = e.message ?: getApplication<Application>().getString(R.string.danai_unbind_dialog_load_failed)
            } finally {
                _isLoadingOptions.value = false
            }
        }
    }

    fun saveAndUnbind(
        driver: DanaIInsulinPumpDriver,
        optionsToSave: DanaRsUserOptions?,
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            _isSavingOptions.value = true
            _optionsError.value = null
            try {
                if (optionsToSave != null) {
                    val pumpAdapter = driver.activePump.value as? DanaIPumpAdapter
                    val pump = pumpAdapter?.pump
                    pump?.setUserOptions(optionsToSave)
                }
                driver.unbindDevice()
                onSuccess?.invoke()
            } catch (e: Exception) {
                _optionsError.value = e.message ?: getApplication<Application>().getString(R.string.danai_unbind_dialog_failed)
            } finally {
                _isSavingOptions.value = false
            }
        }
    }

    fun unbind(
        driver: DanaIInsulinPumpDriver,
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            try {
                driver.unbindDevice()
                onSuccess?.invoke()
            } catch (e: Exception) {
                _optionsError.value = e.message
            }
        }
    }

    companion object {
        class Factory(private val application: Application) : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DanaIBoundViewModel(application) as T
            }
        }
    }
}