package de.dh.pump.danai.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Router
import androidx.compose.material3.Badge
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.dh.daps.ui.common.composables.FramedCard
import de.dh.daps.ui.common.composables.NormalButton
import de.dh.daps.ui.common.composables.PrimaryButton
import de.dh.daps.ui.common.theme.AppPreview
import de.dh.pump.dana.commands.options.DanaRsUserOptions
import de.dh.pump.danai.R

/**
 * View for an already bound Dana-i insulin pump with options, maintenance access, and unbind capabilities.
 */
@Composable
fun DanaIBoundContent(
    displayName: String,
    deviceId: String?,
    userOptions: DanaRsUserOptions?,
    isLoadingOptions: Boolean,
    isSavingOptions: Boolean,
    optionsError: String?,
    onLoadOptions: () -> Unit,
    onStartMaintenance: () -> Unit,
    onSaveAndUnbind: (DanaRsUserOptions?) -> Unit,
    onUnbindOnly: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showUnbindDialog by remember { mutableStateOf(false) }

    LaunchedEffect(showUnbindDialog) {
        if (showUnbindDialog) {
            onLoadOptions()
        }
    }

    FramedCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Router,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (!deviceId.isNullOrEmpty()) {
                            Text(
                                text = deviceId,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Badge(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Text(
                        text = stringResource(R.string.danai_bound_badge),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            HorizontalDivider()

            Text(
                text = stringResource(R.string.danai_bound_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider()

            PrimaryButton(
                onClick = onStartMaintenance,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.danai_bound_btn_start_maintenance))
            }

            NormalButton(
                onClick = { showUnbindDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f),
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
            ) {
                Text(stringResource(R.string.danai_bound_btn_unbind))
            }
        }
    }

    if (showUnbindDialog) {
        DanaIPumpUnbindDialog(
            userOptions = userOptions,
            isLoading = isLoadingOptions,
            isSaving = isSavingOptions,
            error = optionsError,
            onSaveAndUnbind = { options ->
                showUnbindDialog = false
                onSaveAndUnbind(options)
            },
            onUnbindOnly = {
                showUnbindDialog = false
                onUnbindOnly()
            },
            onDismiss = { showUnbindDialog = false }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DanaIBoundContentPreview() {
    AppPreview {
        DanaIBoundContent(
            displayName = "Dana-i (DANA-1234)",
            deviceId = "00:11:22:33:44:55",
            userOptions = null,
            isLoadingOptions = false,
            isSavingOptions = false,
            optionsError = null,
            onLoadOptions = {},
            onStartMaintenance = {},
            onSaveAndUnbind = {},
            onUnbindOnly = {}
        )
    }
}