package tech.arnav.twofac.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import tech.arnav.twofac.companion.CompanionSyncCoordinator
import tech.arnav.twofac.lib.TwoFacLib
import tech.arnav.twofac.lib.backup.BackupProvider
import tech.arnav.twofac.lib.backup.BackupResult
import tech.arnav.twofac.lib.backup.BackupService
import tech.arnav.twofac.lib.backup.EncryptedAccountEntry
import tech.arnav.twofac.session.BiometricSessionManager
import tech.arnav.twofac.session.SecureSessionManager
import tech.arnav.twofac.session.SessionManager
import tech.arnav.twofac.settings.AppPreferences
import tech.arnav.twofac.settings.AppPreferencesRepository
import twofac.composeapp.generated.resources.Res
import twofac.composeapp.generated.resources.action_continue
import twofac.composeapp.generated.resources.action_save
import twofac.composeapp.generated.resources.action_unlock
import twofac.composeapp.generated.resources.backup_export_encrypted_success
import twofac.composeapp.generated.resources.backup_export_failed
import twofac.composeapp.generated.resources.backup_export_plaintext_success
import twofac.composeapp.generated.resources.backup_import_exception
import twofac.composeapp.generated.resources.backup_import_failed
import twofac.composeapp.generated.resources.backup_import_success
import twofac.composeapp.generated.resources.backup_no_backups_found
import twofac.composeapp.generated.resources.backup_no_files_found
import twofac.composeapp.generated.resources.backup_passkey_import_description
import twofac.composeapp.generated.resources.backup_passkey_import_title
import twofac.composeapp.generated.resources.backup_passkey_incorrect
import twofac.composeapp.generated.resources.backup_passkey_plaintext_export_description
import twofac.composeapp.generated.resources.backup_passkey_required_description
import twofac.composeapp.generated.resources.backup_passkey_required_title
import twofac.composeapp.generated.resources.backup_passkey_sync_description
import twofac.composeapp.generated.resources.backup_passkey_unlock_title
import twofac.composeapp.generated.resources.backup_passkey_encrypted_export_description
import twofac.composeapp.generated.resources.backup_restore_unavailable
import twofac.composeapp.generated.resources.error_operation_failed
import twofac.composeapp.generated.resources.error_unknown
import twofac.composeapp.generated.resources.settings_account_all_deleted
import twofac.composeapp.generated.resources.settings_account_delete_exception
import twofac.composeapp.generated.resources.settings_account_delete_failed
import twofac.composeapp.generated.resources.settings_account_deleted_sync_error
import twofac.composeapp.generated.resources.settings_account_deletion_unavailable
import twofac.composeapp.generated.resources.settings_biometric_enrollment_cancelled
import twofac.composeapp.generated.resources.settings_biometric_title
import twofac.composeapp.generated.resources.settings_biometric_description
import twofac.composeapp.generated.resources.settings_biometric_unlock_enabled
import twofac.composeapp.generated.resources.settings_companion_default_name
import twofac.composeapp.generated.resources.settings_companion_discovered
import twofac.composeapp.generated.resources.settings_companion_no_accounts
import twofac.composeapp.generated.resources.settings_companion_not_discovered
import twofac.composeapp.generated.resources.settings_companion_sync_failed
import twofac.composeapp.generated.resources.settings_companion_sync_sent
import twofac.composeapp.generated.resources.settings_companion_sync_unavailable
import twofac.composeapp.generated.resources.settings_remember_passkey_description
import twofac.composeapp.generated.resources.settings_remember_passkey_title
import twofac.composeapp.generated.resources.settings_secure_enrollment_cancelled
import twofac.composeapp.generated.resources.settings_secure_enrollment_unavailable
import twofac.composeapp.generated.resources.settings_secure_unlock_description
import twofac.composeapp.generated.resources.settings_secure_unlock_enabled
import twofac.composeapp.generated.resources.settings_secure_unlock_title
import twofac.composeapp.generated.resources.settings_verify_passkey_failed

enum class SettingsUnlockMode {
    BIOMETRIC,
    SECURE_UNLOCK,
    REMEMBER_PASSKEY,
}

sealed interface SettingsBackupAction {
    data class Export(val providerId: String, val encrypted: Boolean) : SettingsBackupAction
    data class Import(val providerId: String, val backupId: String) : SettingsBackupAction
    data object SyncCompanion : SettingsBackupAction
}

data class EncryptedImportRequest(
    val providerId: String,
    val backupId: String,
    val encryptedAccounts: List<EncryptedAccountEntry>,
    val backupPasskey: String? = null,
)

data class SettingsUiState(
    val isLoading: Boolean = false,
    val passkeyError: String? = null,
    val backupRestorePasskeyError: String? = null,
    val currentRestorePasskeyError: String? = null,
    val enrollmentError: String? = null,
    val pendingAction: SettingsBackupAction? = null,
    val exportProviderId: String? = null,
    val encryptedImportRequest: EncryptedImportRequest? = null,
    val showDeleteStorageDialog: Boolean = false,
    val showEnrollmentDialog: Boolean = false,
    val isDeleteStorageInProgress: Boolean = false,
    val isCompanionSyncInProgress: Boolean = false,
    val isCompanionDiscoveryInProgress: Boolean = false,
    val isCompanionActive: Boolean = false,
    val companionDisplayName: String = "",
    val appPreferences: AppPreferences = AppPreferences(),
    val backupProviders: List<BackupProvider> = emptyList(),
    val isSessionManagerAvailable: Boolean = false,
    val isBackupAvailable: Boolean = false,
    val isCompanionSyncAvailable: Boolean = false,
    val canDeleteStorage: Boolean = false,
    val hasSecureStorage: Boolean = false,
    val isSecureUnlockEnabled: Boolean = false,
    val unlockMode: SettingsUnlockMode = SettingsUnlockMode.REMEMBER_PASSKEY,
)

class SettingsViewModel(
    private val backupService: BackupService? = null,
    private val twoFacLib: TwoFacLib? = null,
    private val companionSyncCoordinator: CompanionSyncCoordinator? = null,
    private val sessionManager: SessionManager? = null,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val accountsViewModel: AccountsViewModel? = null,
    private val onboardingViewModel: OnboardingViewModel? = null,
) : ViewModel() {
    private val secureSessionManager = sessionManager as? SecureSessionManager
    private val biometricSessionManager = sessionManager as? BiometricSessionManager

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            companionDisplayName = companionSyncCoordinator?.companionDisplayName.orEmpty(),
            isSessionManagerAvailable = sessionManager?.isAvailable() == true,
            isBackupAvailable = backupService != null,
            isCompanionSyncAvailable = companionSyncCoordinator != null,
            canDeleteStorage = twoFacLib != null,
            hasSecureStorage = secureSessionManager != null,
            isSecureUnlockEnabled = initialSecureUnlockEnabled(),
            unlockMode = initialUnlockMode(),
        )
    )
    val uiState = _uiState.asStateFlow()

    private val _snackbarMessages = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val snackbarMessages: SharedFlow<String> = _snackbarMessages.asSharedFlow()

    init {
        viewModelScope.launch {
            appPreferencesRepository.preferencesFlow.collect { preferences ->
                _uiState.update { it.copy(appPreferences = preferences) }
            }
        }
        companionSyncCoordinator?.companionActiveFlow?.let { companionFlow ->
            viewModelScope.launch {
                companionFlow.collect { isActive ->
                    _uiState.update { it.copy(isCompanionActive = isActive) }
                }
            }
        }
        refreshBackupProviders()
        refreshCompanionState()
    }

    fun onRememberPasskeyToggleChanged(enabled: Boolean) {
        val manager = sessionManager ?: return
        if (!enabled) {
            secureSessionManager?.setSecureUnlockEnabled(false)
            manager.setRememberPasskey(false)
            _uiState.update {
                it.copy(
                    isSecureUnlockEnabled = false,
                    showEnrollmentDialog = false,
                    enrollmentError = null,
                )
            }
            return
        }

        if (secureSessionManager != null) {
            _uiState.update { it.copy(showEnrollmentDialog = true, enrollmentError = null) }
        } else {
            manager.setRememberPasskey(true)
            _uiState.update { it.copy(isSecureUnlockEnabled = true) }
        }
    }

    fun onShowUpcomingCodeChanged(enabled: Boolean) {
        viewModelScope.launch {
            appPreferencesRepository.setShowUpcomingCode(enabled)
        }
    }

    fun requestDeleteStorage() {
        _uiState.update { it.copy(showDeleteStorageDialog = true) }
    }

    fun dismissDeleteStorageDialog() {
        if (_uiState.value.isDeleteStorageInProgress) return
        _uiState.update { it.copy(showDeleteStorageDialog = false) }
    }

    fun confirmDeleteStorage() {
        val lib = twoFacLib
        if (lib == null) {
            _uiState.update { it.copy(showDeleteStorageDialog = false) }
            viewModelScope.launch {
                emitSnackbar(getString(Res.string.settings_account_deletion_unavailable))
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isDeleteStorageInProgress = true) }
            try {
                val deleted = lib.deleteAllAccountsFromStorage()
                if (!deleted) {
                    emitSnackbar(getString(Res.string.settings_account_delete_failed))
                    return@launch
                }
                try {
                    companionSyncCoordinator?.onAccountsChanged()
                    emitSnackbar(getString(Res.string.settings_account_all_deleted))
                } catch (e: Exception) {
                    throwIfCancellation(e)
                    emitSnackbar(
                        getString(
                            Res.string.settings_account_deleted_sync_error,
                            e.message ?: getString(Res.string.error_unknown),
                        )
                    )
                }
                _uiState.update { it.copy(showDeleteStorageDialog = false) }
            } catch (e: Exception) {
                throwIfCancellation(e)
                emitSnackbar(
                    getString(
                        Res.string.settings_account_delete_exception,
                        e.message ?: getString(Res.string.error_unknown),
                    )
                )
            } finally {
                _uiState.update { it.copy(isDeleteStorageInProgress = false) }
            }
        }
    }

    fun requestBackupExport(providerId: String) {
        _uiState.update { it.copy(exportProviderId = providerId) }
    }

    fun dismissBackupExportDialog() {
        _uiState.update { it.copy(exportProviderId = null) }
    }

    fun onBackupExportModeSelected(encrypted: Boolean) {
        val providerId = _uiState.value.exportProviderId ?: return
        _uiState.update { it.copy(exportProviderId = null) }
        if (twoFacLib?.isUnlocked() == true) {
            runLoadingAction {
                executeBackupExport(providerId = providerId, encrypted = encrypted)
            }
        } else {
            _uiState.update {
                it.copy(
                    pendingAction = SettingsBackupAction.Export(providerId, encrypted),
                    passkeyError = null,
                )
            }
        }
    }

    fun importBackup(providerId: String) {
        runLoadingAction {
            try {
                prepareBackupImport(providerId)
            } catch (e: Exception) {
                throwIfCancellation(e)
                emitSnackbar(
                    getString(
                        Res.string.backup_import_exception,
                        e.message ?: getString(Res.string.error_unknown),
                    )
                )
            }
        }
    }

    fun syncCompanion() {
        if (twoFacLib?.isUnlocked() != true) {
            _uiState.update { it.copy(pendingAction = SettingsBackupAction.SyncCompanion, passkeyError = null) }
            return
        }

        viewModelScope.launch {
            performCompanionSync()
        }
    }

    fun discoverCompanion() {
        val coordinator = companionSyncCoordinator ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isCompanionDiscoveryInProgress = true) }
            try {
                val discovered = coordinator.forceDiscoverCompanion()
                val message = if (discovered) {
                    getString(Res.string.settings_companion_discovered, currentCompanionDisplayName())
                } else {
                    getString(Res.string.settings_companion_not_discovered, currentCompanionDisplayName())
                }
                emitSnackbar(message)
            } finally {
                _uiState.update { it.copy(isCompanionDiscoveryInProgress = false) }
            }
        }
    }

    fun submitPendingActionPasskey(passkey: String) {
        val selectedAction = _uiState.value.pendingAction ?: return
        runLoadingAction(
            onStart = { _uiState.update { it.copy(passkeyError = null) } },
            onFailure = { error ->
                _uiState.update {
                    it.copy(passkeyError = error.message ?: getString(Res.string.error_operation_failed))
                }
            }
        ) {
            twoFacLib?.unlock(passkey)
            when (selectedAction) {
                is SettingsBackupAction.Export -> {
                    executeBackupExport(selectedAction.providerId, selectedAction.encrypted)
                    _uiState.update { it.copy(pendingAction = null) }
                }

                is SettingsBackupAction.Import -> {
                    executeBackupImport(
                        providerId = selectedAction.providerId,
                        backupId = selectedAction.backupId,
                    )
                    _uiState.update { it.copy(pendingAction = null) }
                }

                SettingsBackupAction.SyncCompanion -> {
                    performCompanionSync()
                    _uiState.update { it.copy(pendingAction = null) }
                }
            }
        }
    }

    fun dismissPendingAction() {
        _uiState.update { it.copy(pendingAction = null, passkeyError = null) }
    }

    fun submitBackupRestorePasskey(passkey: String) {
        val importRequest = _uiState.value.encryptedImportRequest ?: return
        runLoadingAction(
            onStart = { _uiState.update { it.copy(backupRestorePasskeyError = null) } }
        ) {
            val lib = twoFacLib
            if (lib == null) {
                _uiState.update {
                    it.copy(backupRestorePasskeyError = getString(Res.string.backup_restore_unavailable))
                }
                return@runLoadingAction
            }
            try {
                importRequest.encryptedAccounts.forEach { account ->
                    lib.decryptEncryptedBackupAccount(account, passkey)
                }
                _uiState.update {
                    it.copy(
                        encryptedImportRequest = importRequest.copy(backupPasskey = passkey),
                        backupRestorePasskeyError = null,
                    )
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        backupRestorePasskeyError = getString(Res.string.backup_passkey_incorrect),
                    )
                }
            }
        }
    }

    fun submitCurrentRestorePasskey(currentPasskey: String) {
        val importRequest = _uiState.value.encryptedImportRequest ?: return
        val backupPasskey = importRequest.backupPasskey ?: return
        runLoadingAction(
            onStart = { _uiState.update { it.copy(currentRestorePasskeyError = null) } },
            onFailure = { error ->
                _uiState.update {
                    it.copy(
                        currentRestorePasskeyError = error.message ?: getString(Res.string.settings_verify_passkey_failed),
                    )
                }
            }
        ) {
            val service = backupService
            if (service == null) {
                _uiState.update {
                    it.copy(currentRestorePasskeyError = getString(Res.string.backup_restore_unavailable))
                }
                return@runLoadingAction
            }
            val result = service.restoreBackup(
                providerId = importRequest.providerId,
                backupId = importRequest.backupId,
                backupPasskey = backupPasskey,
                currentPasskey = currentPasskey,
            )
            when (result) {
                is BackupResult.Success -> {
                    emitSnackbar(
                        getString(
                            Res.string.backup_import_success,
                            result.value.toString(),
                            importRequest.backupId,
                        )
                    )
                    companionSyncCoordinator?.onAccountsChanged()
                    accountsViewModel?.reloadAccounts()
                    _uiState.update {
                        it.copy(
                            backupProviders = service.listProviders(),
                            encryptedImportRequest = null,
                            backupRestorePasskeyError = null,
                            currentRestorePasskeyError = null,
                        )
                    }
                }

                is BackupResult.Failure -> {
                    _uiState.update { it.copy(currentRestorePasskeyError = result.message) }
                }
            }
        }
    }

    fun dismissEncryptedImportRequest() {
        _uiState.update {
            it.copy(
                encryptedImportRequest = null,
                backupRestorePasskeyError = null,
                currentRestorePasskeyError = null,
            )
        }
    }

    fun submitEnrollmentPasskey(passkey: String) {
        val secureManager = secureSessionManager ?: return
        runLoadingAction(
            onStart = { _uiState.update { it.copy(enrollmentError = null) } },
            onFailure = { error ->
                _uiState.update {
                    it.copy(
                        enrollmentError = error.message ?: getString(Res.string.settings_verify_passkey_failed),
                    )
                }
            }
        ) {
            val lib = twoFacLib
            if (lib == null) {
                _uiState.update {
                    it.copy(enrollmentError = getString(Res.string.settings_secure_enrollment_unavailable))
                }
                return@runLoadingAction
            }
            lib.unlock(passkey)
            val enrolled = secureManager.enrollPasskey(passkey)
            if (enrolled) {
                secureManager.setSecureUnlockEnabled(true)
                sessionManager?.setRememberPasskey(true)
                _uiState.update {
                    it.copy(
                        isSecureUnlockEnabled = true,
                        showEnrollmentDialog = false,
                        enrollmentError = null,
                    )
                }
                onboardingViewModel?.refreshAndSyncDerivedCompletion()
                emitSnackbar(
                    getString(
                        if (biometricSessionManager != null) {
                            Res.string.settings_biometric_unlock_enabled
                        } else {
                            Res.string.settings_secure_unlock_enabled
                        }
                    )
                )
            } else {
                _uiState.update {
                    it.copy(
                        enrollmentError = getString(
                            if (biometricSessionManager != null) {
                                Res.string.settings_biometric_enrollment_cancelled
                            } else {
                                Res.string.settings_secure_enrollment_cancelled
                            }
                        )
                    )
                }
            }
        }
    }

    fun dismissEnrollmentDialog() {
        _uiState.update { it.copy(showEnrollmentDialog = false, enrollmentError = null) }
    }

    private fun initialUnlockMode(): SettingsUnlockMode {
        return when {
            biometricSessionManager != null -> SettingsUnlockMode.BIOMETRIC
            secureSessionManager != null -> SettingsUnlockMode.SECURE_UNLOCK
            else -> SettingsUnlockMode.REMEMBER_PASSKEY
        }
    }

    private fun initialSecureUnlockEnabled(): Boolean {
        return if (secureSessionManager != null) {
            secureSessionManager.isSecureUnlockEnabled()
        } else {
            sessionManager?.isRememberPasskeyEnabled() ?: false
        }
    }

    private fun refreshBackupProviders() {
        val service = backupService ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(backupProviders = service.listProviders()) }
        }
    }

    private fun refreshCompanionState() {
        val coordinator = companionSyncCoordinator ?: return
        viewModelScope.launch {
            val defaultName = getString(Res.string.settings_companion_default_name)
            _uiState.update {
                it.copy(
                    companionDisplayName = coordinator.companionDisplayName.ifBlank { defaultName },
                )
            }
            coordinator.isCompanionActive()
        }
    }

    private fun runLoadingAction(
        onStart: (() -> Unit)? = null,
        onFailure: (suspend (Exception) -> Unit)? = null,
        block: suspend () -> Unit,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            onStart?.invoke()
            try {
                block()
            } catch (e: Exception) {
                throwIfCancellation(e)
                if (onFailure != null) {
                    onFailure(e)
                } else {
                    throw e
                }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private suspend fun executeBackupExport(providerId: String, encrypted: Boolean) {
        val service = backupService
        if (service == null) {
            emitSnackbar(getString(Res.string.backup_unavailable_message))
            return
        }
        val result = service.createBackup(providerId, encrypted = encrypted)
        val message = when (result) {
            is BackupResult.Success ->
                if (encrypted) {
                    getString(Res.string.backup_export_encrypted_success, result.value.id)
                } else {
                    getString(Res.string.backup_export_plaintext_success, result.value.id)
                }

            is BackupResult.Failure ->
                getString(Res.string.backup_export_failed, result.message)
        }
        emitSnackbar(message)
        _uiState.update { it.copy(backupProviders = service.listProviders()) }
    }

    private suspend fun executeBackupImport(
        providerId: String,
        backupId: String,
        backupPasskey: String? = null,
        currentPasskey: String? = null,
    ) {
        val service = backupService
        if (service == null) {
            emitSnackbar(getString(Res.string.backup_unavailable_message))
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
        emitSnackbar(message)
        if (result is BackupResult.Success) {
            companionSyncCoordinator?.onAccountsChanged()
            accountsViewModel?.reloadAccounts()
        }
        _uiState.update { it.copy(backupProviders = service.listProviders()) }
    }

    private suspend fun prepareBackupImport(providerId: String) {
        val service = backupService
        if (service == null) {
            emitSnackbar(getString(Res.string.backup_unavailable_message))
            return
        }
        val listResult = service.listBackups(providerId)
        if (listResult is BackupResult.Failure) {
            emitSnackbar(getString(Res.string.backup_no_backups_found, listResult.message))
            return
        }
        val backups = (listResult as BackupResult.Success).value
        if (backups.isEmpty()) {
            emitSnackbar(getString(Res.string.backup_no_files_found))
            return
        }
        val latest = backups.maxBy { it.createdAt }
        val inspectionResult = service.inspectBackup(providerId, latest.id)
        val payload = when (inspectionResult) {
            is BackupResult.Success -> inspectionResult.value
            is BackupResult.Failure -> {
                emitSnackbar(getString(Res.string.backup_import_failed, inspectionResult.message))
                return
            }
        }
        if (payload.encrypted) {
            _uiState.update {
                it.copy(
                    encryptedImportRequest = EncryptedImportRequest(
                        providerId = providerId,
                        backupId = latest.id,
                        encryptedAccounts = payload.encryptedAccounts,
                    ),
                    backupRestorePasskeyError = null,
                    currentRestorePasskeyError = null,
                )
            }
            return
        }
        if (twoFacLib?.isUnlocked() == true) {
            executeBackupImport(providerId = providerId, backupId = latest.id)
        } else {
            _uiState.update {
                it.copy(
                    pendingAction = SettingsBackupAction.Import(providerId = providerId, backupId = latest.id),
                    passkeyError = null,
                )
            }
        }
    }

    private suspend fun performCompanionSync() {
        val coordinator = companionSyncCoordinator
        val lib = twoFacLib
        if (coordinator == null || lib == null) {
            emitSnackbar(getString(Res.string.settings_companion_sync_unavailable))
            return
        }
        if (lib.getAllAccounts().isEmpty()) {
            emitSnackbar(getString(Res.string.settings_companion_no_accounts, currentCompanionDisplayName()))
            return
        }
        _uiState.update { it.copy(isCompanionSyncInProgress = true) }
        try {
            val synced = coordinator.syncNow(manual = true)
            val message = if (synced) {
                getString(Res.string.settings_companion_sync_sent, currentCompanionDisplayName())
            } else {
                getString(Res.string.settings_companion_sync_failed, currentCompanionDisplayName())
            }
            emitSnackbar(message)
            coordinator.isCompanionActive()
        } finally {
            _uiState.update { it.copy(isCompanionSyncInProgress = false) }
        }
    }

    private fun currentCompanionDisplayName(): String {
        return _uiState.value.companionDisplayName.ifBlank {
            companionSyncCoordinator?.companionDisplayName.orEmpty()
        }
    }

    private suspend fun emitSnackbar(message: String) {
        _snackbarMessages.emit(message)
    }

    private fun throwIfCancellation(error: Exception) {
        if (error is CancellationException) {
            throw error
        }
    }
}
