package de.dh.daps.ui.screens.setupwizard.steps

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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.dh.daps.common.model.data.CarbsUnit
import de.dh.daps.common.model.data.GlucoseUnit
import de.dh.daps.ui.R
import de.dh.daps.ui.common.carbsUnitLabel
import de.dh.daps.ui.common.composables.contentScrollIndicator
import de.dh.daps.ui.common.glucoseUnitLabel
import de.dh.daps.ui.common.icons.Icon_Carbs_Unit
import de.dh.daps.ui.common.icons.Icon_Glucose_Unit
import de.dh.daps.ui.screens.setupwizard.components.SetupStepScaffold

@Composable
fun UnitsStep(
    glucoseUnit: GlucoseUnit,
    carbsUnit: CarbsUnit,
    onGlucoseUnitSelected: (GlucoseUnit) -> Unit,
    onCarbsUnitSelected: (CarbsUnit) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    SetupStepScaffold(
        title = stringResource(R.string.setup_wizard_step1_units_title),
        showBackButton = true,
        onBack = onBack,
        onNext = onNext,
        snackbarHostState = snackbarHostState
    ) {
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .contentScrollIndicator(scrollState)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.setup_wizard_step1_units_desc),
                style = MaterialTheme.typography.bodyMedium
            )

            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icon_Glucose_Unit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.pref_glucose_unit_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(8.dp))

                    Column(Modifier.selectableGroup()) {
                        GlucoseUnit.entries.forEach { unit ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .selectable(
                                        selected = (unit == glucoseUnit),
                                        onClick = { onGlucoseUnitSelected(unit) },
                                        role = Role.RadioButton
                                    )
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (unit == glucoseUnit),
                                    onClick = null
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = glucoseUnitLabel(unit),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            }

            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icon_Carbs_Unit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.pref_carbs_unit_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(8.dp))

                    Column(Modifier.selectableGroup()) {
                        CarbsUnit.entries.forEach { unit ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .selectable(
                                        selected = (unit == carbsUnit),
                                        onClick = { onCarbsUnitSelected(unit) },
                                        role = Role.RadioButton
                                    )
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (unit == carbsUnit),
                                    onClick = null
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = carbsUnitLabel(unit),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}