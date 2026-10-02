package de.dh.daps.ui.screens.appdata

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import de.dh.daps.ui.R
import de.dh.daps.ui.common.composables.screenTitle
import de.dh.daps.ui.common.icons.Backup
import de.dh.daps.ui.common.icons.Icon_Screen_Back
import de.dh.daps.ui.common.icons.Restore
import de.dh.daps.ui.common.theme.AppPreview
import de.dh.daps.ui.common.theme.ExtendedTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import de.dh.daps.common.R as CommonR

@Composable
fun AppDataScreen(
    viewModel: AppDataViewModel,
    onNavigateUp: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // SAF Launchers
    val openDocumentTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            viewModel.setBackupDirectoryUri(uri.toString())
        }
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.exportBackup(uri, context.contentResolver)
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.showImportConfirmation(uri)
        }
    }

    val msgResetSuccess = stringResource(R.string.app_data_msg_reset_success)
    val msgExportSuccess = stringResource(R.string.app_data_msg_export_success)
    val msgImportSuccess = stringResource(R.string.app_data_msg_import_success)
    val msgErrorFormat = stringResource(R.string.app_data_msg_error)

    // React to user messages
    LaunchedEffect(uiState.userMessage) {
        val msg = uiState.userMessage ?: return@LaunchedEffect
        val text = when (msg) {
            "RESET_SUCCESS" -> msgResetSuccess
            "EXPORT_SUCCESS" -> msgExportSuccess
            "IMPORT_SUCCESS" -> msgImportSuccess
            else -> String.format(msgErrorFormat, msg)
        }
        Toast.makeText(context, text, Toast.LENGTH_LONG).show()
        viewModel.clearUserMessage()
    }

    AppDataContent(
        uiState = uiState,
        onNavigateUp = onNavigateUp,
        onResetClick = { viewModel.showResetConfirmation(true) },
        onSelectFolderClick = { openDocumentTreeLauncher.launch(null) },
        onIncludeHistoryChange = { viewModel.setIncludeHistory(it) },
        onIncludeDiagnosticsChange = { viewModel.setIncludeDiagnostics(it) },
        onImportIncludeHistoryChange = { viewModel.setImportIncludeHistory(it) },
        onImportIncludeDiagnosticsChange = { viewModel.setImportIncludeDiagnostics(it) },
        onImportIncludeDescriptorsChange = { viewModel.setImportIncludeDescriptors(it) },
        onExportClick = {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val filename = "daps_backup_${dateFormat.format(Date())}.dapsbackup"
            createDocumentLauncher.launch(filename)
        },
        onImportClick = { openDocumentLauncher.launch(arrayOf("*/*")) },
        onResetConfirm = { viewModel.confirmReset() },
        onResetDismiss = { viewModel.showResetConfirmation(false) },
        onImportConfirm = { viewModel.confirmImport(context.contentResolver) },
        onImportDismiss = { viewModel.showImportConfirmation(null) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDataContent(
    uiState: AppDataUiState,
    onNavigateUp: () -> Unit,
    onResetClick: () -> Unit,
    onSelectFolderClick: () -> Unit,
    onIncludeHistoryChange: (Boolean) -> Unit,
    onIncludeDiagnosticsChange: (Boolean) -> Unit,
    onImportIncludeHistoryChange: (Boolean) -> Unit,
    onImportIncludeDiagnosticsChange: (Boolean) -> Unit,
    onImportIncludeDescriptorsChange: (Boolean) -> Unit,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    onResetConfirm: () -> Unit,
    onResetDismiss: () -> Unit,
    onImportConfirm: () -> Unit,
    onImportDismiss: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            LargeTopAppBar(
                title = screenTitle(stringResource(R.string.app_data_screen_title)),
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Sektion 1: Re-Initialisierung (Reset)
                Text(
                    text = stringResource(R.string.app_data_section_reset_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(2.dp, ExtendedTheme.semanticColors.border),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = stringResource(R.string.app_data_card_reset_title),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.app_data_card_reset_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = onResetClick,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(stringResource(R.string.app_data_btn_reset))
                        }
                    }
                }

                // Sektion 2: Speicherort / SAF Verzeichnis
                Text(
                    text = stringResource(R.string.app_data_section_storage_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(2.dp, ExtendedTheme.semanticColors.border),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = stringResource(R.string.app_data_card_storage_title),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.app_data_card_storage_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val currentUriDisplay = uiState.backupDirectoryUri
                            ?.let { it.toUri().path ?: it }
                            ?: stringResource(R.string.app_data_storage_no_folder)
                        Text(
                            text = currentUriDisplay,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onSelectFolderClick,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(stringResource(R.string.app_data_btn_select_folder))
                        }
                    }
                }

                // Sektion 3: Export / Backup
                Text(
                    text = stringResource(R.string.app_data_section_export_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(2.dp, ExtendedTheme.semanticColors.border),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Backup,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = stringResource(R.string.app_data_card_export_title),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.app_data_card_export_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = uiState.includeHistory,
                                onCheckedChange = onIncludeHistoryChange
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.app_data_option_include_history),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = uiState.includeDiagnostics,
                                onCheckedChange = onIncludeDiagnosticsChange
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.app_data_option_include_diagnostics),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onExportClick,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(stringResource(R.string.app_data_btn_export))
                        }
                    }
                }

                // Sektion 4: Import / Restore
                Text(
                    text = stringResource(R.string.app_data_section_import_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(2.dp, ExtendedTheme.semanticColors.border),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Restore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = stringResource(R.string.app_data_card_import_title),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.app_data_card_import_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = uiState.importIncludeHistory,
                                onCheckedChange = onImportIncludeHistoryChange
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.app_data_option_import_history),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = uiState.importIncludeDiagnostics,
                                onCheckedChange = onImportIncludeDiagnosticsChange
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.app_data_option_import_diagnostics),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = uiState.importIncludeDescriptors,
                                onCheckedChange = onImportIncludeDescriptorsChange
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.app_data_option_import_descriptors),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onImportClick,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(stringResource(R.string.app_data_btn_import))
                        }
                    }
                }
            }

            // Progress Overlay
            if (uiState.isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }

    // Confirm Reset Dialog
    if (uiState.showResetDialog) {
        ResetConfirmationDialog(
            onConfirm = onResetConfirm,
            onDismiss = onResetDismiss
        )
    }

    // Confirm Import Dialog
    if (uiState.showImportConfirmDialog) {
        AlertDialog(
            onDismissRequest = onImportDismiss,
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text(stringResource(R.string.app_data_dialog_import_title)) },
            text = { Text(stringResource(R.string.app_data_dialog_import_message)) },
            confirmButton = {
                Button(
                    onClick = onImportConfirm
                ) {
                    Text(stringResource(R.string.app_data_btn_import))
                }
            },
            dismissButton = {
                TextButton(onClick = onImportDismiss) {
                    Text(stringResource(CommonR.string.action_cancel))
                }
            }
        )
    }
}

@Preview(showBackground = true, heightDp = 1400)
@Preview(showBackground = true, heightDp = 1400, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AppDataContentPreview() {
    AppPreview {
        AppDataContent(
            uiState = AppDataUiState(
                backupDirectoryUri = "content://com.android.externalstorage.documents/tree/primary%3ADAPS_Backups",
                includeHistory = true,
                includeDiagnostics = false,
                isProcessing = false
            ),
            onNavigateUp = {},
            onResetClick = {},
            onSelectFolderClick = {},
            onIncludeHistoryChange = {},
            onIncludeDiagnosticsChange = {},
            onImportIncludeHistoryChange = {},
            onImportIncludeDiagnosticsChange = {},
            onImportIncludeDescriptorsChange = {},
            onExportClick = {},
            onImportClick = {},
            onResetConfirm = {},
            onResetDismiss = {},
            onImportConfirm = {},
            onImportDismiss = {}
        )
    }
}

@Composable
fun ResetConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        ResetConfirmationFullscreenDialogContent(
            onConfirm = onConfirm,
            onDismiss = onDismiss
        )
    }
}

@Composable
fun ResetConfirmationFullscreenDialogContent(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.error,
        contentColor = MaterialTheme.colorScheme.onError
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Big Warning Icon
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.size(96.dp)
                )

                // Title
                Text(
                    text = stringResource(R.string.app_data_dialog_reset_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onError,
                    textAlign = TextAlign.Center
                )

                // Warning Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.onError.copy(alpha = 0.15f),
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.onError)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onError
                            )
                            Text(
                                text = stringResource(R.string.app_data_card_reset_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onError
                            )
                        }

                        Text(
                            text = stringResource(R.string.app_data_dialog_reset_message),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onError,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onConfirm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onError,
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null
                        )
                        Text(
                            text = stringResource(R.string.app_data_btn_reset),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.onError),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(
                        text = stringResource(CommonR.string.action_cancel),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ResetConfirmationFullscreenDialogContentPreview() {
    AppPreview {
        ResetConfirmationFullscreenDialogContent(
            onConfirm = {},
            onDismiss = {}
        )
    }
}