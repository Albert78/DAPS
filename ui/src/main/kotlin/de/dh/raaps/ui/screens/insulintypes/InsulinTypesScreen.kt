package de.dh.raaps.ui.screens.insulintypes

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import de.dh.raaps.common.model.InsulinType
import de.dh.raaps.common.model.data.Minutes
import de.dh.raaps.ui.R
import de.dh.raaps.ui.common.composables.NormalTextButton
import de.dh.raaps.ui.common.composables.screenTitle
import de.dh.raaps.ui.common.icons.Icon_Insulin
import de.dh.raaps.ui.common.theme.AppTheme
import de.dh.raaps.common.R as CommonR

@Composable
fun InsulinTypesScreen(
    viewModel: InsulinTypesViewModel,
    onNavigateToEditor: (String?) -> Unit,
    onNavigateUp: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    InsulinTypesContent(
        uiState = uiState,
        onDeleteInsulinType = { viewModel.deleteInsulinType(it) },
        onAddInsulinType = { onNavigateToEditor(null) },
        onEditInsulinType = { onNavigateToEditor(it.id) },
        onNavigateUp = onNavigateUp
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsulinTypesContent(
    uiState: InsulinTypesUiState,
    onDeleteInsulinType: (InsulinType) -> Unit,
    onAddInsulinType: () -> Unit,
    onEditInsulinType: (InsulinType) -> Unit,
    onNavigateUp: () -> Unit
) {
    var insulinTypeToDelete by remember { mutableStateOf<InsulinType?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = screenTitle(stringResource(id = R.string.insulin_types_screen_title)),
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = CommonR.string.cd_navigate_up)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddInsulinType) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(id = R.string.cd_add_insulin_type)
                )
            }
        }
    ) { innerPadding ->
        if (uiState.insulinTypes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Keine Insulintypen definiert",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                items(uiState.insulinTypes) { insulinType ->
                    InsulinTypeItem(
                        insulinType = insulinType,
                        onDelete = { insulinTypeToDelete = insulinType },
                        onClick = { onEditInsulinType(insulinType) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    val targetInsulinType = insulinTypeToDelete
    if (targetInsulinType != null) {
        AlertDialog(
            onDismissRequest = { insulinTypeToDelete = null },
            title = { Text(stringResource(id = R.string.delete_insulin_type_title)) },
            text = { Text(stringResource(id = R.string.delete_insulin_type_message, targetInsulinType.name)) },
            confirmButton = {
                NormalTextButton(onClick = {
                    insulinTypeToDelete = null
                    onDeleteInsulinType(targetInsulinType)
                }) {
                    Text(stringResource(id = CommonR.string.action_delete))
                }
            },
            dismissButton = {
                NormalTextButton(onClick = { insulinTypeToDelete = null }) {
                    Text(stringResource(id = android.R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun InsulinTypeItem(
    insulinType: InsulinType,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val concStr = "U${(insulinType.defaultConcentration.factor * 100).toInt()}"
    val detailsStr = "Peak: ${insulinType.peak.value} Min. | DIA: ${insulinType.dia.value} Min. | $concStr"

    ListItem(
        headlineContent = { Text(insulinType.name) },
        supportingContent = { Text(detailsStr) },
        leadingContent = {
            Icon(
                imageVector = Icon_Insulin,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(id = CommonR.string.action_delete)
                )
            }
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Preview(showBackground = true, name = "Light Mode")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Dark Mode")
@Composable
fun InsulinTypesPreview() {
    AppTheme {
        InsulinTypesContent(
            uiState = InsulinTypesUiState(
                insulinTypes = listOf(
                    InsulinType(name = "Fiasp", peak = Minutes(50), dia = Minutes(300)),
                    InsulinType(name = "NovoRapid", peak = Minutes(75), dia = Minutes(360))
                )
            ),
            onDeleteInsulinType = {},
            onAddInsulinType = {},
            onEditInsulinType = {},
            onNavigateUp = {}
        )
    }
}