package de.dh.pump.omnipod.dash.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.dh.daps.common.model.DeviceLifecycleState
import de.dh.daps.common.ui.UiText
import de.dh.pump.omnipod.dash.OmnipodDashInsulinPumpDriver
import de.dh.pump.omnipod.dash.R
import de.dh.pump.omnipod.protocol.definition.ActivationProgress
import de.dh.pump.omnipod.protocol.state.OmnipodDashPodStateManagerImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel managing the pod setup and activation wizard for Omnipod Dash.
 */
class OmnipodDashSetupViewModel(application: Application) : AndroidViewModel(application) {

    private val _currentStepIndex = MutableStateFlow(0)
    val currentStepIndex: StateFlow<Int> = _currentStepIndex.asStateFlow()

    private val _isActivating = MutableStateFlow(false)
    val isActivating: StateFlow<Boolean> = _isActivating.asStateFlow()

    private val _activationError = MutableStateFlow<String?>(null)
    val activationError: StateFlow<String?> = _activationError.asStateFlow()

    fun previousStep() {
        if (_currentStepIndex.value > 0) {
            _currentStepIndex.value -= 1
        }
    }

    fun nextStep(
        driver: OmnipodDashInsulinPumpDriver,
        onSuccess: () -> Unit
    ) {
        val steps = ActivationStep.entries
        val currentIndex = _currentStepIndex.value
        if (currentIndex !in steps.indices) return

        val preferences = driver.getContext()?.preferences
        val podStateManager = OmnipodDashPodStateManagerImpl(preferences)

        when (steps[currentIndex]) {
            ActivationStep.FILL_AND_PRIME -> {
                podStateManager.activationProgress = ActivationProgress.PRIME_COMPLETED
                _currentStepIndex.value = currentIndex + 1
            }
            ActivationStep.APPLY_AND_INSERT -> {
                podStateManager.activationProgress = ActivationProgress.CANNULA_INSERTED
                _currentStepIndex.value = currentIndex + 1
            }
            ActivationStep.CONFIRM_AND_ACTIVATE -> {
                viewModelScope.launch {
                    _isActivating.value = true
                    _activationError.value = null
                    try {
                        podStateManager.activatedAtTimestampMs = System.currentTimeMillis()
                        podStateManager.activationProgress = ActivationProgress.COMPLETED
                        podStateManager.saveToPreferences()

                        driver.updateLifecycleState(
                            DeviceLifecycleState.DeviceBound(
                                deviceId = podStateManager.bluetoothAddress ?: "POD-DASH-ACTIVE",
                                displayName = UiText.StringResource(R.string.omnipod_dash_pump_display_name),
                            )
                        )

                        _isActivating.value = false
                        onSuccess()
                    } catch (e: Exception) {
                        _isActivating.value = false
                        _activationError.value = e.message
                            ?: getApplication<Application>().getString(R.string.omnipod_dash_setup_activate_failed)
                    }
                }
            }
        }
    }

    companion object {
        class Factory(private val application: Application) : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return OmnipodDashSetupViewModel(application) as T
            }
        }
    }
}