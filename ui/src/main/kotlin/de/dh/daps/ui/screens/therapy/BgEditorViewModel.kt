package de.dh.daps.ui.screens.therapy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import de.dh.daps.common.model.data.BgBlock
import de.dh.daps.core.SystemRegistry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BgEditorUiState(
    val defaultBgBlocks: List<BgBlock> = emptyList(),
    val isLoading: Boolean = true,
)

class BgEditorViewModel(
    systemRegistry: SystemRegistry
) : ViewModel() {
    private val therapyManager = systemRegistry.therapyManager

    val uiState: StateFlow<BgEditorUiState> = therapyManager.currentTherapySettingsFlow
        .map { settings ->
            BgEditorUiState(
                defaultBgBlocks = settings.defaultBgBlocks,
                isLoading = false
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = BgEditorUiState()
        )

    fun updateDefaultBgBlocks(blocks: List<BgBlock>) {
        viewModelScope.launch {
            therapyManager.updateDefaultBgBlocks(blocks)
        }
    }

    companion object {
        class Factory(private val registry: SystemRegistry) : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return BgEditorViewModel(registry) as T
            }
        }
    }
}