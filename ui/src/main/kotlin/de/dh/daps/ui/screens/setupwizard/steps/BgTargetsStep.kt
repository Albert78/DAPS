package de.dh.daps.ui.screens.setupwizard.steps

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import de.dh.daps.common.model.data.BgBlock
import de.dh.daps.ui.R
import de.dh.daps.ui.screens.setupwizard.components.SetupStepScaffold
import de.dh.daps.ui.screens.therapy.BgEditorFormContent
import de.dh.daps.ui.screens.therapy.BgEditorHelpDialog

@Composable
fun BgTargetsStep(
    bgBlocks: List<BgBlock>,
    onSetBgBlocks: (List<BgBlock>) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    stepProgress: Pair<Int, Int>? = null
) {
    var showHelpDialog by remember { mutableStateOf(false) }

    SetupStepScaffold(
        title = stringResource(R.string.setup_wizard_bg_targets_title),
        stepProgress = stepProgress,
        showTopBackButton = false,
        showBottomBackButton = true,
        onBack = onBack,
        onNext = onNext,
        snackbarHostState = snackbarHostState
    ) { innerPadding ->
        BgEditorFormContent(
            blocks = bgBlocks,
            onBlocksChanged = onSetBgBlocks,
            onHelpClick = { showHelpDialog = true },
            modifier = Modifier.padding(innerPadding)
        )
    }

    if (showHelpDialog) {
        BgEditorHelpDialog(onDismiss = { showHelpDialog = false })
    }
}