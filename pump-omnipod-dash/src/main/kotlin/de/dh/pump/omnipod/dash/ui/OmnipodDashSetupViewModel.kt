package de.dh.pump.omnipod.dash.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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

    fun loadSavedState(driver: OmnipodDashInsulinPumpDriver) {
        viewModelScope.launch {
            val preferences = driver.getContext()?.preferences
            val podStateManager = OmnipodDashPodStateManagerImpl(preferences)
            podStateManager.loadFromPreferences()

            val stepIndex = stepIndexForProgress(podStateManager.activationProgress)
            _currentStepIndex.value = stepIndex
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

        viewModelScope.launch {
            podStateManager.loadFromPreferences()

            when (steps[currentIndex]) {
                ActivationStep.FILL_AND_PRIME -> {
                    podStateManager.activationProgress = ActivationProgress.PRIME_COMPLETED
                    podStateManager.saveToPreferences()
                    _currentStepIndex.value = currentIndex + 1
                }
                ActivationStep.APPLY_AND_INSERT -> {
                    podStateManager.activationProgress = ActivationProgress.CANNULA_INSERTED
                    podStateManager.saveToPreferences()
                    _currentStepIndex.value = currentIndex + 1
                }
                ActivationStep.CONFIRM_AND_ACTIVATE -> {
                    _isActivating.value = true
                    _activationError.value = null
                    try {
                        podStateManager.activatedAtTimestampMs = System.currentTimeMillis()
                        podStateManager.activationProgress = ActivationProgress.COMPLETED
                        podStateManager.saveToPreferences()

                        driver.completeActivation(podStateManager)

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

    fun unbind(driver: OmnipodDashInsulinPumpDriver) {
        viewModelScope.launch {
            try {
                driver.unbindDevice()
            } catch (e: Exception) {
                _activationError.value = e.message
            }
        }
    }

    companion object {
        fun stepIndexForProgress(progress: ActivationProgress): Int {
            return when {
                progress.isAtLeast(ActivationProgress.CANNULA_INSERTED) -> 2
                progress.isAtLeast(ActivationProgress.PRIME_COMPLETED) -> 1
                else -> 0
            }
        }

        class Factory(private val application: Application) : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return OmnipodDashSetupViewModel(application) as T
            }
        }
    }
}