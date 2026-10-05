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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.dh.daps.common.model.InsulinConcentration
import de.dh.daps.common.model.InsulinType
import de.dh.daps.common.model.data.Block
import de.dh.daps.common.model.data.CarbsUnit
import de.dh.daps.common.model.data.GlucoseUnit
import de.dh.daps.common.model.data.InsulinProfile
import de.dh.daps.common.model.data.Minutes
import de.dh.daps.ui.R
import de.dh.daps.ui.common.carbsUnitLabel
import de.dh.daps.ui.common.composables.FramedCard
import de.dh.daps.ui.common.composables.contentScrollIndicator
import de.dh.daps.ui.common.glucoseUnitLabel
import de.dh.daps.ui.common.theme.AppPreview
import de.dh.daps.ui.screens.setupwizard.SetupWizardUiState
import de.dh.daps.ui.screens.setupwizard.components.SetupStepScaffold

@Composable
fun SummaryStep(
    uiState: SetupWizardUiState,
    cgmDisplayName: String?,
    pumpDisplayName: String?,
    onComplete: () -> Unit,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    stepProgress: Pair<Int, Int>? = null
) {
    SetupStepScaffold(
        title = stringResource(R.string.setup_wizard_summary_title),
        stepProgress = stepProgress,
        showTopBackButton = false,
        showBottomBackButton = true,
        onBack = onBack,
        nextButtonText = stringResource(R.string.setup_wizard_complete_btn),
        onNext = onComplete,
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
            FramedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.setup_wizard_summary_card_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(12.dp))

                    Text(
                        stringResource(
                            R.string.setup_wizard_summary_units_format,
                            glucoseUnitLabel(uiState.glucoseUnit),
                            carbsUnitLabel(uiState.carbsUnit)
                        )
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    val insulin = uiState.selectedInsulinType
                    if (insulin != null) {
                        val insulinDisplayName = if (!insulin.activeSubstance.isNullOrBlank()) {
                            "${insulin.name} (${insulin.activeSubstance})"
                        } else {
                            insulin.name
                        }
                        Text(
                            stringResource(
                                R.string.setup_wizard_summary_insulin_type_format,
                                insulinDisplayName,
                                insulin.dia.value.toInt(),
                                insulin.peak.value.toInt()
                            )
                        )
                    }

                    val profile = uiState.insulinProfile
                    if (profile != null) {
                        Text(
                            stringResource(
                                R.string.setup_wizard_summary_profile_format,
                                profile.name
                            )
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Text(
                        stringResource(
                            R.string.setup_wizard_summary_cgm_format,
                            cgmDisplayName ?: stringResource(R.string.setup_wizard_summary_not_connected)
                        )
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(
                            R.string.setup_wizard_summary_pump_format,
                            pumpDisplayName ?: stringResource(R.string.setup_wizard_summary_not_connected)
                        )
                    )
                }
            }
        }
    }
}

private val previewInsulinType = InsulinType(
    id = "1",
    name = "NovoRapid",
    activeSubstance = "Insulin aspart",
    dia = Minutes.ofHours(5),
    peak = Minutes(75)
)

private val previewInsulinProfile = InsulinProfile(
    id = 1L,
    name = "Standard",
    basalBlocks = listOf(Block(Minutes.ofHours(24), 1.0)),
    isfBlocks = listOf(Block(Minutes.ofHours(24), 40.0)),
    crBlocks = listOf(Block(Minutes.ofHours(24), 10.0)),
    insulinType = previewInsulinType,
    insulinConcentration = InsulinConcentration.U100,
    dia = Minutes.ofHours(5),
    peak = Minutes(75)
)

@Preview(showBackground = true, name = "Light Mode")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Dark Mode")
@Composable
private fun SummaryStepPreview() {
    AppPreview {
        SummaryStep(
            uiState = SetupWizardUiState(
                glucoseUnit = GlucoseUnit.MG_DL,
                carbsUnit = CarbsUnit.GRAMS,
                availableInsulinTypes = listOf(previewInsulinType),
                selectedInsulinTypeIds = setOf(previewInsulinType.id),
                primaryInsulinTypeId = previewInsulinType.id,
                insulinProfile = previewInsulinProfile
            ),
            cgmDisplayName = "SimBody Virtual Glucose Sensor",
            pumpDisplayName = "SimBody Virtual Insulin Pump",
            onComplete = {},
            onBack = {},
            snackbarHostState = remember { SnackbarHostState() },
            stepProgress = Pair(7, 7)
        )
    }
}