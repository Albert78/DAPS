package de.dh.raaps.ui.screens.insulintypes

import android.content.res.Configuration
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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExposedDropdownMenuAnchorType
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.dh.raaps.common.model.InsulinConcentration
import de.dh.raaps.ui.R
import de.dh.raaps.ui.common.composables.contentScrollIndicator
import de.dh.raaps.ui.common.composables.screenTitle
import de.dh.raaps.ui.common.theme.AppTheme
import de.dh.raaps.common.R as CommonR

@Composable
fun InsulinTypeEditorScreen(
    viewModel: InsulinTypeEditorViewModel,
    onNavigateUp: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    InsulinTypeEditorContent(
        uiState = uiState,
        onNameChange = viewModel::onNameChange,
        onPeakChange = viewModel::onPeakChange,
        onDiaChange = viewModel::onDiaChange,
        onConcentrationChange = viewModel::onConcentrationChange,
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
    onPeakChange: (String) -> Unit,
    onDiaChange: (String) -> Unit,
    onConcentrationChange: (InsulinConcentration) -> Unit,
    onSave: () -> Unit,
    onNavigateUp: () -> Unit
) {
    val scrollState = rememberScrollState()
    var expandedConc by remember { mutableStateOf(false) }

    val concentrations = listOf(
        InsulinConcentration.U20,
        InsulinConcentration.U40,
        InsulinConcentration.U100,
        InsulinConcentration.U200
    )

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
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = CommonR.string.cd_navigate_up)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
                singleLine = true
            )

            OutlinedTextField(
                value = uiState.peak,
                onValueChange = onPeakChange,
                label = { Text(stringResource(id = R.string.insulin_type_editor_peak_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = uiState.dia,
                onValueChange = onDiaChange,
                label = { Text(stringResource(id = R.string.insulin_type_editor_dia_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            ExposedDropdownMenuBox(
                expanded = expandedConc,
                onExpandedChange = { expandedConc = !expandedConc }
            ) {
                OutlinedTextField(
                    value = "U${(uiState.concentration.factor * 100).toInt()}",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(id = R.string.insulin_type_editor_concentration_label)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedConc) },
                    modifier = Modifier
                        .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = expandedConc,
                    onDismissRequest = { expandedConc = false }
                ) {
                    concentrations.forEach { conc ->
                        DropdownMenuItem(
                            text = { Text("U${(conc.factor * 100).toInt()}") },
                            onClick = {
                                onConcentrationChange(conc)
                                expandedConc = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Light Mode")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Dark Mode")
@Composable
fun InsulinTypeEditorPreview() {
    AppTheme {
        InsulinTypeEditorContent(
            uiState = InsulinTypeEditorUiState(
                id = "1",
                name = "Fiasp",
                peak = "50",
                dia = "300"
            ),
            onNameChange = {},
            onPeakChange = {},
            onDiaChange = {},
            onConcentrationChange = {},
            onSave = {},
            onNavigateUp = {}
        )
    }
}