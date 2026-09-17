package de.dh.daps.ui.screens.mealtypes

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.ui.unit.dp
import de.dh.daps.common.model.CarbCurveComponentData
import de.dh.daps.common.model.ID_MEAL_FAST
import de.dh.daps.common.model.ID_MEAL_SLOW
import de.dh.daps.common.model.ID_MEAL_STANDARD
import de.dh.daps.common.model.MealType
import de.dh.daps.common.model.data.Minutes
import de.dh.daps.ui.R
import de.dh.daps.ui.common.composables.NormalTextButton
import de.dh.daps.ui.common.composables.contentScrollIndicator
import de.dh.daps.ui.common.composables.screenTitle
import de.dh.daps.ui.common.theme.AppTheme
import de.dh.daps.common.R as CommonR

@Composable
fun MealTypesScreen(
    viewModel: MealTypesViewModel,
    onNavigateToEditor: (String?) -> Unit,
    onNavigateUp: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    MealTypesContent(
        uiState = uiState,
        onDeleteMealType = { viewModel.deleteMealType(it) },
        onMoveMealTypeUp = { viewModel.moveMealTypeUp(it) },
        onMoveMealTypeDown = { viewModel.moveMealTypeDown(it) },
        onAddMealType = { onNavigateToEditor(null) },
        onEditMealType = { onNavigateToEditor(it.id) },
        onNavigateUp = onNavigateUp
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealTypesContent(
    uiState: MealTypesUiState,
    onDeleteMealType: (MealType) -> Unit,
    onMoveMealTypeUp: (MealType) -> Unit,
    onMoveMealTypeDown: (MealType) -> Unit,
    onAddMealType: () -> Unit,
    onEditMealType: (MealType) -> Unit,
    onNavigateUp: () -> Unit
) {
    var mealTypeToDelete by remember { mutableStateOf<MealType?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = screenTitle(stringResource(id = R.string.meal_types_screen_title)),
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
            FloatingActionButton(onClick = onAddMealType) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(id = R.string.cd_add_meal_type)
                )
            }
        }
    ) { innerPadding ->
        if (uiState.mealTypes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(id = R.string.meal_types_empty_list),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            val listState = rememberLazyListState()
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .contentScrollIndicator(listState)
            ) {
                itemsIndexed(
                    items = uiState.mealTypes,
                    key = { _, mealType -> mealType.id }
                ) { index, mealType ->
                    MealTypeItem(
                        mealType = mealType,
                        canMoveUp = index > 0,
                        canMoveDown = index < uiState.mealTypes.size - 1,
                        onMoveUp = { onMoveMealTypeUp(mealType) },
                        onMoveDown = { onMoveMealTypeDown(mealType) },
                        onDelete = { mealTypeToDelete = mealType },
                        onClick = { onEditMealType(mealType) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    val targetMealType = mealTypeToDelete
    if (targetMealType != null) {
        AlertDialog(
            onDismissRequest = { mealTypeToDelete = null },
            title = { Text(stringResource(id = R.string.delete_meal_type_title)) },
            text = { Text(stringResource(id = R.string.delete_meal_type_message, targetMealType.name)) },
            confirmButton = {
                NormalTextButton(onClick = {
                    mealTypeToDelete = null
                    onDeleteMealType(targetMealType)
                }) {
                    Text(stringResource(id = CommonR.string.action_delete))
                }
            },
            dismissButton = {
                NormalTextButton(onClick = { mealTypeToDelete = null }) {
                    Text(stringResource(id = android.R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun MealTypeItem(
    mealType: MealType,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(mealType.name) },
        supportingContent = {
            Text(stringResource(id = CommonR.string.duration_minutes_format, mealType.cat.value))
        },
        leadingContent = {
            MealTypeIcon(
                mealType = mealType,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = stringResource(id = CommonR.string.cd_move_up)
                    )
                }
                IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = stringResource(id = CommonR.string.cd_move_down)
                    )
                }
                if (!mealType.isStandardMealType()) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(id = CommonR.string.action_delete)
                        )
                    }
                }
            }
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Preview(showBackground = true, name = "Empty - Light Mode")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Empty - Dark Mode")
@Composable
fun MealTypesEmptyPreview() {
    AppTheme {
        MealTypesContent(
            uiState = MealTypesUiState(),
            onDeleteMealType = {},
            onMoveMealTypeUp = {},
            onMoveMealTypeDown = {},
            onAddMealType = {},
            onEditMealType = {},
            onNavigateUp = {}
        )
    }
}

private fun getPreviewMealTypes(): List<MealType> = listOf(
    MealType(
        id = ID_MEAL_FAST,
        name = "Schnell",
        components = listOf(CarbCurveComponentData(100, Minutes(30))),
        cat = Minutes(90),
        sortOrder = 0
    ),
    MealType(
        id = ID_MEAL_STANDARD,
        name = "Standard",
        components = listOf(CarbCurveComponentData(100, Minutes(60))),
        cat = Minutes(180),
        sortOrder = 1
    ),
    MealType(
        id = ID_MEAL_SLOW,
        name = "Langsam",
        components = listOf(CarbCurveComponentData(100, Minutes(90))),
        cat = Minutes(240),
        sortOrder = 2
    )
)

@Preview(showBackground = true, name = "With 3 Items - Light Mode")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "With 3 Items - Dark Mode")
@Composable
fun MealTypesWithItemsPreview() {
    AppTheme {
        MealTypesContent(
            uiState = MealTypesUiState(mealTypes = getPreviewMealTypes()),
            onDeleteMealType = {},
            onMoveMealTypeUp = {},
            onMoveMealTypeDown = {},
            onAddMealType = {},
            onEditMealType = {},
            onNavigateUp = {}
        )
    }
}