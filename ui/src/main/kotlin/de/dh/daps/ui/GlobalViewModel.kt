package de.dh.daps.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import de.dh.daps.carbsUnit
import de.dh.daps.common.model.data.CarbsUnit
import de.dh.daps.common.model.data.GlucoseUnit
import de.dh.daps.core.SystemRegistry
import de.dh.daps.glucoseUnit
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class GlobalViewModel(
    systemRegistry: SystemRegistry
) : ViewModel() {
    private val appPreferencesRepository = systemRegistry.appPreferencesRepository

    val glucoseUnit: StateFlow<GlucoseUnit> = appPreferencesRepository.cachedPreferences
        .map { it.glucoseUnit }
        .stateIn(
            scope = MainScope(),
            started = SharingStarted.Eagerly,
            initialValue = GlucoseUnit.MG_DL
        )

    val carbsUnit: StateFlow<CarbsUnit> = appPreferencesRepository.cachedPreferences
        .map { it.carbsUnit }
        .stateIn(
            scope = MainScope(),
            started = SharingStarted.Eagerly,
            initialValue = CarbsUnit.GRAMS
        )

    companion object {
        class Factory(private val registry: SystemRegistry) : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return GlobalViewModel(registry) as T
            }
        }
    }
}