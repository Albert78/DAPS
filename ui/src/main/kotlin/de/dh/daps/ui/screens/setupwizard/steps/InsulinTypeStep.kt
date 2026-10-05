package de.dh.daps.ui.screens.setupwizard.steps

import android.content.res.Configuration
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.dh.daps.common.model.InsulinConcentration
import de.dh.daps.common.model.InsulinType
import de.dh.daps.common.model.data.Minutes
import de.dh.daps.ui.R
import de.dh.daps.ui.common.composables.FramedCard
import de.dh.daps.ui.common.composables.contentScrollIndicator
import de.dh.daps.ui.common.theme.AppPreview
import de.dh.daps.ui.screens.insulintypes.InsulinTypeEditorFormContent
import de.dh.daps.ui.screens.insulintypes.InsulinTypeEditorUiState
import de.dh.daps.ui.screens.setupwizard.components.SetupStepScaffold

@Composable
fun InsulinTypeStep(
    availableTypes: List<InsulinType>,
    selectedTypeIds: Set<String>,
    primaryTypeId: String?,
    isEditing: Boolean,
    editorUiState: InsulinTypeEditorUiState,
    onToggleTypeSelection: (InsulinType) -> Unit,
    onSelectPrimaryType: (InsulinType) -> Unit,
    onStartEditing: (InsulinType?) -> Unit,
    onCancelEditing: () -> Unit,
    onUpdateName: (String) -> Unit,
    onUpdateActiveSubstance: (String) -> Unit = {},
    onUpdatePeak: (String) -> Unit,
    onUpdateDia: (String) -> Unit,
    onUpdateConcentration: (InsulinConcentration) -> Unit,
    onSaveEditedType: () -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    stepProgress: Pair<Int, Int>? = null
) {
    if (isEditing) {
        val titleRes = if (editorUiState.id == null) {
            R.string.insulin_type_editor_title_new
        } else {
            R.string.insulin_type_editor_title_edit
        }

        SetupStepScaffold(
            title = stringResource(titleRes),
            stepProgress = stepProgress,
            showTopBackButton = true,
            useCloseIcon = true,
            onTopBack = onCancelEditing,
            showBottomBackButton = true,
            onBack = onCancelEditing,
            nextButtonText = stringResource(de.dh.daps.common.R.string.action_save),
            onNext = onSaveEditedType,
            isNextEnabled = editorUiState.isValid,
            snackbarHostState = snackbarHostState
        ) { innerPadding ->
            InsulinTypeEditorFormContent(
                uiState = editorUiState,
                onNameChange = onUpdateName,
                onActiveSubstanceChange = onUpdateActiveSubstance,
                onPeakChange = onUpdatePeak,
                onDiaChange = onUpdateDia,
                onConcentrationChange = onUpdateConcentration,
                modifier = Modifier.padding(innerPadding)
            )
        }
    } else {
        val isNextEnabled = selectedTypeIds.isNotEmpty() && primaryTypeId != null

        SetupStepScaffold(
            title = stringResource(R.string.setup_wizard_insulin_title),
            stepProgress = stepProgress,
            showTopBackButton = false,
            showBottomBackButton = true,
            onBack = onBack,
            onNext = onNext,
            isNextEnabled = isNextEnabled,
            snackbarHostState = snackbarHostState
        ) { innerPadding ->
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .contentScrollIndicator(scrollState)
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.setup_wizard_insulin_desc),
                    style = MaterialTheme.typography.bodyMedium
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableTypes.forEach { type ->
                        val isSelected = type.id in selectedTypeIds
                        val isPrimary = type.id == primaryTypeId

                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = isSelected,
                                    onClick = { onToggleTypeSelection(type) },
                                    role = Role.Checkbox
                                ),
                            colors = if (isSelected) {
                                CardDefaults.outlinedCardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                                )
                            } else CardDefaults.outlinedCardColors()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = null
                                )
                                Spacer(Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = type.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (isSelected && isPrimary) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.primary,
                                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                                shape = MaterialTheme.shapes.extraSmall
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.setup_wizard_insulin_primary_badge),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    val activeSubstance = type.activeSubstance
                                    if (!activeSubstance.isNullOrBlank()) {
                                        Text(
                                            text = activeSubstance,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
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
                    Text(stringResource(R.string.setup_wizard_insulin_custom_btn))
                }

                val selectedTypes = availableTypes.filter { it.id in selectedTypeIds }
                if (selectedTypes.size > 1) {
                    FramedCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.setup_wizard_insulin_primary_header),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.setup_wizard_insulin_primary_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Column(
                                modifier = Modifier.selectableGroup(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                selectedTypes.forEach { type ->
                                    val isPrimary = type.id == primaryTypeId
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .selectable(
                                                selected = isPrimary,
                                                onClick = { onSelectPrimaryType(type) },
                                                role = Role.RadioButton
                                            )
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isPrimary,
                                            onClick = null
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Text(
                                            text = type.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = if (isPrimary) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private val previewInsulinType1 = InsulinType(
    id = "1",
    name = "NovoRapid",
    activeSubstance = "Insulin aspart",
    dia = Minutes.ofHours(5),
    peak = Minutes(75)
)

private val previewInsulinType2 = InsulinType(
    id = "2",
    name = "Fiasp",
    activeSubstance = "Insulin aspart",
    dia = Minutes.ofHours(5),
    peak = Minutes(50)
)

@Preview(showBackground = true, name = "Light Mode")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Dark Mode")
@Composable
private fun InsulinTypeStepPreview() {
    AppPreview {
        InsulinTypeStep(
            availableTypes = listOf(previewInsulinType1, previewInsulinType2),
            selectedTypeIds = setOf("1", "2"),
            primaryTypeId = "1",
            isEditing = false,
            editorUiState = InsulinTypeEditorUiState(),
            onToggleTypeSelection = {},
            onSelectPrimaryType = {},
            onStartEditing = {},
            onCancelEditing = {},
            onUpdateName = {},
            onUpdatePeak = {},
            onUpdateDia = {},
            onUpdateConcentration = {},
            onSaveEditedType = {},
            onNext = {},
            onBack = {},
            snackbarHostState = remember { SnackbarHostState() },
            stepProgress = Pair(2, 7)
        )
    }
}

@Preview(showBackground = true, name = "Editing Insulin Type - Light")
@Composable
private fun InsulinTypeStepEditingPreview() {
    AppPreview {
        InsulinTypeStep(
            availableTypes = listOf(previewInsulinType1),
            selectedTypeIds = setOf("1"),
            primaryTypeId = "1",
            isEditing = true,
            editorUiState = InsulinTypeEditorUiState(
                id = previewInsulinType1.id,
                name = previewInsulinType1.name,
                peak = previewInsulinType1.peak.value.toString(),
                dia = previewInsulinType1.dia.value.toString(),
                concentration = previewInsulinType1.defaultConcentration
            ),
            onToggleTypeSelection = {},
            onSelectPrimaryType = {},
            onStartEditing = {},
            onCancelEditing = {},
            onUpdateName = {},
            onUpdatePeak = {},
            onUpdateDia = {},
            onUpdateConcentration = {},
            onSaveEditedType = {},
            onNext = {},
            onBack = {},
            snackbarHostState = remember { SnackbarHostState() },
            stepProgress = Pair(2, 7)
        )
    }
}