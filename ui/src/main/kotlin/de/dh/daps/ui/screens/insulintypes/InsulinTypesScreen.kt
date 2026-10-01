package de.dh.daps.ui.screens.insulintypes

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.dh.daps.common.model.InsulinType
import de.dh.daps.common.model.data.Minutes
import de.dh.daps.common.model.getDefaultInsulinTypes
import de.dh.daps.ui.R
import de.dh.daps.ui.common.composables.NormalTextButton
import de.dh.daps.ui.common.composables.screenTitle
import de.dh.daps.ui.common.icons.Icon_Insulin
import de.dh.daps.ui.common.theme.AppPreview
import de.dh.daps.common.R as CommonR

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
        onAddStandardInsulinType = { viewModel.addInsulinType(it) },
        onAddCustomInsulinType = { onNavigateToEditor(null) },
        onEditInsulinType = { onNavigateToEditor(it.id) },
        onNavigateUp = onNavigateUp
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsulinTypesContent(
    uiState: InsulinTypesUiState,
    onDeleteInsulinType: (InsulinType) -> Unit,
    onAddStandardInsulinType: (InsulinType) -> Unit,
    onAddCustomInsulinType: () -> Unit,
    onEditInsulinType: (InsulinType) -> Unit,
    onNavigateUp: () -> Unit
) {
    var insulinTypeToDelete by remember { mutableStateOf<InsulinType?>(null) }
    var showAddMenu by remember { mutableStateOf(false) }

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
            Box {
                FloatingActionButton(onClick = { showAddMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(id = R.string.cd_add_insulin_type)
                    )
                }
                DropdownMenu(
                    expanded = showAddMenu,
                    onDismissRequest = { showAddMenu = false }
                ) {
                    val context = LocalContext.current
                    val allStandardTypes = remember(context) { getDefaultInsulinTypes(context) }
                    val availableStandardTypes = remember(allStandardTypes, uiState.insulinTypes) {
                        allStandardTypes.filter { std ->
                            uiState.insulinTypes.none { existing ->
                                existing.id == std.id || existing.name.equals(std.name, ignoreCase = true)
                            }
                        }
                    }

                    if (availableStandardTypes.isNotEmpty()) {
                        availableStandardTypes.forEach { standardType ->
                            DropdownMenuItem(
                                text = { Text(standardType.name) },
                                onClick = {
                                    showAddMenu = false
                                    onAddStandardInsulinType(standardType)
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icon_Insulin,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            )
                        }
                        HorizontalDivider()
                    }

                    DropdownMenuItem(
                        text = { Text(stringResource(id = R.string.insulin_type_custom)) },
                        onClick = {
                            showAddMenu = false
                            onAddCustomInsulinType()
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null
                            )
                        }
                    )
                }
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
                    val isInUse = uiState.isTypeInUse(insulinType)
                    val canDelete = uiState.canDeleteType(insulinType)
                    InsulinTypeItem(
                        insulinType = insulinType,
                        isInUse = isInUse,
                        canDelete = canDelete,
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
    isInUse: Boolean,
    canDelete: Boolean,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val concStr = "U${(insulinType.defaultConcentration.factor * 100).toInt()}"
    val detailsStr = "Peak: ${insulinType.peak.value} Min. | DIA: ${insulinType.dia.value} Min. | $concStr"

    ListItem(
        headlineContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(insulinType.name)
                if (isInUse) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.insulin_type_in_use_label),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        },
        supportingContent = { Text(detailsStr) },
        leadingContent = {
            Icon(
                imageVector = Icon_Insulin,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingContent = if (canDelete) {
            {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(id = CommonR.string.action_delete)
                    )
                }
            }
        } else null,
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Preview(showBackground = true, name = "Light Mode")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Dark Mode")
@Composable
fun InsulinTypesPreview() {
    val fiasp = InsulinType(id = "1", name = "Fiasp", peak = Minutes(50), dia = Minutes(300))
    val novorapid = InsulinType(id = "2", name = "NovoRapid", peak = Minutes(75), dia = Minutes(360))
    AppPreview {
        InsulinTypesContent(
            uiState = InsulinTypesUiState(
                insulinTypes = listOf(fiasp, novorapid),
                usedInsulinTypeIds = setOf("1")
            ),
            onDeleteInsulinType = {},
            onAddStandardInsulinType = {},
            onAddCustomInsulinType = {},
            onEditInsulinType = {},
            onNavigateUp = {}
        )
    }
}