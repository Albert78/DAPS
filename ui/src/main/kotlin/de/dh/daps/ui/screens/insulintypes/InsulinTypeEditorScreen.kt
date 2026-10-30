package de.dh.daps.ui.screens.insulintypes

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.dh.daps.ui.R
import de.dh.daps.ui.common.composables.AppAlertDialog
import de.dh.daps.ui.common.composables.NormalTextButton
import de.dh.daps.ui.common.composables.contentScrollIndicator
import de.dh.daps.ui.common.composables.screenTitle
import de.dh.daps.ui.common.theme.AppPreview
import de.dh.daps.common.R as CommonR

private data class InitialInsulinTypeValues(
    val id: String?,
    val name: String,
    val activeSubstance: String?,
    val peak: String,
    val dia: String
)

@Composable
fun InsulinTypeEditorScreen(
    viewModel: InsulinTypeEditorViewModel,
    onNavigateUp: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    InsulinTypeEditorContent(
        uiState = uiState,
        onNameChange = viewModel::onNameChange,
        onActiveSubstanceChange = viewModel::onActiveSubstanceChange,
        onPeakChange = viewModel::onPeakChange,
        onDiaChange = viewModel::onDiaChange,
        onSave = {
            viewModel.save(onSuccess = onNavigateUp)
        },
        onNavigateUp = onNavigateUp
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsulinTypeEditorContent(
    uiState: InsulinTypeEditorUiState,
    onNameChange: (String) -> Unit,
    onActiveSubstanceChange: (String) -> Unit,
    onPeakChange: (String) -> Unit,
    onDiaChange: (String) -> Unit,
    onSave: () -> Unit,
    onNavigateUp: () -> Unit
) {
    var showDiscardConfirmation by remember { mutableStateOf(false) }

    val initialValues = remember(uiState.isLoading) {
        if (!uiState.isLoading) {
            InitialInsulinTypeValues(
                id = uiState.id,
                name = uiState.name,
                activeSubstance = uiState.activeSubstance,
                peak = uiState.peak,
                dia = uiState.dia
            )
        } else null
    }

    val hasChanges = remember(
        uiState.name,
        uiState.activeSubstance,
        uiState.peak,
        uiState.dia,
        initialValues
    ) {
        if (initialValues == null) false
        else {
            uiState.name != initialValues.name ||
                    uiState.activeSubstance != initialValues.activeSubstance ||
                    uiState.peak != initialValues.peak ||
                    uiState.dia != initialValues.dia
        }
    }

    fun handleBack() {
        if (hasChanges) {
            showDiscardConfirmation = true
        } else {
            onNavigateUp()
        }
    }

    BackHandler(onBack = ::handleBack)

    val titleRes = if (uiState.id == null) {
        R.string.insulin_type_editor_title_new
    } else {
        R.string.insulin_type_editor_title_edit
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = screenTitle(stringResource(id = titleRes)),
                navigationIcon = {
                    IconButton(onClick = ::handleBack) {
                        Icon(
                            imageVector = if (hasChanges) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(
                                id = if (hasChanges) CommonR.string.cd_cancel
                                else CommonR.string.cd_navigate_up
                            )
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onSave,
                        enabled = uiState.isValid && !uiState.isSaving
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = stringResource(id = CommonR.string.action_save)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        InsulinTypeEditorFormContent(
            uiState = uiState,
            onNameChange = onNameChange,
            onActiveSubstanceChange = onActiveSubstanceChange,
            onPeakChange = onPeakChange,
            onDiaChange = onDiaChange,
            modifier = Modifier.padding(innerPadding)
        )
    }

    if (showDiscardConfirmation) {
        DiscardChangesDialog(
            isSaveEnabled = uiState.isValid && !uiState.isSaving,
            onDismissRequest = { showDiscardConfirmation = false },
            onSave = {
                showDiscardConfirmation = false
                onSave()
            },
            onDiscard = {
                showDiscardConfirmation = false
                onNavigateUp()
            }
        )
    }
}

@Composable
private fun DiscardChangesDialog(
    isSaveEnabled: Boolean,
    onDismissRequest: () -> Unit,
    onSave: () -> Unit,
    onDiscard: () -> Unit
) {
    AppAlertDialog(
        onDismissRequest = onDismissRequest,
        titleText = stringResource(id = R.string.insulin_type_editor_discard_title),
        content = { Text(stringResource(id = R.string.insulin_type_editor_discard_message)) },
        confirmButton = {
            NormalTextButton(
                onClick = onSave,
                enabled = isSaveEnabled
            ) {
                Text(stringResource(id = CommonR.string.action_save))
            }
        },
        dismissButton = {
            NormalTextButton(onClick = onDiscard) {
                Text(stringResource(id = R.string.discard_confirm_button))
            }
        }
    )
}

@Composable
fun InsulinTypeEditorFormContent(
    uiState: InsulinTypeEditorUiState,
    onNameChange: (String) -> Unit,
    onActiveSubstanceChange: (String) -> Unit,
    onPeakChange: (String) -> Unit,
    onDiaChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .contentScrollIndicator(scrollState)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = uiState.name,
            onValueChange = onNameChange,
            label = { Text(stringResource(id = R.string.insulin_type_editor_name_label)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Next
            )
        )

        OutlinedTextField(
            value = uiState.activeSubstance ?: "",
            onValueChange = onActiveSubstanceChange,
            label = { Text(stringResource(id = R.string.insulin_type_editor_active_substance_label)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Next
            )
        )

        OutlinedTextField(
            value = uiState.peak,
            onValueChange = onPeakChange,
            label = { Text(stringResource(id = R.string.insulin_type_editor_peak_label)) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = uiState.dia,
            onValueChange = onDiaChange,
            label = { Text(stringResource(id = R.string.insulin_type_editor_dia_label)) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}

@Preview(showBackground = true, name = "Light Mode")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Dark Mode")
@Composable
fun InsulinTypeEditorPreview() {
    AppPreview {
        InsulinTypeEditorContent(
            uiState = InsulinTypeEditorUiState(
                id = "1",
                name = "Fiasp",
                activeSubstance = "Insulin aspart",
                peak = "50",
                dia = "300"
            ),
            onNameChange = {},
            onActiveSubstanceChange = {},
            onPeakChange = {},
            onDiaChange = {},
            onSave = {},
            onNavigateUp = {}
        )
    }
}

@Preview(showBackground = true, name = "Form Content Light Mode")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Form Content Dark Mode")
@Composable
fun InsulinTypeEditorFormContentPreview() {
    AppPreview {
        InsulinTypeEditorFormContent(
            uiState = InsulinTypeEditorUiState(
                id = "1",
                name = "Fiasp",
                activeSubstance = "Insulin aspart",
                peak = "50",
                dia = "300"
            ),
            onNameChange = {},
            onActiveSubstanceChange = {},
            onPeakChange = {},
            onDiaChange = {}
        )
    }
}

@Preview(showSystemUi = true, name = "Discard Changes Dialog")
@Composable
private fun DiscardChangesDialogPreview() {
    AppPreview {
        DiscardChangesDialog(
            isSaveEnabled = true,
            onDismissRequest = {},
            onSave = {},
            onDiscard = {}
        )
    }
}