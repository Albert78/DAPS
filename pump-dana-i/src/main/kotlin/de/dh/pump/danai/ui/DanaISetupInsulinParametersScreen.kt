package de.dh.pump.danai.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.dh.daps.common.domain.model.therapy.InsulinConcentration
import de.dh.daps.common.domain.model.therapy.InsulinType
import de.dh.daps.common.domain.model.therapy.getDefaultInsulinTypes
import de.dh.daps.ui.common.composables.InsulinConcentrationCard
import de.dh.daps.ui.common.composables.InsulinTypeCard
import de.dh.daps.ui.common.composables.NormalTextButton
import de.dh.daps.ui.common.composables.PrimaryButton
import de.dh.daps.ui.common.theme.AppPreview
import de.dh.pump.danai.DanaIInsulinPumpDriver
import de.dh.pump.danai.R

/**
 * Stateful screen container for selecting insulin parameters during Dana-i pump setup.
 */
@Composable
fun DanaISetupInsulinParametersScreen(
    driver: DanaIInsulinPumpDriver,
    modifier: Modifier = Modifier,
    viewModel: DanaISetupInsulinParametersViewModel = viewModel(
        factory = DanaISetupInsulinParametersViewModel.Companion.Factory(driver.preferences)
    )
) {
    val context = LocalContext.current
    val availableInsulinTypes = remember(context) { getDefaultInsulinTypes(context) }
    val selectedTypeId by viewModel.selectedTypeId.collectAsStateWithLifecycle()
    val selectedConcentration by viewModel.selectedConcentration.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()

    LaunchedEffect(availableInsulinTypes) {
        if (selectedTypeId == null && availableInsulinTypes.isNotEmpty()) {
            viewModel.selectInsulinType(availableInsulinTypes.first().id)
        }
    }

    DanaISetupInsulinParametersContent(
        selectedTypeId = selectedTypeId,
        availableTypes = availableInsulinTypes,
        selectedConcentration = selectedConcentration,
        isSaving = isSaving,
        onSelectType = { viewModel.selectInsulinType(it.id) },
        onSelectConcentration = { viewModel.selectInsulinConcentration(it) },
        onCompleteSetup = { viewModel.completeSetup(driver) },
        onCancelSetup = { viewModel.cancelSetup(driver) },
        modifier = modifier
    )
}

/**
 * Stateless UI content for selecting insulin type and concentration during setup.
 */
@Composable
fun DanaISetupInsulinParametersContent(
    selectedTypeId: String?,
    availableTypes: List<InsulinType>,
    selectedConcentration: InsulinConcentration,
    isSaving: Boolean,
    onSelectType: (InsulinType) -> Unit,
    onSelectConcentration: (InsulinConcentration) -> Unit,
    onCompleteSetup: () -> Unit,
    onCancelSetup: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = stringResource(R.string.danai_setup_insulin_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.danai_setup_insulin_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        InsulinTypeCard(
            selectedTypeId = selectedTypeId,
            availableTypes = availableTypes,
            onSelectType = onSelectType
        )

        InsulinConcentrationCard(
            selectedConcentration = selectedConcentration,
            onSelectConcentration = onSelectConcentration
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PrimaryButton(
                onClick = onCompleteSetup,
                enabled = !isSaving && selectedTypeId != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(text = stringResource(R.string.danai_setup_insulin_btn_finish))
            }

            NormalTextButton(
                onClick = onCancelSetup,
                enabled = !isSaving
            ) {
                Text(text = stringResource(R.string.danai_setup_insulin_btn_cancel))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DanaISetupInsulinParametersContentPreview() {
    val context = LocalContext.current
    val sampleTypes = remember(context) { getDefaultInsulinTypes(context) }

    AppPreview {
        DanaISetupInsulinParametersContent(
            selectedTypeId = sampleTypes.firstOrNull()?.id,
            availableTypes = sampleTypes,
            selectedConcentration = InsulinConcentration.U100,
            isSaving = false,
            onSelectType = {},
            onSelectConcentration = {},
            onCompleteSetup = {},
            onCancelSetup = {}
        )
    }
}