package de.dh.daps.ui.screens.setupwizard.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.dh.daps.common.model.InsulinConcentration
import de.dh.daps.common.model.InsulinType
import de.dh.daps.ui.R
import de.dh.daps.ui.common.composables.contentScrollIndicator
import de.dh.daps.ui.screens.insulintypes.InsulinTypeEditorFormContent
import de.dh.daps.ui.screens.insulintypes.InsulinTypeEditorUiState
import de.dh.daps.ui.screens.setupwizard.components.SetupStepScaffold

@Composable
fun InsulinTypeStep(
    availableTypes: List<InsulinType>,
    selectedType: InsulinType?,
    isEditing: Boolean,
    editorUiState: InsulinTypeEditorUiState,
    onSelectType: (InsulinType) -> Unit,
    onStartEditing: (InsulinType?) -> Unit,
    onCancelEditing: () -> Unit,
    onUpdateName: (String) -> Unit,
    onUpdatePeak: (String) -> Unit,
    onUpdateDia: (String) -> Unit,
    onUpdateConcentration: (InsulinConcentration) -> Unit,
    onSaveEditedType: () -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    if (isEditing) {
        val titleRes = if (editorUiState.id == null) {
            R.string.insulin_type_editor_title_new
        } else {
            R.string.insulin_type_editor_title_edit
        }

        SetupStepScaffold(
            title = stringResource(titleRes),
            showBackButton = true,
            onBack = onCancelEditing,
            nextButtonText = stringResource(de.dh.daps.common.R.string.action_save),
            onNext = onSaveEditedType,
            isNextEnabled = editorUiState.isValid,
            snackbarHostState = snackbarHostState
        ) { innerPadding ->
            InsulinTypeEditorFormContent(
                uiState = editorUiState,
                onNameChange = onUpdateName,
                onPeakChange = onUpdatePeak,
                onDiaChange = onUpdateDia,
                onConcentrationChange = onUpdateConcentration,
                modifier = Modifier.padding(innerPadding)
            )
        }
    } else {
        SetupStepScaffold(
            title = stringResource(R.string.setup_wizard_step2_insulin_title),
            showBackButton = true,
            onBack = onBack,
            onNext = onNext,
            isNextEnabled = selectedType != null,
            snackbarHostState = snackbarHostState
        ) {
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .contentScrollIndicator(scrollState)
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.setup_wizard_step2_insulin_desc),
                    style = MaterialTheme.typography.bodyMedium
                )

                Column(
                    modifier = Modifier.selectableGroup(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableTypes.forEach { type ->
                        val isSelected = type.id == selectedType?.id
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = isSelected,
                                    onClick = { onSelectType(type) },
                                    role = Role.RadioButton
                                ),
                            colors = if (isSelected) {
                                CardDefaults.outlinedCardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                )
                            } else CardDefaults.outlinedCardColors()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = null
                                )
                                Spacer(Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = type.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "DIA: ${type.dia.value} min, Peak: ${type.peak.value} min (U${(type.defaultConcentration.factor * 100).toInt()})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { onStartEditing(type) }) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = { onStartEditing(null) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.setup_wizard_step2_insulin_custom_btn))
                }
            }
        }
    }
}