package de.dh.daps.ui.screens.appdata

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import de.dh.daps.AppPreferencesRepository
import de.dh.daps.backupDirectoryUri
import de.dh.daps.core.SystemRegistry
import de.dh.daps.core.backup.BackupOptions
import de.dh.daps.core.backup.BackupRepository
import de.dh.daps.core.backup.BackupResult
import de.dh.daps.setBackupDirectoryUri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppDataUiState(
    val backupDirectoryUri: String? = null,
    val includeHistory: Boolean = true,
    val includeDiagnostics: Boolean = false,
    val isProcessing: Boolean = false,
    val userMessage: String? = null,
    val isError: Boolean = false,
    val showResetDialog: Boolean = false,
    val showImportConfirmDialog: Boolean = false,
    val pendingImportUri: Uri? = null,
)

class AppDataViewModel(
    systemRegistry: SystemRegistry
) : ViewModel() {
    private val backupRepository: BackupRepository = systemRegistry.backupRepository
    private val appPreferencesRepository: AppPreferencesRepository = systemRegistry.appPreferencesRepository

    private val _uiState = MutableStateFlow(AppDataUiState())
    val uiState: StateFlow<AppDataUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            appPreferencesRepository.cachedPreferences.collect { prefs ->
                _uiState.update { it.copy(backupDirectoryUri = prefs.backupDirectoryUri) }
            }
        }
    }

    fun setBackupDirectoryUri(uriStr: String?) {
        viewModelScope.launch {
            appPreferencesRepository.setBackupDirectoryUri(uriStr)
        }
    }

    fun setIncludeHistory(value: Boolean) {
        _uiState.update { it.copy(includeHistory = value) }
    }

    fun setIncludeDiagnostics(value: Boolean) {
        _uiState.update { it.copy(includeDiagnostics = value) }
    }

    fun showResetConfirmation(show: Boolean) {
        _uiState.update { it.copy(showResetDialog = show) }
    }

    fun showImportConfirmation(uri: Uri?) {
        _uiState.update {
            it.copy(
                showImportConfirmDialog = (uri != null),
                pendingImportUri = uri
            )
        }
    }

    fun confirmReset() {
        showResetConfirmation(false)
        _uiState.update { it.copy(isProcessing = true, userMessage = null) }
        viewModelScope.launch {
            try {
                backupRepository.resetToDefaultData()
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        userMessage = "RESET_SUCCESS",
                        isError = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        userMessage = e.localizedMessage ?: "Reset failed",
                        isError = true
                    )
                }
            }
        }
    }

    fun exportBackup(uri: Uri, contentResolver: ContentResolver) {
        _uiState.update { it.copy(isProcessing = true, userMessage = null) }
        viewModelScope.launch {
            try {
                val outputStream = contentResolver.openOutputStream(uri)
                if (outputStream == null) {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            userMessage = "Could not open file for writing",
                            isError = true
                        )
                    }
                    return@launch
                }
                val options = BackupOptions(
                    includeHistory = _uiState.value.includeHistory,
                    includeDiagnostics = _uiState.value.includeDiagnostics,
                    includeDescriptors = true
                )
                val result = backupRepository.exportBackup(outputStream, options)
                when (result) {
                    is BackupResult.Success -> {
                        _uiState.update {
                            it.copy(
                                isProcessing = false,
                                userMessage = "EXPORT_SUCCESS",
                                isError = false
                            )
                        }
                    }
                    is BackupResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isProcessing = false,
                                userMessage = result.exception.localizedMessage ?: "Operation failed",
                                isError = true
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        userMessage = e.localizedMessage ?: "Export failed",
                        isError = true
                    )
                }
            }
        }
    }

    fun confirmImport(contentResolver: ContentResolver) {
        val uri = _uiState.value.pendingImportUri ?: return
        showImportConfirmation(null)
        _uiState.update { it.copy(isProcessing = true, userMessage = null) }
        viewModelScope.launch {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            userMessage = "Could not open file for reading",
                            isError = true
                        )
                    }
                    return@launch
                }
                val result = backupRepository.importBackup(inputStream)
                when (result) {
                    is BackupResult.Success -> {
                        _uiState.update {
                            it.copy(
                                isProcessing = false,
                                userMessage = "IMPORT_SUCCESS",
                                isError = false
                            )
                        }
                    }
                    is BackupResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isProcessing = false,
                                userMessage = result.exception.localizedMessage ?: "Import failed",
                                isError = true
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        userMessage = e.localizedMessage ?: "Import failed",
                        isError = true
                    )
                }
            }
        }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    companion object {
        class Factory(
            private val registry: SystemRegistry
        ) : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return AppDataViewModel(registry) as T
            }
        }
    }
}