package de.dh.pump.omnipod.dash.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.dh.pump.omnipod.dash.OmnipodDashInsulinPumpDriver
import de.dh.pump.omnipod.dash.R
import de.dh.pump.omnipod.dash.activation.OmnipodDashActivationManager
import de.dh.pump.omnipod.protocol.ble.OmnipodDashBleManager
import de.dh.pump.omnipod.protocol.ble.PodBleConnectionState
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

    private val _bleConnectionState = MutableStateFlow(PodBleConnectionState.DISCONNECTED)
    val bleConnectionState: StateFlow<PodBleConnectionState> = _bleConnectionState.asStateFlow()

    fun loadSavedState(driver: OmnipodDashInsulinPumpDriver) {
        viewModelScope.launch {
            val pc = driver.getContext() ?: return@launch
            val preferences = pc.preferences
            val podStateManager = OmnipodDashPodStateManagerImpl(preferences)
            podStateManager.loadFromPreferences()

            val bleManager = OmnipodDashBleManager(pc.appContext, podStateManager)
            viewModelScope.launch {
                bleManager.connectionState.collect { state ->
                    _bleConnectionState.value = state
                }
            }
            val activationManager = OmnipodDashActivationManager(podStateManager, bleManager)

            val recoveredProgress = activationManager.recoverActivationProgress()
            val stepIndex = stepIndexForProgress(recoveredProgress)
            _currentStepIndex.value = stepIndex
        }
    }

    fun nextStep(
        driver: OmnipodDashInsulinPumpDriver,
        onSuccess: (() -> Unit)? = null
    ) {
        val steps = ActivationStep.entries
        val currentIndex = _currentStepIndex.value
        if (currentIndex !in steps.indices) return

        val pc = driver.getContext()
        if (pc == null) {
            _activationError.value = getApplication<Application>().getString(R.string.omnipod_dash_setup_activate_failed)
            return
        }
        val preferences = pc.preferences
        val podStateManager = OmnipodDashPodStateManagerImpl(preferences)

        viewModelScope.launch {
            podStateManager.loadFromPreferences()

            val bleManager = OmnipodDashBleManager(pc.appContext, podStateManager)
            val activationManager = OmnipodDashActivationManager(podStateManager, bleManager)

            _isActivating.value = true
            _activationError.value = null

            val result = when (steps[currentIndex]) {
                ActivationStep.FILL_AND_PRIME -> activationManager.performFillAndPrime()
                ActivationStep.APPLY_AND_INSERT -> activationManager.performApplyAndInsert()
                ActivationStep.CONFIRM_AND_ACTIVATE -> activationManager.performConfirmAndActivate()
            }

            result.fold(
                onSuccess = {
                    if (steps[currentIndex] == ActivationStep.CONFIRM_AND_ACTIVATE) {
                        driver.completeActivation(podStateManager)
                        _isActivating.value = false
                        onSuccess?.invoke()
                    } else {
                        _isActivating.value = false
                        _currentStepIndex.value = currentIndex + 1
                    }
                },
                onFailure = { error ->
                    _isActivating.value = false
                    _activationError.value = error.message
                        ?: getApplication<Application>().getString(R.string.omnipod_dash_setup_activate_failed)
                }
            )
        }
    }

    fun cancelSetup(
        driver: OmnipodDashInsulinPumpDriver,
        onCancelled: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            try {
                driver.cancelSetup()
            } catch (e: Exception) {
                _activationError.value = e.message
            } finally {
                _currentStepIndex.value = 0
                _activationError.value = null
                _isActivating.value = false
                onCancelled?.invoke()
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

        class Factory(private val application: Application) : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return OmnipodDashSetupViewModel(application) as T
            }
        }
    }
}