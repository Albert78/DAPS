package de.dh.daps.ui.screens.insulinprofile

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.dh.daps.common.model.InsulinType
import de.dh.daps.common.model.data.Block
import de.dh.daps.common.model.data.InsulinProfile
import de.dh.daps.common.model.data.Minutes
import de.dh.daps.ui.R
import de.dh.daps.ui.common.composables.NormalTextButton
import de.dh.daps.ui.common.composables.contentScrollIndicator
import de.dh.daps.ui.common.composables.screenTitle
import de.dh.daps.ui.common.theme.AppPreview
import de.dh.daps.common.R as CommonR

@Composable
fun InsulinProfilesScreen(
    viewModel: InsulinProfileSettingsViewModel,
    onNavigateUp: () -> Unit,
    title: String = stringResource(id = R.string.insulin_profiles_screen_title)
) {
    val uiState by viewModel.uiState.collectAsState()
    val copyNameFormat = stringResource(R.string.insulin_profiles_copy_name_format)

    if (uiState.editingProfile != null) {
        val editingProfile = uiState.editingProfile!!
        InsulinProfileDetailEditor(
            profile = editingProfile,
            insulinTypes = uiState.insulinTypes,
            onSave = { viewModel.saveInsulinProfile(it) },
            onCancel = { viewModel.stopEditing() },
            isNameUnique = { name, id -> viewModel.isNameUnique(name, id) }
        )
    } else {
        InsulinProfilesContent(
            uiState = uiState,
            title = title,
            onNavigateUp = onNavigateUp,
            onAddProfile = { viewModel.startEditingNewProfile() },
            onEditProfile = { viewModel.startEditing(it) },
            onDeleteProfile = { viewModel.confirmDelete(it) },
            onCopyProfile = { profile ->
                viewModel.copyInsulinProfile(profile, copyNameFormat.format(profile.name))
            },
            onConfirmDeleteProfile = { profile -> viewModel.deleteInsulinProfile(profile) },
            onCancelDeleteDialog = { viewModel.cancelDelete() }
        )
    }
}

@Composable
fun InsulinProfilesContent(
    uiState: InsulinProfileSettingsUiState,
    onNavigateUp: () -> Unit,
    onAddProfile: () -> Unit,
    onEditProfile: (InsulinProfile) -> Unit,
    onDeleteProfile: (InsulinProfile) -> Unit,
    onCopyProfile: (InsulinProfile) -> Unit,
    onConfirmDeleteProfile: (InsulinProfile) -> Unit,
    onCancelDeleteDialog: () -> Unit,
    title: String = stringResource(id = R.string.insulin_profiles_screen_title)
) {
    InsulinProfileList(
        uiState = uiState,
        title = title,
        onNavigateUp = onNavigateUp,
        onAddProfile = onAddProfile,
        onEditProfile = onEditProfile,
        onDeleteProfile = onDeleteProfile,
        onCopyProfile = onCopyProfile
    )

    uiState.showDeleteConfirmation?.let { profile ->
        AlertDialog(
            onDismissRequest = onCancelDeleteDialog,
            title = { Text(stringResource(id = R.string.delete_insulin_profile_title)) },
            text = { Text(stringResource(id = R.string.delete_insulin_profile_message, profile.name)) },
            confirmButton = {
                NormalTextButton(onClick = { onConfirmDeleteProfile(profile) }) {
                    Text(stringResource(id = android.R.string.ok))
                }
            },
            dismissButton = {
                NormalTextButton(onClick = onCancelDeleteDialog) {
                    Text(stringResource(id = android.R.string.cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsulinProfileList(
    uiState: InsulinProfileSettingsUiState,
    onNavigateUp: () -> Unit,
    onAddProfile: () -> Unit,
    onEditProfile: (InsulinProfile) -> Unit,
    onDeleteProfile: (InsulinProfile) -> Unit,
    onCopyProfile: (InsulinProfile) -> Unit,
    title: String = stringResource(id = R.string.insulin_profiles_screen_title)
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = screenTitle(title),
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = CommonR.string.cd_navigate_up)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddProfile) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(id = R.string.cd_add_insulin_profile)
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                val listState = rememberLazyListState()
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .contentScrollIndicator(listState)
                ) {
                    items(uiState.profiles.size) { index ->
                        val profile = uiState.profiles[index]
                        ListItem(
                            headlineContent = { Text(profile.name) },
                            modifier = Modifier
                                .padding(8.dp)
                                .clickable { onEditProfile(profile) },
                            trailingContent = {
                                Row {
                                    IconButton(onClick = { onCopyProfile(profile) }) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = stringResource(id = R.string.cd_copy_insulin_profile)
                                        )
                                    }
                                    IconButton(onClick = { onDeleteProfile(profile) }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = stringResource(id = R.string.cd_delete_insulin_profile)
                                        )
                                    }
                                }
                            },
                            tonalElevation = 2.dp
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Light Mode")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Dark Mode")
@Composable
private fun InsulinProfilesPreview() {
    val sampleInsulinType = InsulinType(name = "Humalog", dia = Minutes.ofHours(5), peak = Minutes.ofHours(1))
    val sampleProfiles = listOf(
        InsulinProfile(
            id = 1,
            name = "Normal",
            basalBlocks = listOf(Block(Minutes.ofHours(24), 0.8)),
            isfBlocks = listOf(Block(Minutes.ofHours(24), 50.0)),
            crBlocks = listOf(Block(Minutes.ofHours(24), 10.0)),
            insulinType = sampleInsulinType,
            dia = sampleInsulinType.dia,
            peak = sampleInsulinType.peak
        ),
        InsulinProfile(
            id = 2,
            name = "Sport",
            basalBlocks = listOf(Block(Minutes.ofHours(24), 0.5)),
            isfBlocks = listOf(Block(Minutes.ofHours(24), 80.0)),
            crBlocks = listOf(Block(Minutes.ofHours(24), 15.0)),
            insulinType = sampleInsulinType,
            dia = sampleInsulinType.dia,
            peak = sampleInsulinType.peak
        )
    )

    AppPreview {
        InsulinProfilesContent(
            uiState = InsulinProfileSettingsUiState(profiles = sampleProfiles),
            onNavigateUp = {},
            onAddProfile = {},
            onEditProfile = {},
            onDeleteProfile = {},
            onCopyProfile = {},
            onConfirmDeleteProfile = {},
            onCancelDeleteDialog = {}
        )
    }
}