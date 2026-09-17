package de.dh.daps.ui.screens.masterdata

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import de.dh.daps.common.R as CommonR
import de.dh.daps.ui.R
import de.dh.daps.ui.common.composables.screenTitle
import de.dh.daps.ui.common.icons.Icon_Config
import de.dh.daps.ui.common.icons.Icon_Insulin
import de.dh.daps.ui.common.icons.Icon_Menu_Alarms
import de.dh.daps.ui.common.icons.Icon_Menu_Meal_Types
import de.dh.daps.ui.common.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasterDataScreen(
    onNavigateToInsulinTypes: () -> Unit,
    onNavigateToInsulinProfileEditor: () -> Unit,
    onNavigateToBgEditor: () -> Unit,
    onNavigateToMealTypes: () -> Unit,
    onNavigateToAlarmProfiles: () -> Unit,
    onNavigateToTherapyAdjustments: () -> Unit,
    onNavigateUp: () -> Unit
) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = screenTitle(stringResource(id = R.string.master_data_screen_title)),
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = CommonR.string.cd_navigate_up)
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
                .verticalScroll(scrollState)
        ) {
            ListItem(
                headlineContent = { Text(stringResource(id = R.string.master_data_item_insulin_types_title)) },
                supportingContent = { Text(stringResource(id = R.string.master_data_item_insulin_types_desc)) },
                leadingContent = {
                    Icon(
                        imageVector = Icon_Insulin,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingContent = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null
                    )
                },
                modifier = Modifier.clickable(onClick = onNavigateToInsulinTypes)
            )
            HorizontalDivider()

            ListItem(
                headlineContent = { Text(stringResource(id = R.string.master_data_item_insulin_profiles_title)) },
                supportingContent = { Text(stringResource(id = R.string.master_data_item_insulin_profiles_desc)) },
                leadingContent = {
                    Icon(
                        imageVector = Icon_Config,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingContent = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null
                    )
                },
                modifier = Modifier.clickable(onClick = onNavigateToInsulinProfileEditor)
            )
            HorizontalDivider()

            ListItem(
                headlineContent = { Text(stringResource(id = R.string.master_data_item_bg_targets_title)) },
                supportingContent = { Text(stringResource(id = R.string.master_data_item_bg_targets_desc)) },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Adjust,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingContent = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null
                    )
                },
                modifier = Modifier.clickable(onClick = onNavigateToBgEditor)
            )
            HorizontalDivider()

            ListItem(
                headlineContent = { Text(stringResource(id = R.string.master_data_item_meal_types_title)) },
                supportingContent = { Text(stringResource(id = R.string.master_data_item_meal_types_desc)) },
                leadingContent = {
                    Icon(
                        imageVector = Icon_Menu_Meal_Types,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingContent = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null
                    )
                },
                modifier = Modifier.clickable(onClick = onNavigateToMealTypes)
            )
            HorizontalDivider()

            ListItem(
                headlineContent = { Text(stringResource(id = R.string.master_data_item_alarm_profiles_title)) },
                supportingContent = { Text(stringResource(id = R.string.master_data_item_alarm_profiles_desc)) },
                leadingContent = {
                    Icon(
                        imageVector = Icon_Menu_Alarms,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingContent = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null
                    )
                },
                modifier = Modifier.clickable(onClick = onNavigateToAlarmProfiles)
            )
            HorizontalDivider()

            ListItem(
                headlineContent = { Text(stringResource(id = R.string.master_data_item_therapy_adjustments_title)) },
                supportingContent = { Text(stringResource(id = R.string.master_data_item_therapy_adjustments_desc)) },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingContent = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null
                    )
                },
                modifier = Modifier.clickable(onClick = onNavigateToTherapyAdjustments)
            )
            HorizontalDivider()
        }
    }
}

@Preview(showBackground = true, name = "Light Mode")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Dark Mode")
@Composable
fun MasterDataScreenPreview() {
    AppTheme {
        MasterDataScreen(
            onNavigateToInsulinTypes = {},
            onNavigateToInsulinProfileEditor = {},
            onNavigateToBgEditor = {},
            onNavigateToMealTypes = {},
            onNavigateToAlarmProfiles = {},
            onNavigateToTherapyAdjustments = {},
            onNavigateUp = {}
        )
    }
}