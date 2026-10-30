package de.dh.daps.ui.screens.setupwizard.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.dh.daps.ui.R
import de.dh.daps.ui.common.composables.NormalButton
import de.dh.daps.ui.common.composables.PrimaryButton
import de.dh.daps.ui.common.composables.ScreenTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupStepScaffold(
    title: String,
    stepProgress: Pair<Int, Int>? = null,
    showTopBackButton: Boolean = false,
    useCloseIcon: Boolean = false,
    onTopBack: () -> Unit = {},
    showBottomBackButton: Boolean = true,
    onBack: () -> Unit = {},
    nextButtonText: String? = stringResource(R.string.setup_wizard_btn_next),
    onNext: (() -> Unit)? = null,
    isNextEnabled: Boolean = true,
    snackbarHostState: SnackbarHostState? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        snackbarHost = {
            if (snackbarHostState != null) {
                SnackbarHost(snackbarHostState)
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        ScreenTitle(text = title)
                        if (stepProgress != null) {
                            Text(
                                text = stringResource(
                                    R.string.setup_wizard_step_progress_format,
                                    stepProgress.first,
                                    stepProgress.second
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (showTopBackButton) {
                        IconButton(onClick = onTopBack) {
                            Icon(
                                imageVector = if (useCloseIcon) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(
                                    if (useCloseIcon) de.dh.daps.common.R.string.cd_cancel else R.string.setup_wizard_btn_back
                                )
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (showBottomBackButton || onNext != null) {
                Surface(
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .imePadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (showBottomBackButton) {
                            NormalButton(onClick = onBack) {
                                Text(stringResource(R.string.setup_wizard_btn_back))
                            }
                        } else {
                            Spacer(Modifier.width(1.dp))
                        }

                        if (onNext != null && nextButtonText != null) {
                            PrimaryButton(
                                onClick = onNext,
                                enabled = isNextEnabled
                            ) {
                                Text(nextButtonText)
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            content(innerPadding)
        }
    }
}

@Preview(showBackground = true, name = "Default Mode", heightDp = 200)
@Preview(showBackground = true, name = "Default Mode - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, heightDp = 200)
@Composable
fun ScaffoldPreview() {
    SetupStepScaffold(
        title = "Setup Schritt 1",
        stepProgress = Pair(3, 5),
        showTopBackButton = true,
        nextButtonText = "Weiter",
        onNext = {},
        isNextEnabled = true
    ) { }
}