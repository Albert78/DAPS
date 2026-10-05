package de.dh.daps.ui.screens.setupwizard.steps

import android.content.res.Configuration
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import de.dh.daps.common.model.InsulinConcentration
import de.dh.daps.common.model.InsulinType
import de.dh.daps.common.model.data.Block
import de.dh.daps.common.model.data.InsulinProfile
import de.dh.daps.common.model.data.Minutes
import de.dh.daps.ui.R
import de.dh.daps.ui.common.theme.AppPreview
import de.dh.daps.ui.screens.insulinprofile.InsulinProfileEditorContent
import de.dh.daps.ui.screens.setupwizard.components.SetupStepScaffold

@Composable
fun InsulinProfileStep(
    profile: InsulinProfile,
    availableInsulinTypes: List<InsulinType>,
    onSaveProfile: (InsulinProfile) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    stepProgress: Pair<Int, Int>? = null
) {
    var name by remember(profile) { mutableStateOf(profile.name) }
    var basalBlocks by remember(profile) { mutableStateOf(profile.basalBlocks) }
    var isfBlocks by remember(profile) { mutableStateOf(profile.isfBlocks) }
    var crBlocks by remember(profile) { mutableStateOf(profile.crBlocks) }
    var selectedInsulinType by remember(profile) { mutableStateOf(profile.insulinType) }
    var selectedConcentration by remember(profile) { mutableStateOf(profile.insulinConcentration) }
    var dia by remember(profile) { mutableStateOf(profile.dia.value.toString()) }
    var peak by remember(profile) { mutableStateOf(profile.peak.value.toString()) }
    var selectedTab by remember { mutableIntStateOf(0) }

    val isNameValid = name.trim().isNotBlank()
    val diaValue = dia.toIntOrNull() ?: 0
    val peakValue = peak.toIntOrNull() ?: 0
    val isValid = isNameValid && diaValue > 0 && peakValue > 0

    fun handleSaveAndNext() {
        if (!isValid) return
        val updatedProfile = profile.copy(
            name = name.trim(),
            basalBlocks = basalBlocks,
            isfBlocks = isfBlocks,
            crBlocks = crBlocks,
            insulinType = selectedInsulinType,
            insulinConcentration = selectedConcentration,
            dia = Minutes(diaValue.toShort()),
            peak = Minutes(peakValue.toShort())
        )
        onSaveProfile(updatedProfile)
        onNext()
    }

    SetupStepScaffold(
        title = stringResource(R.string.setup_wizard_profile_title),
        stepProgress = stepProgress,
        showTopBackButton = false,
        showBottomBackButton = true,
        onBack = onBack,
        onNext = ::handleSaveAndNext,
        isNextEnabled = isValid,
        snackbarHostState = snackbarHostState
    ) { innerPadding ->
        InsulinProfileEditorContent(
            name = name,
            onNameChange = { name = it },
            isNameValid = isNameValid,
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            insulinTypes = availableInsulinTypes,
            selectedInsulinType = selectedInsulinType,
            onInsulinTypeSelected = { type ->
                selectedInsulinType = type
                dia = type.dia.value.toString()
                peak = type.peak.value.toString()
                selectedConcentration = type.defaultConcentration
            },
            selectedConcentration = selectedConcentration,
            onConcentrationSelected = { selectedConcentration = it },
            dia = dia,
            onDiaChanged = { dia = it },
            peak = peak,
            onPeakChanged = { peak = it },
            basalBlocks = basalBlocks,
            onBasalBlocksChanged = { basalBlocks = it },
            isfBlocks = isfBlocks,
            onIsfBlocksChanged = { isfBlocks = it },
            crBlocks = crBlocks,
            onCrBlocksChanged = { crBlocks = it },
            modifier = Modifier.padding(innerPadding)
        )
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
private fun InsulinProfileStepPreview() {
    AppPreview {
        InsulinProfileStep(
            profile = previewInsulinProfile,
            availableInsulinTypes = listOf(previewInsulinType),
            onSaveProfile = {},
            onNext = {},
            onBack = {},
            snackbarHostState = remember { SnackbarHostState() },
            stepProgress = Pair(3, 7)
        )
    }
}