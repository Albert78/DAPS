package de.dh.daps.ui.screens.retention

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import de.dh.daps.common.domain.model.retention.DataRetentionCategory
import de.dh.daps.common.domain.model.retention.RetentionOption
import de.dh.daps.common.domain.model.retention.allowedOptions
import de.dh.daps.ui.R
import de.dh.daps.ui.common.composables.FramedCard
import de.dh.daps.ui.common.composables.PrimaryButton
import de.dh.daps.ui.common.composables.contentScrollIndicator
import de.dh.daps.ui.common.composables.screenTitle
import de.dh.daps.ui.common.icons.Icon_Screen_Back
import de.dh.daps.ui.common.theme.AppPreview
import de.dh.daps.common.R as CommonR

@Composable
fun DataRetentionScreen(
    viewModel: DataRetentionViewModel,
    onNavigateUp: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val msgCleanupSuccess = stringResource(R.string.data_retention_msg_cleanup_success)
    val msgErrorFormat = stringResource(R.string.app_data_msg_error)

    LaunchedEffect(uiState.userMessage) {
        val msg = uiState.userMessage ?: return@LaunchedEffect
        val text = when (msg) {
            "CLEANUP_SUCCESS" -> msgCleanupSuccess
            else -> String.format(msgErrorFormat, msg)
        }
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        viewModel.clearUserMessage()
    }

    DataRetentionContent(
        uiState = uiState,
        onNavigateUp = onNavigateUp,
        onOptionSelected = { category, option ->
            viewModel.updateRetentionOption(category, option)
        },
        onCleanupClick = {
            viewModel.executeManualCleanup()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataRetentionContent(
    uiState: DataRetentionUiState,
    onNavigateUp: () -> Unit,
    onOptionSelected: (DataRetentionCategory, RetentionOption) -> Unit,
    onCleanupClick: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            LargeTopAppBar(
                title = screenTitle(stringResource(R.string.data_retention_screen_title)),
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icon_Screen_Back,
                            contentDescription = stringResource(CommonR.string.cd_navigate_up)
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        val scrollState = rememberScrollState()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .contentScrollIndicator(scrollState)
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section: Category Policies
                Text(
                    text = stringResource(R.string.data_retention_section_categories_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                FramedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        DataRetentionCategory.entries.forEachIndexed { index, category ->
                            val currentOption = uiState.settings.getOption(category)

                            RetentionCategoryRow(
                                category = category,
                                selectedOption = currentOption,
                                onOptionSelected = { option ->
                                    onOptionSelected(category, option)
                                }
                            )

                            if (index < DataRetentionCategory.entries.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                            }
                        }
                    }
                }

                // Section: Manual Actions
                Text(
                    text = stringResource(R.string.data_retention_section_actions_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                FramedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CleaningServices,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = stringResource(R.string.data_retention_card_cleanup_title),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.data_retention_card_cleanup_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        PrimaryButton(
                            onClick = onCleanupClick,
                            enabled = !uiState.isExecutingCleanup,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            if (uiState.isExecutingCleanup) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(stringResource(R.string.data_retention_btn_cleanup_now))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RetentionCategoryRow(
    category: DataRetentionCategory,
    selectedOption: RetentionOption,
    onOptionSelected: (RetentionOption) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = category.toDisplayString(),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedOption.toDisplayString(),
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                category.allowedOptions().forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.toDisplayString()) },
                        onClick = {
                            onOptionSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DataRetentionCategory.toDisplayString(): String = when (this) {
    DataRetentionCategory.GLUCOSE_READINGS -> stringResource(R.string.data_retention_category_glucose)
    DataRetentionCategory.THERAPY_HISTORY -> stringResource(R.string.data_retention_category_therapy)
    DataRetentionCategory.DEVICE_HARDWARE_LOGS -> stringResource(R.string.data_retention_category_hardware_logs)
    DataRetentionCategory.REMINDERS -> stringResource(R.string.data_retention_category_reminders)
    DataRetentionCategory.SYSTEM_METRICS -> stringResource(R.string.data_retention_category_system_metrics)
    DataRetentionCategory.OTHER -> stringResource(R.string.data_retention_category_other)
}

@Composable
private fun RetentionOption.toDisplayString(): String = when (this) {
    RetentionOption.DAY_1 -> stringResource(R.string.data_retention_option_1_day)
    RetentionOption.DAYS_3 -> stringResource(R.string.data_retention_option_3_days)
    RetentionOption.DAYS_7 -> stringResource(R.string.data_retention_option_7_days)
    RetentionOption.DAYS_14 -> stringResource(R.string.data_retention_option_14_days)
    RetentionOption.DAYS_30 -> stringResource(R.string.data_retention_option_30_days)
    RetentionOption.DAYS_90 -> stringResource(R.string.data_retention_option_90_days)
    RetentionOption.DAYS_180 -> stringResource(R.string.data_retention_option_180_days)
    RetentionOption.UNLIMITED -> stringResource(R.string.data_retention_option_unlimited)
}

@Preview(showBackground = true, heightDp = 1000)
@Preview(showBackground = true, heightDp = 1000, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DataRetentionContentPreview() {
    AppPreview {
        DataRetentionContent(
            uiState = DataRetentionUiState(),
            onNavigateUp = {},
            onOptionSelected = { _, _ -> },
            onCleanupClick = {}
        )
    }
}