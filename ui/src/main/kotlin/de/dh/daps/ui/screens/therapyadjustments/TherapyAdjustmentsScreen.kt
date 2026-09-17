package de.dh.daps.ui.screens.therapyadjustments

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.runtime.CompositionLocalProvider
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
import de.dh.daps.common.model.data.BgValue
import de.dh.daps.common.model.data.GlucoseUnit
import de.dh.daps.common.model.data.TherapyAdjustment
import de.dh.daps.ui.R
import de.dh.daps.ui.common.ConfigurableDisplayStrategy
import de.dh.daps.ui.common.LocalGlucoseUnit
import de.dh.daps.ui.common.composables.NormalTextButton
import de.dh.daps.ui.common.composables.contentScrollIndicator
import de.dh.daps.ui.common.composables.screenTitle
import de.dh.daps.ui.common.glucoseValue
import de.dh.daps.ui.common.theme.AppTheme
import de.dh.daps.ui.common.theme.NeutralGrey
import de.dh.daps.ui.common.theme.SoftBlue
import de.dh.daps.ui.common.theme.SoftRed
import de.dh.daps.common.R as CommonR

@Composable
fun TherapyAdjustmentsScreen(
    viewModel: TherapyAdjustmentsViewModel,
    onNavigateToEditor: (Long?) -> Unit,
    onNavigateUp: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    TherapyAdjustmentsContent(
        uiState = uiState,
        onDeleteAdjustment = { viewModel.deleteAdjustment(it) },
        onAddAdjustment = { onNavigateToEditor(null) },
        onEditAdjustment = { onNavigateToEditor(it.id) },
        onNavigateUp = onNavigateUp
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TherapyAdjustmentsContent(
    uiState: TherapyAdjustmentsUiState,
    onDeleteAdjustment: (TherapyAdjustment) -> Unit,
    onAddAdjustment: () -> Unit,
    onEditAdjustment: (TherapyAdjustment) -> Unit,
    onNavigateUp: () -> Unit
) {
    var adjustmentToDelete by remember { mutableStateOf<TherapyAdjustment?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = screenTitle(stringResource(id = R.string.therapy_adjustments_screen_title)),
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
            FloatingActionButton(onClick = onAddAdjustment) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(id = R.string.cd_add_therapy_adjustment)
                )
            }
        }
    ) { innerPadding ->
        if (uiState.adjustments.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(id = R.string.therapy_adjustments_empty_list),
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
                items(uiState.adjustments) { adjustment ->
                    TherapyAdjustmentListItem(
                        adjustment = adjustment,
                        onDelete = { adjustmentToDelete = adjustment },
                        onClick = { onEditAdjustment(adjustment) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    val targetAdjustment = adjustmentToDelete
    if (targetAdjustment != null) {
        AlertDialog(
            onDismissRequest = { adjustmentToDelete = null },
            title = { Text(stringResource(id = R.string.delete_therapy_adjustment_title)) },
            text = { Text(stringResource(id = R.string.delete_therapy_adjustment_message, targetAdjustment.name)) },
            confirmButton = {
                NormalTextButton(onClick = {
                    adjustmentToDelete = null
                    onDeleteAdjustment(targetAdjustment)
                }) {
                    Text(stringResource(id = CommonR.string.action_delete))
                }
            },
            dismissButton = {
                NormalTextButton(onClick = { adjustmentToDelete = null }) {
                    Text(stringResource(id = android.R.string.cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TherapyAdjustmentListItem(
    adjustment: TherapyAdjustment,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val displayStrategyInsulin = remember {
        ConfigurableDisplayStrategy(
            positiveColor = SoftRed,
            negativeColor = SoftBlue,
            neutralColor = NeutralGrey,
            positivePrefix = "+",
            suffix = "%"
        )
    }

    ListItem(
        headlineContent = { Text(adjustment.name) },
        supportingContent = {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = displayStrategyInsulin.format(adjustment.percentage.toDouble()),
                    color = displayStrategyInsulin.color(adjustment.percentage.toDouble())
                )
                val targetBg = adjustment.targetBgMgDl
                if (targetBg != null) {
                    val targetValue = BgValue.fromMgDl(targetBg.toInt())
                    Text(
                        text = "•\u00A0Ziel:\u00A0${glucoseValue(targetValue, withUnit = true).replace(' ', '\u00A0')}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                val lowThreshold = adjustment.lowThresholdMgDl
                if (lowThreshold != null) {
                    val lowValue = BgValue.fromMgDl(lowThreshold.toInt())
                    Text(
                        text = "•\u00A0Low:\u00A0${glucoseValue(lowValue, withUnit = true).replace(' ', '\u00A0')}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        leadingContent = {
            Icon(
                imageVector = Icons.Default.Tune,
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
fun TherapyAdjustmentsPreview() {
    AppTheme {
        CompositionLocalProvider(LocalGlucoseUnit provides GlucoseUnit.MG_DL) {
            TherapyAdjustmentsContent(
                uiState = TherapyAdjustmentsUiState(
                    adjustments = listOf(
                        TherapyAdjustment(name = "Fahrrad fahren", percentage = -30, targetBgMgDl = 150, lowThresholdMgDl = 100),
                        TherapyAdjustment(name = "Stress", percentage = 20, targetBgMgDl = 115, lowThresholdMgDl = 75)
                    )
                ),
                onDeleteAdjustment = {},
                onAddAdjustment = {},
                onEditAdjustment = {},
                onNavigateUp = {}
            )
        }
    }
}