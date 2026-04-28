package tech.arnav.twofac.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import tech.arnav.twofac.companion.isSyncToCompanionEnabled
import tech.arnav.twofac.components.security.PasskeyDialog
import tech.arnav.twofac.components.settings.BackupExportModeDialog
import tech.arnav.twofac.components.settings.BackupProvidersCard
import tech.arnav.twofac.components.settings.CompanionSyncCard
import tech.arnav.twofac.components.settings.DeleteStorageDialog
import tech.arnav.twofac.components.settings.RememberPasskeyCard
import tech.arnav.twofac.components.settings.StorageLocationCard
import tech.arnav.twofac.storage.getStoragePath
import tech.arnav.twofac.viewmodels.SettingsBackupAction
import tech.arnav.twofac.viewmodels.SettingsUiState
import tech.arnav.twofac.viewmodels.SettingsUnlockMode
import tech.arnav.twofac.viewmodels.SettingsViewModel
import twofac.composeapp.generated.resources.Res
import twofac.composeapp.generated.resources.action_back
import twofac.composeapp.generated.resources.action_continue
import twofac.composeapp.generated.resources.action_save
import twofac.composeapp.generated.resources.action_unlock
import twofac.composeapp.generated.resources.backup_passkey_encrypted_export_description
import twofac.composeapp.generated.resources.backup_passkey_import_description
import twofac.composeapp.generated.resources.backup_passkey_import_title
import twofac.composeapp.generated.resources.backup_passkey_plaintext_export_description
import twofac.composeapp.generated.resources.backup_passkey_required_description
import twofac.composeapp.generated.resources.backup_passkey_required_title
import twofac.composeapp.generated.resources.backup_passkey_sync_description
import twofac.composeapp.generated.resources.backup_passkey_unlock_title
import twofac.composeapp.generated.resources.settings_biometric_description
import twofac.composeapp.generated.resources.settings_biometric_title
import twofac.composeapp.generated.resources.settings_onboarding_button
import twofac.composeapp.generated.resources.settings_onboarding_content_description
import twofac.composeapp.generated.resources.settings_remember_passkey_description
import twofac.composeapp.generated.resources.settings_remember_passkey_title
import twofac.composeapp.generated.resources.settings_secure_unlock_description
import twofac.composeapp.generated.resources.settings_secure_unlock_title
import twofac.composeapp.generated.resources.settings_title
import twofac.composeapp.generated.resources.settings_upcoming_code_description
import twofac.composeapp.generated.resources.settings_upcoming_code_title

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: (() -> Unit)? = null,
    onNavigateToOnboarding: (() -> Unit)? = null,
    onQuit: (() -> Unit)? = null,
    viewModel: SettingsViewModel = koinInject(),
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.snackbarMessages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.settings_title)) },
                windowInsets = WindowInsets(0, 0, 0, 0),
                navigationIcon = {
                    onNavigateBack?.let { navigateBack ->
                        IconButton(onClick = navigateBack) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(Res.string.action_back),
                            )
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        SettingsScreenContent(
            uiState = uiState,
            onNavigateToOnboarding = onNavigateToOnboarding,
            onQuit = onQuit,
            onDeleteStorageClick = viewModel::requestDeleteStorage,
            onRememberPasskeyChanged = viewModel::onRememberPasskeyToggleChanged,
            onShowUpcomingCodeChanged = viewModel::onShowUpcomingCodeChanged,
            onExportClick = { providerId -> viewModel.requestBackupExport(providerId) },
            onImportClick = viewModel::importBackup,
            onSyncClick = viewModel::syncCompanion,
            onDiscoverClick = viewModel::discoverCompanion,
            modifier = Modifier.padding(paddingValues),
        )
    }

    SettingsDialogs(
        uiState = uiState,
        onDeleteDialogDismiss = viewModel::dismissDeleteStorageDialog,
        onDeleteConfirm = viewModel::confirmDeleteStorage,
        onExportModeDismiss = viewModel::dismissBackupExportDialog,
        onPlaintextExportSelected = { viewModel.onBackupExportModeSelected(encrypted = false) },
        onEncryptedExportSelected = { viewModel.onBackupExportModeSelected(encrypted = true) },
        onPendingActionDismiss = viewModel::dismissPendingAction,
        onPendingActionPasskeySubmit = viewModel::submitPendingActionPasskey,
        onBackupRestoreDismiss = viewModel::dismissEncryptedImportRequest,
        onBackupRestorePasskeySubmit = viewModel::submitBackupRestorePasskey,
        onCurrentRestorePasskeySubmit = viewModel::submitCurrentRestorePasskey,
        onEnrollmentDismiss = viewModel::dismissEnrollmentDialog,
        onEnrollmentPasskeySubmit = viewModel::submitEnrollmentPasskey,
    )
}

@Composable
private fun SettingsScreenContent(
    uiState: SettingsUiState,
    onNavigateToOnboarding: (() -> Unit)?,
    onQuit: (() -> Unit)?,
    onDeleteStorageClick: () -> Unit,
    onRememberPasskeyChanged: (Boolean) -> Unit,
    onShowUpcomingCodeChanged: (Boolean) -> Unit,
    onExportClick: (String) -> Unit,
    onImportClick: (String) -> Unit,
    onSyncClick: () -> Unit,
    onDiscoverClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),
    ) {
        StorageLocationCard(
            storagePath = getStoragePath(),
            onDeleteClick = onDeleteStorageClick,
            isDeleteEnabled = uiState.canDeleteStorage &&
                !uiState.isDeleteStorageInProgress &&
                !uiState.isLoading,
        )

        if (uiState.isSessionManagerAvailable) {
            val (toggleTitle, toggleDescription) = rememberPasskeyCardCopy(uiState.unlockMode)
            RememberPasskeyCard(
                title = toggleTitle,
                description = toggleDescription,
                isEnabled = uiState.isSecureUnlockEnabled,
                onEnabledChanged = onRememberPasskeyChanged,
            )
        }

        RememberPasskeyCard(
            title = stringResource(Res.string.settings_upcoming_code_title),
            description = stringResource(Res.string.settings_upcoming_code_description),
            isEnabled = uiState.appPreferences.showUpcomingCode,
            onEnabledChanged = onShowUpcomingCodeChanged,
        )

        if (uiState.isBackupAvailable) {
            BackupProvidersCard(
                providers = uiState.backupProviders,
                isLoading = uiState.isLoading,
                onExportClick = { provider -> onExportClick(provider.id) },
                onImportClick = { provider -> onImportClick(provider.id) },
            )
        }

        if (uiState.isCompanionSyncAvailable) {
            CompanionSyncCard(
                companionDisplayName = uiState.companionDisplayName,
                isCompanionActive = uiState.isCompanionActive,
                isSyncEnabled = isSyncToCompanionEnabled(
                    isCompanionActive = uiState.isCompanionActive,
                    isSyncInProgress = uiState.isCompanionSyncInProgress,
                ),
                isDiscoveryEnabled = !uiState.isCompanionSyncInProgress &&
                    !uiState.isCompanionDiscoveryInProgress,
                onSyncClick = onSyncClick,
                onDiscoverClick = onDiscoverClick,
            )
        }

        PlatformSettingsContent(onQuit = onQuit)

        onNavigateToOnboarding?.let { navigateToOnboarding ->
            OutlinedButton(
                onClick = navigateToOnboarding,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Rounded.School,
                    contentDescription = stringResource(Res.string.settings_onboarding_content_description),
                    modifier = Modifier.padding(end = 8.dp),
                )
                Text(stringResource(Res.string.settings_onboarding_button))
            }
        }
    }
}

@Composable
private fun SettingsDialogs(
    uiState: SettingsUiState,
    onDeleteDialogDismiss: () -> Unit,
    onDeleteConfirm: () -> Unit,
    onExportModeDismiss: () -> Unit,
    onPlaintextExportSelected: () -> Unit,
    onEncryptedExportSelected: () -> Unit,
    onPendingActionDismiss: () -> Unit,
    onPendingActionPasskeySubmit: (String) -> Unit,
    onBackupRestoreDismiss: () -> Unit,
    onBackupRestorePasskeySubmit: (String) -> Unit,
    onCurrentRestorePasskeySubmit: (String) -> Unit,
    onEnrollmentDismiss: () -> Unit,
    onEnrollmentPasskeySubmit: (String) -> Unit,
) {
    if (uiState.showDeleteStorageDialog) {
        DeleteStorageDialog(
            onDismissRequest = onDeleteDialogDismiss,
            onConfirm = onDeleteConfirm,
            isDeleteInProgress = uiState.isDeleteStorageInProgress,
        )
    }

    if (uiState.exportProviderId != null) {
        BackupExportModeDialog(
            isVisible = true,
            onPlaintextSelected = onPlaintextExportSelected,
            onEncryptedSelected = onEncryptedExportSelected,
            onDismiss = onExportModeDismiss,
        )
    }

    PendingActionDialog(
        uiState = uiState,
        onDismiss = onPendingActionDismiss,
        onPasskeySubmit = onPendingActionPasskeySubmit,
    )
    BackupRestoreDialogs(
        uiState = uiState,
        onDismiss = onBackupRestoreDismiss,
        onBackupPasskeySubmit = onBackupRestorePasskeySubmit,
        onCurrentPasskeySubmit = onCurrentRestorePasskeySubmit,
    )

    if (uiState.showEnrollmentDialog) {
        PasskeyDialog(
            isVisible = true,
            isLoading = uiState.isLoading,
            error = uiState.enrollmentError,
            onPasskeySubmit = onEnrollmentPasskeySubmit,
            onDismiss = onEnrollmentDismiss,
        )
    }
}

@Composable
private fun PendingActionDialog(
    uiState: SettingsUiState,
    onDismiss: () -> Unit,
    onPasskeySubmit: (String) -> Unit,
) {
    val action = uiState.pendingAction ?: return
    val (dialogTitle, dialogDescription, confirmLabel) = when (action) {
        is SettingsBackupAction.Export -> Triple(
            stringResource(Res.string.backup_passkey_unlock_title),
            if (action.encrypted) {
                stringResource(Res.string.backup_passkey_encrypted_export_description)
            } else {
                stringResource(Res.string.backup_passkey_plaintext_export_description)
            },
            stringResource(Res.string.action_continue),
        )

        is SettingsBackupAction.Import -> Triple(
            stringResource(Res.string.backup_passkey_import_title),
            stringResource(Res.string.backup_passkey_import_description),
            stringResource(Res.string.action_save),
        )

        SettingsBackupAction.SyncCompanion -> Triple(
            stringResource(Res.string.backup_passkey_unlock_title),
            stringResource(Res.string.backup_passkey_sync_description),
            stringResource(Res.string.action_unlock),
        )
    }

    PasskeyDialog(
        isVisible = true,
        isLoading = uiState.isLoading,
        error = uiState.passkeyError,
        title = dialogTitle,
        description = dialogDescription,
        confirmLabel = confirmLabel,
        onPasskeySubmit = onPasskeySubmit,
        onDismiss = onDismiss,
    )
}

@Composable
private fun BackupRestoreDialogs(
    uiState: SettingsUiState,
    onDismiss: () -> Unit,
    onBackupPasskeySubmit: (String) -> Unit,
    onCurrentPasskeySubmit: (String) -> Unit,
) {
    val importRequest = uiState.encryptedImportRequest ?: return
    if (importRequest.backupPasskey == null) {
        PasskeyDialog(
            isVisible = true,
            isLoading = uiState.isLoading,
            error = uiState.backupRestorePasskeyError,
            title = stringResource(Res.string.backup_passkey_required_title),
            description = stringResource(Res.string.backup_passkey_required_description),
            confirmLabel = stringResource(Res.string.action_continue),
            onPasskeySubmit = onBackupPasskeySubmit,
            onDismiss = onDismiss,
        )
        return
    }

    PasskeyDialog(
        isVisible = true,
        isLoading = uiState.isLoading,
        error = uiState.currentRestorePasskeyError,
        title = stringResource(Res.string.backup_passkey_import_title),
        description = stringResource(Res.string.backup_passkey_import_description),
        confirmLabel = stringResource(Res.string.action_save),
        onPasskeySubmit = onCurrentPasskeySubmit,
        onDismiss = onDismiss,
    )
}

@Composable
private fun rememberPasskeyCardCopy(unlockMode: SettingsUnlockMode): Pair<String, String> {
    return when (unlockMode) {
        SettingsUnlockMode.BIOMETRIC -> {
            stringResource(Res.string.settings_biometric_title) to
                stringResource(Res.string.settings_biometric_description)
        }

        SettingsUnlockMode.SECURE_UNLOCK -> {
            stringResource(Res.string.settings_secure_unlock_title) to
                stringResource(Res.string.settings_secure_unlock_description)
        }

        SettingsUnlockMode.REMEMBER_PASSKEY -> {
            stringResource(Res.string.settings_remember_passkey_title) to
                stringResource(Res.string.settings_remember_passkey_description)
        }
    }
}

@Preview
@Composable
private fun SettingsScreenPreview() {
    SettingsScreen()
}
