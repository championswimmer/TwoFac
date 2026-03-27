package tech.arnav.twofac.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.getString
import twofac.composeapp.generated.resources.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.koin.compose.getKoin
import tech.arnav.twofac.companion.CompanionSyncCoordinator
import tech.arnav.twofac.companion.isSyncToCompanionEnabled
import tech.arnav.twofac.components.security.PasskeyDialog
import tech.arnav.twofac.components.settings.BackupExportModeDialog
import tech.arnav.twofac.components.settings.BackupProvidersCard
import tech.arnav.twofac.components.settings.CompanionSyncCard
import tech.arnav.twofac.components.settings.DeleteStorageDialog
import tech.arnav.twofac.components.settings.RememberPasskeyCard
import tech.arnav.twofac.components.settings.StorageLocationCard
import tech.arnav.twofac.lib.TwoFacLib
import tech.arnav.twofac.lib.backup.EncryptedAccountEntry
import tech.arnav.twofac.lib.backup.BackupProvider
import tech.arnav.twofac.lib.backup.BackupResult
import tech.arnav.twofac.lib.backup.BackupService
import tech.arnav.twofac.session.BiometricSessionManager
import tech.arnav.twofac.session.SecureSessionManager
import tech.arnav.twofac.session.SessionManager
import tech.arnav.twofac.storage.getStoragePath
import tech.arnav.twofac.viewmodels.AccountsViewModel
import tech.arnav.twofac.viewmodels.OnboardingViewModel

private sealed interface BackupAction {
    data class Export(val providerId: String, val encrypted: Boolean) : BackupAction
    data class Import(val providerId: String, val backupId: String) : BackupAction
    data object SyncCompanion : BackupAction
}

private data class EncryptedImportRequest(
    val providerId: String,
    val backupId: String,
    val encryptedAccounts: List<EncryptedAccountEntry>,
    val backupPasskey: String? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: (() -> Unit)? = null,
    onNavigateToOnboarding: (() -> Unit)? = null,
    onQuit: (() -> Unit)? = null
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val koin = getKoin()
    val backupService = remember { koin.getOrNull<BackupService>() }
    val twoFacLib = remember { koin.getOrNull<TwoFacLib>() }
    val companionSyncCoordinator = remember { koin.getOrNull<CompanionSyncCoordinator>() }
    val sessionManager = remember { koin.getOrNull<SessionManager>() }
    val accountsViewModel = remember { koin.getOrNull<AccountsViewModel>() }
    val onboardingViewModel = remember { koin.getOrNull<OnboardingViewModel>() }

    var pendingAction by remember { mutableStateOf<BackupAction?>(null) }
    var passkeyError by remember { mutableStateOf<String?>(null) }
    var backupRestorePasskeyError by remember { mutableStateOf<String?>(null) }
    var currentRestorePasskeyError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var exportProviderId by remember { mutableStateOf<String?>(null) }
    var encryptedImportRequest by remember { mutableStateOf<EncryptedImportRequest?>(null) }
    var showDeleteStorageDialog by remember { mutableStateOf(false) }
    var isDeleteStorageInProgress by remember { mutableStateOf(false) }
    var isCompanionSyncInProgress by remember { mutableStateOf(false) }
    var isCompanionDiscoveryInProgress by remember { mutableStateOf(false) }
    val isCompanionActive by (companionSyncCoordinator?.companionActiveFlow
        ?: remember { MutableStateFlow(false) }).collectAsState()
    var companionDisplayName by remember {
        mutableStateOf(companionSyncCoordinator?.companionDisplayName ?: "")
    }
    val secureSessionManager = sessionManager as? SecureSessionManager
    val biometricSessionManager = sessionManager as? BiometricSessionManager
    // Determine if secure storage is available — if so, we show one unified toggle
    val hasSecureStorage = secureSessionManager != null
    val isSecureUnlockEnabled = remember(sessionManager) {
        mutableStateOf(
            if (hasSecureStorage) {
                secureSessionManager!!.isSecureUnlockEnabled()
            } else {
                sessionManager?.isRememberPasskeyEnabled() ?: false
            }
        )
    }
    // Choose title/description based on platform capabilities
    val toggleTitle = when {
        biometricSessionManager != null -> stringResource(Res.string.settings_biometric_title)
        secureSessionManager != null -> stringResource(Res.string.settings_secure_unlock_title)
        else -> stringResource(Res.string.settings_remember_passkey_title)
    }
    val toggleDescription = when {
        biometricSessionManager != null -> stringResource(Res.string.settings_biometric_description)
        secureSessionManager != null -> stringResource(Res.string.settings_secure_unlock_description)
        else -> stringResource(Res.string.settings_remember_passkey_description)
    }
    var showEnrollmentDialog by remember { mutableStateOf(false) }
    var enrollmentError by remember { mutableStateOf<String?>(null) }
    var backupProviders by remember { mutableStateOf<List<BackupProvider>>(emptyList()) }

    // Pre-resolve simple localizable strings for use in coroutine lambdas
    val msgBackupUnavailable = stringResource(Res.string.backup_unavailable_message)
    val msgNoFilesFound = stringResource(Res.string.backup_no_files_found)
    val msgCompanionSyncUnavailable = stringResource(Res.string.settings_companion_sync_unavailable)
    val msgDeletionUnavailable = stringResource(Res.string.settings_account_deletion_unavailable)
    val msgDeleteFailed = stringResource(Res.string.settings_account_delete_failed)
    val msgAllDeleted = stringResource(Res.string.settings_account_all_deleted)
    val msgOperationFailed = stringResource(Res.string.error_operation_failed)
    val msgSecureUnavailable = stringResource(Res.string.settings_secure_unlock_unavailable)
    val msgSecureEnabled = stringResource(Res.string.settings_secure_unlock_enabled)
    val msgSecureCancelled = stringResource(Res.string.settings_secure_enrollment_cancelled)
    val msgVerifyFailed = stringResource(Res.string.settings_verify_passkey_failed)
    val msgBiometricEnabled = stringResource(Res.string.settings_biometric_unlock_enabled)
    val msgBiometricCancelled = stringResource(Res.string.settings_biometric_enrollment_cancelled)
    val msgRestoreUnavailable = stringResource(Res.string.backup_restore_unavailable)
    val msgIncorrectPasskey = stringResource(Res.string.backup_passkey_incorrect)
    val msgErrorUnknown = stringResource(Res.string.error_unknown)

    // Pre-resolve backup passkey dialog strings
    val msgUnlockTitle = stringResource(Res.string.backup_passkey_unlock_title)
    val msgEncryptedExportDesc = stringResource(Res.string.backup_passkey_encrypted_export_description)
    val msgPlaintextExportDesc = stringResource(Res.string.backup_passkey_plaintext_export_description)
    val msgSaveToDeviceTitle = stringResource(Res.string.backup_passkey_import_title)
    val msgSaveToDeviceDesc = stringResource(Res.string.backup_passkey_import_description)
    val msgSyncDesc = stringResource(Res.string.backup_passkey_sync_description)
    val msgContinue = stringResource(Res.string.action_continue)
    val msgSave = stringResource(Res.string.action_save)
    val msgUnlock = stringResource(Res.string.action_unlock)
    val msgBackupPasskeyTitle = stringResource(Res.string.backup_passkey_required_title)
    val msgBackupPasskeyDesc = stringResource(Res.string.backup_passkey_required_description)

    LaunchedEffect(backupService) {
        backupProviders = backupService?.listProviders().orEmpty()
    }

    val defaultCompanionName = stringResource(Res.string.settings_companion_default_name)
    LaunchedEffect(companionSyncCoordinator) {
        companionDisplayName = companionSyncCoordinator?.companionDisplayName ?: defaultCompanionName
        companionSyncCoordinator?.isCompanionActive()
    }

    suspend fun executeBackupExport(providerId: String, encrypted: Boolean) {
        val service = backupService
        if (service == null) {
            snackbarHostState.showSnackbar(msgBackupUnavailable)
            return
        }
        val result = service.createBackup(providerId, encrypted = encrypted)
        val message = when (result) {
            is BackupResult.Success ->
                if (encrypted)
                    getString(Res.string.backup_export_encrypted_success, result.value.id)
                else
                    getString(Res.string.backup_export_plaintext_success, result.value.id)
            is BackupResult.Failure ->
                getString(Res.string.backup_export_failed, result.message)
        }
        snackbarHostState.showSnackbar(message)
        backupProviders = service.listProviders()
    }

    suspend fun executeBackupImport(
        providerId: String,
        backupId: String,
        backupPasskey: String? = null,
        currentPasskey: String? = null,
    ) {
        val service = backupService
        if (service == null) {
            snackbarHostState.showSnackbar(msgBackupUnavailable)
            return
        }
        val result = service.restoreBackup(
            providerId = providerId,
            backupId = backupId,
            backupPasskey = backupPasskey,
            currentPasskey = currentPasskey,
        )
        val message = when (result) {
            is BackupResult.Success ->
                getString(Res.string.backup_import_success, result.value.toString(), backupId)
            is BackupResult.Failure ->
                getString(Res.string.backup_import_failed, result.message)
        }
        snackbarHostState.showSnackbar(message)
        if (result is BackupResult.Success) {
            companionSyncCoordinator?.onAccountsChanged()
            accountsViewModel?.reloadAccounts()
        }
        backupProviders = service.listProviders()
    }

    suspend fun prepareBackupImport(providerId: String) {
        val service = backupService
        if (service == null) {
            snackbarHostState.showSnackbar(msgBackupUnavailable)
            return
        }
        val listResult = service.listBackups(providerId)
        if (listResult is BackupResult.Failure) {
            snackbarHostState.showSnackbar(getString(Res.string.backup_no_backups_found, listResult.message))
            return
        }
        val backups = (listResult as BackupResult.Success).value
        if (backups.isEmpty()) {
            snackbarHostState.showSnackbar(msgNoFilesFound)
            return
        }
        val latest = backups.maxBy { it.createdAt }
        val inspectionResult = service.inspectBackup(providerId, latest.id)
        val payload = when (inspectionResult) {
            is BackupResult.Success -> inspectionResult.value
            is BackupResult.Failure -> {
                snackbarHostState.showSnackbar(getString(Res.string.backup_import_failed, inspectionResult.message))
                return
            }
        }
        if (payload.encrypted) {
            encryptedImportRequest = EncryptedImportRequest(
                providerId = providerId,
                backupId = latest.id,
                encryptedAccounts = payload.encryptedAccounts,
            )
            backupRestorePasskeyError = null
            currentRestorePasskeyError = null
            return
        }
        if (twoFacLib != null && twoFacLib.isUnlocked()) {
            executeBackupImport(providerId = providerId, backupId = latest.id)
        } else {
            pendingAction = BackupAction.Import(providerId = providerId, backupId = latest.id)
            passkeyError = null
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
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(Res.string.action_back))
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top)
        ) {
            StorageLocationCard(
                storagePath = getStoragePath(),
                onDeleteClick = { showDeleteStorageDialog = true },
                isDeleteEnabled = twoFacLib != null && !isDeleteStorageInProgress && !isLoading,
            )

            if (sessionManager != null && sessionManager.isAvailable()) {
                RememberPasskeyCard(
                    title = toggleTitle,
                    description = toggleDescription,
                    isEnabled = isSecureUnlockEnabled.value,
                    onEnabledChanged = { enabled ->
                        if (!enabled) {
                            // Disable: clear everything
                            if (hasSecureStorage) {
                                secureSessionManager!!.setSecureUnlockEnabled(false)
                            }
                            sessionManager.setRememberPasskey(false)
                            isSecureUnlockEnabled.value = false
                            showEnrollmentDialog = false
                            enrollmentError = null
                        } else if (hasSecureStorage) {
                            // Enable on secure platform: show enrollment dialog
                            enrollmentError = null
                            showEnrollmentDialog = true
                        } else {
                            // Enable on plain platform (no secure storage)
                            sessionManager.setRememberPasskey(true)
                            isSecureUnlockEnabled.value = true
                        }
                    },
                )
            }

            if (backupService != null) {
                BackupProvidersCard(
                    providers = backupProviders,
                    isLoading = isLoading,
                    onExportClick = { provider ->
                        exportProviderId = provider.id
                    },
                    onImportClick = { provider ->
                        isLoading = true
                        coroutineScope.launch {
                            try {
                                prepareBackupImport(provider.id)
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar(
                                    getString(Res.string.backup_import_exception, e.message ?: msgErrorUnknown)
                                )
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                )
            }

            if (companionSyncCoordinator != null) {
                CompanionSyncCard(
                    companionDisplayName = companionDisplayName,
                    isCompanionActive = isCompanionActive,
                    isSyncEnabled = isSyncToCompanionEnabled(
                        isCompanionActive = isCompanionActive,
                        isSyncInProgress = isCompanionSyncInProgress,
                    ),
                    isDiscoveryEnabled = !isCompanionSyncInProgress && !isCompanionDiscoveryInProgress,
                    onSyncClick = {
                        if (twoFacLib != null && !twoFacLib.isUnlocked()) {
                            pendingAction = BackupAction.SyncCompanion
                        } else {
                            coroutineScope.launch {
                                try {
                                    isCompanionSyncInProgress = true
                                    if (twoFacLib != null && twoFacLib.getAllAccounts().isEmpty()) {
                                        snackbarHostState.showSnackbar(getString(Res.string.settings_companion_no_accounts, companionDisplayName))
                                        return@launch
                                    }
                                    val synced = companionSyncCoordinator.syncNow(manual = true)
                                    if (synced) {
                                        snackbarHostState.showSnackbar(getString(Res.string.settings_companion_sync_sent, companionDisplayName))
                                    } else {
                                        snackbarHostState.showSnackbar(getString(Res.string.settings_companion_sync_failed, companionDisplayName))
                                    }
                                    companionSyncCoordinator.isCompanionActive()
                                } finally {
                                    isCompanionSyncInProgress = false
                                }
                            }
                        }
                    },
                    onDiscoverClick = {
                        coroutineScope.launch {
                            try {
                                isCompanionDiscoveryInProgress = true
                                val discovered = companionSyncCoordinator.forceDiscoverCompanion()
                                val message = if (discovered) {
                                    getString(Res.string.settings_companion_discovered, companionDisplayName)
                                } else {
                                    getString(Res.string.settings_companion_not_discovered, companionDisplayName)
                                }
                                snackbarHostState.showSnackbar(message)
                            } finally {
                                isCompanionDiscoveryInProgress = false
                            }
                        }
                    },
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

    if (showDeleteStorageDialog) {
        DeleteStorageDialog(
            onDismissRequest = {
                if (!isDeleteStorageInProgress) {
                    showDeleteStorageDialog = false
                }
            },
            onConfirm = {
                if (twoFacLib == null) {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(msgDeletionUnavailable)
                    }
                    showDeleteStorageDialog = false
                } else {
                    coroutineScope.launch {
                        isDeleteStorageInProgress = true
                        try {
                            val deleted = twoFacLib.deleteAllAccountsFromStorage()
                            if (!deleted) {
                                snackbarHostState.showSnackbar(msgDeleteFailed)
                                return@launch
                            }
                            try {
                                companionSyncCoordinator?.onAccountsChanged()
                                snackbarHostState.showSnackbar(msgAllDeleted)
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar(
                                    getString(Res.string.settings_account_deleted_sync_error, e.message ?: msgErrorUnknown)
                                )
                            }
                            showDeleteStorageDialog = false
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar(
                                getString(Res.string.settings_account_delete_exception, e.message ?: msgErrorUnknown)
                            )
                        } finally {
                            isDeleteStorageInProgress = false
                        }
                    }
                }
            },
            isDeleteInProgress = isDeleteStorageInProgress,
        )
    }

    if (exportProviderId != null) {
        BackupExportModeDialog(
            isVisible = true,
            onPlaintextSelected = {
                val providerId = exportProviderId ?: return@BackupExportModeDialog
                exportProviderId = null
                if (twoFacLib != null && twoFacLib.isUnlocked()) {
                    isLoading = true
                    coroutineScope.launch {
                        try {
                            executeBackupExport(providerId, encrypted = false)
                        } finally {
                            isLoading = false
                        }
                    }
                } else {
                    pendingAction = BackupAction.Export(providerId, encrypted = false)
                    passkeyError = null
                }
            },
            onEncryptedSelected = {
                val providerId = exportProviderId ?: return@BackupExportModeDialog
                exportProviderId = null
                if (twoFacLib != null && twoFacLib.isUnlocked()) {
                    isLoading = true
                    coroutineScope.launch {
                        try {
                            executeBackupExport(providerId, encrypted = true)
                        } finally {
                            isLoading = false
                        }
                    }
                } else {
                    pendingAction = BackupAction.Export(providerId, encrypted = true)
                    passkeyError = null
                }
            },
            onDismiss = {
                exportProviderId = null
            },
        )
    }

    // Passkey dialog for backup operations
    pendingAction?.let { action ->
        val (dialogTitle, dialogDescription, confirmLabel) = when (action) {
            is BackupAction.Export -> Triple(
                msgUnlockTitle,
                if (action.encrypted) msgEncryptedExportDesc else msgPlaintextExportDesc,
                msgContinue,
            )
            is BackupAction.Import -> Triple(
                msgSaveToDeviceTitle,
                msgSaveToDeviceDesc,
                msgSave,
            )
            BackupAction.SyncCompanion -> Triple(
                msgUnlockTitle,
                msgSyncDesc,
                msgUnlock,
            )
        }
        PasskeyDialog(
            isVisible = true,
            isLoading = isLoading,
            error = passkeyError,
            title = dialogTitle,
            description = dialogDescription,
            confirmLabel = confirmLabel,
            onPasskeySubmit = { passkey ->
                val selectedAction = pendingAction ?: return@PasskeyDialog
                passkeyError = null
                isLoading = true
                coroutineScope.launch {
                    try {
                        twoFacLib?.unlock(passkey)
                        when (selectedAction) {
                            is BackupAction.Export -> {
                                executeBackupExport(selectedAction.providerId, selectedAction.encrypted)
                                pendingAction = null
                            }
                            is BackupAction.Import -> {
                                executeBackupImport(
                                    providerId = selectedAction.providerId,
                                    backupId = selectedAction.backupId,
                                )
                                pendingAction = null
                            }

                            BackupAction.SyncCompanion -> {
                                if (companionSyncCoordinator == null || twoFacLib == null) {
                                    snackbarHostState.showSnackbar(msgCompanionSyncUnavailable)
                                    pendingAction = null
                                    return@launch
                                }
                                val hasAccounts = twoFacLib.getAllAccounts().isNotEmpty()
                                if (!hasAccounts) {
                                    snackbarHostState.showSnackbar(getString(Res.string.settings_companion_no_accounts, companionDisplayName))
                                    pendingAction = null
                                    return@launch
                                }
                                try {
                                    isCompanionSyncInProgress = true
                                    val synced = companionSyncCoordinator.syncNow(manual = true)
                                    val message = if (synced) {
                                        getString(Res.string.settings_companion_sync_sent, companionDisplayName)
                                    } else {
                                        getString(Res.string.settings_companion_sync_failed, companionDisplayName)
                                    }
                                    snackbarHostState.showSnackbar(message)
                                    companionSyncCoordinator.isCompanionActive()
                                } finally {
                                    isCompanionSyncInProgress = false
                                }
                                pendingAction = null
                            }
                        }
                    } catch (e: Exception) {
                        passkeyError = e.message ?: msgOperationFailed
                    } finally {
                        isLoading = false
                    }
                }
            },
            onDismiss = {
                pendingAction = null
                passkeyError = null
            }
        )
    }

    val importRequest = encryptedImportRequest
    if (importRequest != null && importRequest.backupPasskey == null) {
        PasskeyDialog(
            isVisible = true,
            isLoading = isLoading,
            error = backupRestorePasskeyError,
            title = msgBackupPasskeyTitle,
            description = msgBackupPasskeyDesc,
            confirmLabel = msgContinue,
            onPasskeySubmit = { passkey ->
                backupRestorePasskeyError = null
                isLoading = true
                coroutineScope.launch {
                    try {
                        if (twoFacLib == null) {
                            backupRestorePasskeyError = msgRestoreUnavailable
                            return@launch
                        }
                        importRequest.encryptedAccounts.forEach { account ->
                            twoFacLib.decryptEncryptedBackupAccount(account, passkey)
                        }
                        encryptedImportRequest = importRequest.copy(backupPasskey = passkey)
                    } catch (_: Exception) {
                        backupRestorePasskeyError = msgIncorrectPasskey
                    } finally {
                        isLoading = false
                    }
                }
            },
            onDismiss = {
                encryptedImportRequest = null
                backupRestorePasskeyError = null
                currentRestorePasskeyError = null
            },
        )
    }

    if (importRequest != null && importRequest.backupPasskey != null) {
        PasskeyDialog(
            isVisible = true,
            isLoading = isLoading,
            error = currentRestorePasskeyError,
            title = msgSaveToDeviceTitle,
            description = msgSaveToDeviceDesc,
            confirmLabel = msgSave,
            onPasskeySubmit = { currentPasskey ->
                currentRestorePasskeyError = null
                isLoading = true
                coroutineScope.launch {
                    try {
                        val service = backupService
                        if (service == null) {
                            currentRestorePasskeyError = msgRestoreUnavailable
                            return@launch
                        }
                        val result = service.restoreBackup(
                            providerId = importRequest.providerId,
                            backupId = importRequest.backupId,
                            backupPasskey = importRequest.backupPasskey,
                            currentPasskey = currentPasskey,
                        )
                        when (result) {
                            is BackupResult.Success -> {
                                snackbarHostState.showSnackbar(
                                    getString(Res.string.backup_import_success, result.value.toString(), importRequest.backupId)
                                )
                                companionSyncCoordinator?.onAccountsChanged()
                                accountsViewModel?.reloadAccounts()
                                backupProviders = service.listProviders()
                                encryptedImportRequest = null
                            }

                            is BackupResult.Failure -> {
                                currentRestorePasskeyError = result.message
                            }
                        }
                    } catch (e: Exception) {
                        currentRestorePasskeyError = e.message ?: msgVerifyFailed
                    } finally {
                        isLoading = false
                    }
                }
            },
            onDismiss = {
                encryptedImportRequest = null
                backupRestorePasskeyError = null
                currentRestorePasskeyError = null
            },
        )
    }

    // Unified enrollment dialog for secure/biometric unlock
    if (showEnrollmentDialog && secureSessionManager != null) {
        val successMsg = if (biometricSessionManager != null) msgBiometricEnabled else msgSecureEnabled
        val cancelledMsg = if (biometricSessionManager != null) msgBiometricCancelled else msgSecureCancelled
        PasskeyDialog(
            isVisible = true,
            isLoading = isLoading,
            error = enrollmentError,
            onPasskeySubmit = { passkey ->
                enrollmentError = null
                isLoading = true
                coroutineScope.launch {
                    try {
                        if (twoFacLib == null) {
                            enrollmentError = msgSecureUnavailable
                            return@launch
                        }
                        twoFacLib.unlock(passkey)
                        // Enroll first, then set enabled — avoids a window where
                        // isSecureUnlockEnabled is true but no passkey is stored.
                        val enrolled = secureSessionManager.enrollPasskey(passkey)
                        if (enrolled) {
                            secureSessionManager.setSecureUnlockEnabled(true)
                            sessionManager.setRememberPasskey(true)
                            isSecureUnlockEnabled.value = true
                            showEnrollmentDialog = false
                            onboardingViewModel?.refreshAndSyncDerivedCompletion()
                            snackbarHostState.showSnackbar(successMsg)
                        } else {
                            enrollmentError = cancelledMsg
                        }
                    } catch (e: Exception) {
                        enrollmentError = e.message ?: msgVerifyFailed
                    } finally {
                        isLoading = false
                    }
                }
            },
            onDismiss = {
                showEnrollmentDialog = false
                enrollmentError = null
            }
        )
    }
}

@Preview
@Composable
private fun SettingsScreenPreview() {
    SettingsScreen()
}
