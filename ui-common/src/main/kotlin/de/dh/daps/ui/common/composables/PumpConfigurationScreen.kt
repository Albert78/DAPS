package de.dh.daps.ui.common.composables

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.dh.daps.common.domain.model.therapy.InsulinConcentration
import de.dh.daps.common.domain.model.therapy.InsulinType
import de.dh.daps.common.domain.model.therapy.getDefaultInsulinTypes
import de.dh.daps.ui.common.R
import de.dh.daps.ui.common.theme.AppPreview
import de.dh.daps.common.R as CommonR

val DEFAULT_INSULIN_CONCENTRATION_OPTIONS = listOf(
    InsulinConcentration.U200 to "U200",
    InsulinConcentration.U100 to "U100",
    InsulinConcentration.U40 to "U40",
    InsulinConcentration.U20 to "U20",
)

/**
 * Card displaying insulin type selection options.
 */
@Composable
fun InsulinTypeCard(
    selectedTypeId: String?,
    availableTypes: List<InsulinType>,
    onSelectType: (InsulinType) -> Unit,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.pump_config_insulin_type_title),
    subtitle: String = stringResource(R.string.pump_config_insulin_type_subtitle),
) {
    FramedCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableTypes.forEach { insulinType ->
                    val isSelected = selectedTypeId == insulinType.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectType(insulinType) },
                        label = { Text(text = insulinType.name) }
                    )
                }
            }
        }
    }
}

/**
 * Card displaying insulin concentration selection options.
 */
@Composable
fun InsulinConcentrationCard(
    selectedConcentration: InsulinConcentration,
    onSelectConcentration: (InsulinConcentration) -> Unit,
    modifier: Modifier = Modifier,
    options: List<Pair<InsulinConcentration, String>> = DEFAULT_INSULIN_CONCENTRATION_OPTIONS,
    title: String = stringResource(R.string.pump_config_concentration_title),
    subtitle: String = stringResource(R.string.pump_config_concentration_subtitle),
) {
    FramedCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                options.forEach { (concentration, label) ->
                    val isSelected = selectedConcentration == concentration
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectConcentration(concentration) },
                        label = { Text(text = label) }
                    )
                }
            }
        }
    }
}

/**
 * Generic container screen layout for pump configuration screens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PumpConfigurationScreen(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = screenTitle(title),
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(CommonR.string.cd_close)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                content = content
            )
        }
    }
}

/**
 * Generic pump configuration screen composable with standard insulin type and concentration cards.
 */
@Composable
fun PumpConfigurationScreen(
    title: String,
    selectedTypeId: String?,
    availableTypes: List<InsulinType>,
    onSelectType: (InsulinType) -> Unit,
    selectedConcentration: InsulinConcentration,
    onSelectConcentration: (InsulinConcentration) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    typeCardTitle: String = stringResource(R.string.pump_config_insulin_type_title),
    typeCardSubtitle: String = stringResource(R.string.pump_config_insulin_type_subtitle),
    concentrationCardTitle: String = stringResource(R.string.pump_config_concentration_title),
    concentrationCardSubtitle: String = stringResource(R.string.pump_config_concentration_subtitle),
    additionalContent: (@Composable ColumnScope.() -> Unit)? = null,
) {
    PumpConfigurationScreen(
        title = title,
        onClose = onClose,
        modifier = modifier
    ) {
        InsulinTypeCard(
            selectedTypeId = selectedTypeId,
            availableTypes = availableTypes,
            onSelectType = onSelectType,
            title = typeCardTitle,
            subtitle = typeCardSubtitle,
        )
        InsulinConcentrationCard(
            selectedConcentration = selectedConcentration,
            onSelectConcentration = onSelectConcentration,
            title = concentrationCardTitle,
            subtitle = concentrationCardSubtitle,
        )
        additionalContent?.invoke(this)
    }
}

@Preview(name = "Pump Config Screen - Light", showBackground = true)
@Preview(name = "Pump Config Screen - Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PumpConfigurationScreenPreview() {
    val context = LocalContext.current
    val types = remember(context) { getDefaultInsulinTypes(context) }
    AppPreview {
        PumpConfigurationScreen(
            title = "Pump Configuration",
            selectedTypeId = types.firstOrNull()?.id,
            availableTypes = types,
            onSelectType = {},
            selectedConcentration = InsulinConcentration.U100,
            onSelectConcentration = {},
            onClose = {}
        )
    }
}