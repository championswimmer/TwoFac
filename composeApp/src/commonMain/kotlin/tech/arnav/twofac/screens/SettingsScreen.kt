package tech.arnav.twofac.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.koin.compose.getKoin
import tech.arnav.twofac.backup.AuthorizableBackupTransport
import tech.arnav.twofac.backup.BackupPreferencesManager
import tech.arnav.twofac.backup.BackupAuthorizationChallenge
import tech.arnav.twofac.backup.BackupAuthorizationState
import tech.arnav.twofac.backup.BackupAuthorizationStatus
import tech.arnav.twofac.companion.CompanionSyncCoordinator
import tech.arnav.twofac.companion.isSyncToCompanionEnabled
import tech.arnav.twofac.components.PasskeyDialog
import tech.arnav.twofac.lib.TwoFacLib
import tech.arnav.twofac.lib.backup.BackupDescriptor
import tech.arnav.twofac.lib.backup.BackupPreferences
import tech.arnav.twofac.lib.backup.BackupProvider
import tech.arnav.twofac.lib.backup.BackupResult
import tech.arnav.twofac.lib.backup.BackupService
import tech.arnav.twofac.lib.backup.BackupTransportRegistry
import tech.arnav.twofac.lib.backup.providerPreferences
import tech.arnav.twofac.session.BiometricSessionManager
import tech.arnav.twofac.session.SessionManager
import tech.arnav.twofac.session.WebAuthnSessionManager
import tech.arnav.twofac.storage.getStoragePath

private enum class BackupActionType { EXPORT, IMPORT, SYNC_COMPANION }

private data class BackupAction(
    val type: BackupActionType,
    val providerId: String? = null,
)

private data class RestoreSelectionState(
    val provider: BackupProvider,
    val backups: List<BackupDescriptor>,
)

private data class RestoreConfirmationState(
    val provider: BackupProvider,
    val backup: BackupDescriptor,
    val existingAccountCount: Int,
)

private data class BackupAuthorizationConfigState(
    val provider: BackupProvider,
    val clientId: String = "",
)

private data class BackupAuthorizationFlowState(
    val provider: BackupProvider,
    val challenge: BackupAuthorizationChallenge,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: (() -> Unit)? = null,
    onQuit: (() -> Unit)? = null
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Optional backup dependencies – only registered on platforms that support local file backup
    val koin = getKoin()
    val backupService = remember { koin.getOrNull<BackupService>() }
    val backupRegistry = remember { koin.getOrNull<BackupTransportRegistry>() }
    val backupPreferencesManager = remember { koin.getOrNull<BackupPreferencesManager>() }
    val backupProviders = remember(backupRegistry) { backupRegistry?.all().orEmpty() }
    val backupPreferences by (backupPreferencesManager?.updates?.collectAsState(BackupPreferences())
        ?: remember { mutableStateOf(BackupPreferences()) })
    val twoFacLib = remember { koin.getOrNull<TwoFacLib>() }
    val companionSyncCoordinator = remember { koin.getOrNull<CompanionSyncCoordinator>() }
    val sessionManager = remember { koin.getOrNull<SessionManager>() }

    var pendingAction by remember { mutableStateOf<BackupAction?>(null) }
    var restoreSelectionState by remember { mutableStateOf<RestoreSelectionState?>(null) }
    var restoreConfirmationState by remember { mutableStateOf<RestoreConfirmationState?>(null) }
    var authorizationConfigState by remember { mutableStateOf<BackupAuthorizationConfigState?>(null) }
    var authorizationFlowState by remember { mutableStateOf<BackupAuthorizationFlowState?>(null) }
    var providerAuthorizationStates by remember {
        mutableStateOf<Map<String, BackupAuthorizationStatus>>(emptyMap())
    }
    var providerAvailabilityStates by remember {
        mutableStateOf<Map<String, BackupProviderAvailability>>(emptyMap())
    }
    var passkeyError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showDeleteStorageDialog by remember { mutableStateOf(false) }
    var isDeleteStorageInProgress by remember { mutableStateOf(false) }
    var isCompanionActive by remember { mutableStateOf(false) }
    var isCompanionSyncInProgress by remember { mutableStateOf(false) }
    var isCompanionDiscoveryInProgress by remember { mutableStateOf(false) }
    var companionDisplayName by remember {
        mutableStateOf(companionSyncCoordinator?.companionDisplayName ?: "Watch")
    }
    var isRememberPasskeyEnabled by remember {
        mutableStateOf(sessionManager?.isRememberPasskeyEnabled() ?: false)
    }
    val biometricSessionManager = sessionManager as? BiometricSessionManager
    val webAuthnSessionManager = sessionManager as? WebAuthnSessionManager
    val rememberPasskeyTitle =
        if (webAuthnSessionManager != null) "Secure Unlock" else "Remember Passkey"
    val rememberPasskeyDescription = if (webAuthnSessionManager != null) {
        "Require device credential verification before unlock and keep only encrypted passkey data in browser storage."
    } else {
        "Keep the passkey saved so you don't have to enter it every time the extension is opened. Only enable this on devices you trust."
    }
    var isBiometricEnabled by remember {
        mutableStateOf(biometricSessionManager?.isBiometricEnabled() ?: false)
    }
    var showBiometricEnrollmentDialog by remember { mutableStateOf(false) }
    var biometricEnrollmentError by remember { mutableStateOf<String?>(null) }
    var showSecureEnrollmentDialog by remember { mutableStateOf(false) }
    var secureEnrollmentError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(companionSyncCoordinator) {
        companionDisplayName = companionSyncCoordinator?.companionDisplayName ?: "Watch"
        if (companionSyncCoordinator != null) {
            isCompanionActive = companionSyncCoordinator.isCompanionActive()
        }
    }

    LaunchedEffect(backupProviders) {
        providerAuthorizationStates = backupProviders.associate { provider ->
            val authorizationStatus = (provider.transport as? AuthorizableBackupTransport)
                ?.authorizationStatus()
                ?: BackupAuthorizationStatus(BackupAuthorizationState.CONNECTED)
            provider.info.id to authorizationStatus
        }
    }

    LaunchedEffect(backupProviders, providerAuthorizationStates) {
        providerAvailabilityStates = backupProviders.associate { provider ->
            val isAvailable = provider.transport.isAvailable()
            val detail = provider.transport.availabilityDetail()
            provider.info.id to BackupProviderAvailability(
                isAvailable = isAvailable,
                detail = detail,
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    onNavigateBack?.let { navigateBack ->
                        IconButton(onClick = navigateBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
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
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top)
        ) {

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Storage Location",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { showDeleteStorageDialog = true },
                            enabled = twoFacLib != null && !isDeleteStorageInProgress && !isLoading
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "Delete all accounts",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    Text(
                        text = "Accounts are saved at:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = getStoragePath(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp),
                        textAlign = TextAlign.Start
                    )
                }
            }

            if (sessionManager != null && sessionManager.isAvailable()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = rememberPasskeyTitle,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Switch(
                                checked = isRememberPasskeyEnabled,
                                onCheckedChange = { enabled ->
                                    if (!enabled) {
                                        sessionManager.setRememberPasskey(false)
                                        isRememberPasskeyEnabled = false
                                        showSecureEnrollmentDialog = false
                                        secureEnrollmentError = null
                                        biometricSessionManager?.setBiometricEnabled(false)
                                        isBiometricEnabled = false
                                        return@Switch
                                    }
                                    if (webAuthnSessionManager != null) {
                                        secureEnrollmentError = null
                                        showSecureEnrollmentDialog = true
                                        return@Switch
                                    }
                                    sessionManager.setRememberPasskey(true)
                                    isRememberPasskeyEnabled = true
                                }
                            )
                        }
                        Text(
                            text = rememberPasskeyDescription,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        // Biometric unlock toggle — only shown when biometric hardware is available
                        if (biometricSessionManager != null && biometricSessionManager.isBiometricAvailable()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Biometric Unlock",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "Use fingerprint or face recognition to unlock your vault",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Switch(
                                    checked = isBiometricEnabled,
                                    onCheckedChange = { enabled ->
                                        if (enabled) {
                                            // Start enrollment flow: prompt for passkey first
                                            showBiometricEnrollmentDialog = true
                                        } else {
                                            biometricSessionManager.setBiometricEnabled(false)
                                            isBiometricEnabled = false
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (backupService != null && backupProviders.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Backups",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Export or import accounts using any available backup provider.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            backupProviders.forEach { provider ->
                                BackupProviderActions(
                                    provider = provider,
                                    backupPreferences = backupPreferences,
                                    authorizationStatus = providerAuthorizationStates[provider.info.id],
                                    availability = providerAvailabilityStates[provider.info.id],
                                    isLoading = isLoading,
                                    onExport = {
                                        pendingAction = BackupAction(
                                            type = BackupActionType.EXPORT,
                                            providerId = provider.info.id,
                                        )
                                    },
                                    onImport = {
                                        pendingAction = BackupAction(
                                            type = BackupActionType.IMPORT,
                                            providerId = provider.info.id,
                                        )
                                    },
                                    automaticRestoreSelectedProviderId =
                                        backupPreferences.selectedAutomaticRestoreProviderId,
                                    onAutomaticRestoreChange = { enabled ->
                                        coroutineScope.launch {
                                            try {
                                                backupPreferencesManager?.setAutomaticRestoreProvider(
                                                    providerId = if (enabled) provider.info.id else null,
                                                    providers = backupProviders,
                                                )
                                                val message = if (enabled) {
                                                    "Automatic restore enabled for ${provider.info.displayName}"
                                                } else {
                                                    "Automatic restore disabled"
                                                }
                                                snackbarHostState.showSnackbar(message)
                                            } catch (e: IllegalArgumentException) {
                                                snackbarHostState.showSnackbar(e.message ?: "Unable to update automatic restore")
                                            }
                                        }
                                    },
                                    onConfigureAuthorization = {
                                        authorizationConfigState = BackupAuthorizationConfigState(provider = provider)
                                    },
                                    onBeginAuthorization = {
                                        coroutineScope.launch {
                                            val authorizable = provider.transport as? AuthorizableBackupTransport
                                            if (authorizable == null) {
                                                snackbarHostState.showSnackbar("${provider.info.displayName} does not support sign-in from settings yet")
                                                return@launch
                                            }
                                            val result = authorizable.beginAuthorization()
                                            when (result) {
                                                is BackupResult.Success -> {
                                                    authorizationFlowState = BackupAuthorizationFlowState(
                                                        provider = provider,
                                                        challenge = result.value,
                                                    )
                                                }
                                                is BackupResult.Failure -> {
                                                    snackbarHostState.showSnackbar(result.message)
                                                }
                                            }
                                            providerAuthorizationStates = providerAuthorizationStates +
                                                (provider.info.id to authorizable.authorizationStatus())
                                            providerAvailabilityStates = providerAvailabilityStates +
                                                (
                                                    provider.info.id to BackupProviderAvailability(
                                                        isAvailable = provider.transport.isAvailable(),
                                                        detail = provider.transport.availabilityDetail(),
                                                    )
                                                    )
                                        }
                                    },
                                    onDisconnectAuthorization = {
                                        coroutineScope.launch {
                                            val authorizable = provider.transport as? AuthorizableBackupTransport
                                            if (authorizable == null) return@launch
                                            val result = authorizable.disconnectAuthorization()
                                            val message = when (result) {
                                                is BackupResult.Success -> "${provider.info.displayName} disconnected"
                                                is BackupResult.Failure -> result.message
                                            }
                                            providerAuthorizationStates = providerAuthorizationStates +
                                                (provider.info.id to authorizable.authorizationStatus())
                                            providerAvailabilityStates = providerAvailabilityStates +
                                                (
                                                    provider.info.id to BackupProviderAvailability(
                                                        isAvailable = provider.transport.isAvailable(),
                                                        detail = provider.transport.availabilityDetail(),
                                                    )
                                                    )
                                            snackbarHostState.showSnackbar(message)
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }

            if (companionSyncCoordinator != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Companion Sync",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = if (isCompanionActive) {
                                "$companionDisplayName companion is active."
                            } else {
                                "$companionDisplayName companion is not active."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Button(
                            onClick = {
                                if (twoFacLib != null && !twoFacLib.isUnlocked()) {
                                    pendingAction = BackupAction(type = BackupActionType.SYNC_COMPANION)
                                    return@Button
                                }
                                coroutineScope.launch {
                                    try {
                                        isCompanionSyncInProgress = true
                                        if (twoFacLib != null && twoFacLib.getAllAccounts().isEmpty()) {
                                            snackbarHostState.showSnackbar("No accounts to sync to $companionDisplayName")
                                            return@launch
                                        }
                                        val synced = companionSyncCoordinator.syncNow(manual = true)
                                        if (synced) {
                                            snackbarHostState.showSnackbar("Sync sent to $companionDisplayName")
                                        } else {
                                            snackbarHostState.showSnackbar("Unable to sync to $companionDisplayName right now")
                                        }
                                        isCompanionActive = companionSyncCoordinator.isCompanionActive()
                                    } finally {
                                        isCompanionSyncInProgress = false
                                    }
                                }
                            },
                            enabled = isSyncToCompanionEnabled(
                                isCompanionActive = isCompanionActive,
                                isSyncInProgress = isCompanionSyncInProgress,
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Sync to $companionDisplayName")
                        }
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    try {
                                        isCompanionDiscoveryInProgress = true
                                        isCompanionActive =
                                            companionSyncCoordinator.forceDiscoverCompanion()
                                        val message = if (isCompanionActive) {
                                            "$companionDisplayName companion discovered"
                                        } else {
                                            "Unable to discover $companionDisplayName companion"
                                        }
                                        snackbarHostState.showSnackbar(message)
                                    } finally {
                                        isCompanionDiscoveryInProgress = false
                                    }
                                }
                            },
                            enabled = !isCompanionSyncInProgress && !isCompanionDiscoveryInProgress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Text("Force Discover Companion")
                        }
                    }
                }
            }

            PlatformSettingsContent(onQuit = onQuit)
        }
    }

    if (showDeleteStorageDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isDeleteStorageInProgress) {
                    showDeleteStorageDialog = false
                }
            },
            title = { Text("Delete all accounts?") },
            text = {
                Text(
                    "This deletes all existing accounts and cannot be undone unless you have a backup.\n\n" +
                        "A fresh storage file will be created on next run/use."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (twoFacLib == null) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Account deletion is unavailable")
                            }
                            showDeleteStorageDialog = false
                            return@Button
                        }
                        coroutineScope.launch {
                            isDeleteStorageInProgress = true
                            try {
                                val deleted = twoFacLib.deleteAllAccountsFromStorage()
                                if (!deleted) {
                                    snackbarHostState.showSnackbar("Failed to delete accounts from storage")
                                    return@launch
                                }
                                try {
                                    companionSyncCoordinator?.onAccountsChanged()
                                    snackbarHostState.showSnackbar("All accounts deleted from storage")
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar(
                                        "Accounts deleted, but companion sync failed: ${e.message ?: "unknown error"}"
                                    )
                                }
                                showDeleteStorageDialog = false
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar(
                                    "Delete failed: ${e.message ?: "unknown error"}"
                                )
                            } finally {
                                isDeleteStorageInProgress = false
                            }
                        }
                    },
                    enabled = !isDeleteStorageInProgress
                ) {
                    Text(if (isDeleteStorageInProgress) "Deleting..." else "Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteStorageDialog = false },
                    enabled = !isDeleteStorageInProgress
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Passkey dialog for backup operations
    if (pendingAction != null) {
        PasskeyDialog(
            isVisible = true,
            isLoading = isLoading,
            error = passkeyError,
            onPasskeySubmit = { passkey ->
                val action = pendingAction ?: return@PasskeyDialog
                val backupProvider = action.providerId?.let { backupRegistry?.get(it) }
                passkeyError = null
                isLoading = true
                coroutineScope.launch {
                    try {
                        twoFacLib?.unlock(passkey)
                        when (action.type) {
                            BackupActionType.EXPORT -> {
                                if (backupProvider == null || backupService == null) {
                                    snackbarHostState.showSnackbar("Backup provider is unavailable")
                                    pendingAction = null
                                    return@launch
                                }
                                if (!backupProvider.transport.isAvailable()) {
                                    snackbarHostState.showSnackbar("${backupProvider.info.displayName} is unavailable")
                                    pendingAction = null
                                    return@launch
                                }
                                val result = backupService.createBackup(backupProvider.transport)
                                val message = when (result) {
                                    is BackupResult.Success ->
                                        "${backupProvider.info.displayName} export created: ${result.value.id}"
                                    is BackupResult.Failure ->
                                        "Export failed: ${result.message}"
                                }
                                if (result is BackupResult.Success) {
                                    backupPreferencesManager?.recordBackupSuccess(
                                        provider = backupProvider,
                                        descriptor = result.value,
                                    )
                                }
                                snackbarHostState.showSnackbar(message)
                                pendingAction = null
                            }
                            BackupActionType.IMPORT -> {
                                if (backupProvider == null || backupService == null) {
                                    snackbarHostState.showSnackbar("Backup provider is unavailable")
                                    pendingAction = null
                                    return@launch
                                }
                                if (!backupProvider.transport.isAvailable()) {
                                    snackbarHostState.showSnackbar("${backupProvider.info.displayName} is unavailable")
                                    pendingAction = null
                                    return@launch
                                }
                                val listResult = backupService.listBackups(backupProvider.transport)
                                if (listResult is BackupResult.Failure) {
                                    snackbarHostState.showSnackbar("No backups found: ${listResult.message}")
                                    pendingAction = null
                                    return@launch
                                }
                                val backups = (listResult as BackupResult.Success).value
                                if (backups.isEmpty()) {
                                    snackbarHostState.showSnackbar("No backup files found")
                                    pendingAction = null
                                    return@launch
                                }
                                restoreSelectionState = RestoreSelectionState(
                                    provider = backupProvider,
                                    backups = backups.sortedByDescending { it.createdAt },
                                )
                                pendingAction = null
                            }

                            BackupActionType.SYNC_COMPANION -> {
                                if (companionSyncCoordinator == null || twoFacLib == null) {
                                    snackbarHostState.showSnackbar("Companion sync is unavailable")
                                    pendingAction = null
                                    return@launch
                                }
                                val hasAccounts = twoFacLib.getAllAccounts().isNotEmpty()
                                if (!hasAccounts) {
                                    snackbarHostState.showSnackbar("No accounts to sync to $companionDisplayName")
                                    pendingAction = null
                                    return@launch
                                }
                                try {
                                    isCompanionSyncInProgress = true
                                    val synced = companionSyncCoordinator.syncNow(manual = true)
                                    val message = if (synced) {
                                        "Sync sent to $companionDisplayName"
                                    } else {
                                        "Unable to sync to $companionDisplayName right now"
                                    }
                                    snackbarHostState.showSnackbar(message)
                                    isCompanionActive =
                                        companionSyncCoordinator.isCompanionActive()
                                } finally {
                                    isCompanionSyncInProgress = false
                                }
                                pendingAction = null
                            }
                        }
                    } catch (e: Exception) {
                        passkeyError = e.message ?: "Operation failed"
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

    if (showSecureEnrollmentDialog && webAuthnSessionManager != null) {
        PasskeyDialog(
            isVisible = true,
            isLoading = isLoading,
            error = secureEnrollmentError,
            onPasskeySubmit = { passkey ->
                secureEnrollmentError = null
                isLoading = true
                coroutineScope.launch {
                    try {
                        if (twoFacLib == null) {
                            secureEnrollmentError = "Secure unlock is unavailable"
                            return@launch
                        }
                        twoFacLib.unlock(passkey)
                        webAuthnSessionManager.setSecureUnlockEnabled(true)
                        val enrolled = webAuthnSessionManager.enrollPasskey(passkey)
                        if (enrolled) {
                            isRememberPasskeyEnabled =
                                webAuthnSessionManager.isSecureUnlockEnabled()
                            showSecureEnrollmentDialog = false
                            snackbarHostState.showSnackbar("Secure unlock enabled")
                        } else {
                            webAuthnSessionManager.setSecureUnlockEnabled(false)
                            isRememberPasskeyEnabled = false
                            secureEnrollmentError = "Secure unlock enrollment cancelled"
                        }
                    } catch (e: Exception) {
                        webAuthnSessionManager.setSecureUnlockEnabled(false)
                        isRememberPasskeyEnabled = false
                        secureEnrollmentError = e.message ?: "Failed to verify passkey"
                    } finally {
                        isLoading = false
                    }
                }
            },
            onDismiss = {
                showSecureEnrollmentDialog = false
                secureEnrollmentError = null
            }
        )
    }

    // Passkey dialog for biometric enrollment
    if (showBiometricEnrollmentDialog && biometricSessionManager != null) {
        PasskeyDialog(
            isVisible = true,
            isLoading = isLoading,
            error = biometricEnrollmentError,
            onPasskeySubmit = { passkey ->
                biometricEnrollmentError = null
                isLoading = true
                coroutineScope.launch {
                    try {
                        // Verify passkey is correct
                        twoFacLib?.unlock(passkey)
                        // Enable biometric and enroll the passkey
                        biometricSessionManager.setBiometricEnabled(true)
                        sessionManager.setRememberPasskey(true)
                        val enrolled = biometricSessionManager.enrollPasskey(passkey)
                        if (enrolled) {
                            isBiometricEnabled = true
                            isRememberPasskeyEnabled = true
                            showBiometricEnrollmentDialog = false
                            snackbarHostState.showSnackbar("Biometric unlock enabled")
                        } else {
                            biometricSessionManager.setBiometricEnabled(false)
                            biometricEnrollmentError = "Biometric enrollment cancelled"
                        }
                    } catch (e: Exception) {
                        biometricEnrollmentError = e.message ?: "Failed to verify passkey"
                    } finally {
                        isLoading = false
                    }
                }
            },
            onDismiss = {
                showBiometricEnrollmentDialog = false
                biometricEnrollmentError = null
            }
        )
    }

    if (restoreSelectionState != null) {
        val restoreState = restoreSelectionState!!
        AlertDialog(
            onDismissRequest = { restoreSelectionState = null },
            title = { Text("Restore from ${restoreState.provider.info.displayName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Choose which backup snapshot to import.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    restoreState.backups.forEach { backup ->
                        OutlinedButton(
                            onClick = {
                                restoreSelectionState = null
                                val existingAccountCount = twoFacLib?.getAllAccounts()?.size ?: 0
                                if (existingAccountCount > 0) {
                                    restoreConfirmationState = RestoreConfirmationState(
                                        provider = restoreState.provider,
                                        backup = backup,
                                        existingAccountCount = existingAccountCount,
                                    )
                                } else {
                                    coroutineScope.launch {
                                        restoreBackup(
                                            backupService = backupService,
                                            backupPreferencesManager = backupPreferencesManager,
                                            provider = restoreState.provider,
                                            backup = backup,
                                            companionSyncCoordinator = companionSyncCoordinator,
                                            snackbarHostState = snackbarHostState,
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(backup.id)
                                backupDescriptorMetadataLines(backup).forEach { line ->
                                    Text(
                                        text = line,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { restoreSelectionState = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (restoreConfirmationState != null) {
        val confirmationState = restoreConfirmationState!!
        AlertDialog(
            onDismissRequest = { restoreConfirmationState = null },
            title = { Text("Import into existing vault?") },
            text = {
                Text(
                    "You already have ${confirmationState.existingAccountCount} account(s). " +
                        "Importing ${confirmationState.backup.id} from ${confirmationState.provider.info.displayName} " +
                        "will add accounts on top of your current vault and skip any duplicates or invalid entries."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        restoreConfirmationState = null
                        coroutineScope.launch {
                            restoreBackup(
                                backupService = backupService,
                                backupPreferencesManager = backupPreferencesManager,
                                provider = confirmationState.provider,
                                backup = confirmationState.backup,
                                companionSyncCoordinator = companionSyncCoordinator,
                                snackbarHostState = snackbarHostState,
                            )
                        }
                    }
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { restoreConfirmationState = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (authorizationConfigState != null) {
        val configState = authorizationConfigState!!
        var clientId by remember(configState.provider.info.id) { mutableStateOf(configState.clientId) }
        AlertDialog(
            onDismissRequest = { authorizationConfigState = null },
            title = { Text("Configure ${configState.provider.info.displayName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Create an OAuth 2.0 client ID in Google Cloud Console, then enter it here so TwoFac can start the Google Drive device-code flow.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    TextField(
                        value = clientId,
                        onValueChange = { clientId = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("OAuth Client ID") },
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val authorizable = configState.provider.transport as? AuthorizableBackupTransport
                            if (authorizable == null) {
                                snackbarHostState.showSnackbar("${configState.provider.info.displayName} does not support configuration")
                                authorizationConfigState = null
                                return@launch
                            }
                            val result = authorizable.configureAuthorization(clientId)
                            val message = when (result) {
                                is BackupResult.Success -> {
                                    providerAuthorizationStates = providerAuthorizationStates +
                                        (configState.provider.info.id to authorizable.authorizationStatus())
                                    providerAvailabilityStates = providerAvailabilityStates +
                                        (
                                            configState.provider.info.id to BackupProviderAvailability(
                                                isAvailable = configState.provider.transport.isAvailable(),
                                                detail = configState.provider.transport.availabilityDetail(),
                                            )
                                            )
                                    authorizationConfigState = null
                                    "${configState.provider.info.displayName} client ID saved"
                                }
                                is BackupResult.Failure -> result.message
                            }
                            snackbarHostState.showSnackbar(message)
                        }
                    },
                    enabled = clientId.isNotBlank(),
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { authorizationConfigState = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (authorizationFlowState != null) {
        val flowState = authorizationFlowState!!
        AlertDialog(
            onDismissRequest = { authorizationFlowState = null },
            title = { Text("Connect ${flowState.provider.info.displayName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "1. Open the verification URL below in a browser.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        "2. Enter the displayed user code.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        "3. Return here and continue once Google shows that the backup access prompt has been approved.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = flowState.challenge.verificationUriComplete
                            ?: flowState.challenge.verificationUri,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "User code: ${flowState.challenge.userCode}",
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val authorizable = flowState.provider.transport as? AuthorizableBackupTransport
                            if (authorizable == null) {
                                snackbarHostState.showSnackbar("${flowState.provider.info.displayName} does not support authorization")
                                authorizationFlowState = null
                                return@launch
                            }
                            isLoading = true
                            val result = authorizable.completeAuthorization(flowState.challenge)
                            val message = when (result) {
                                is BackupResult.Success -> {
                                    providerAuthorizationStates = providerAuthorizationStates +
                                        (flowState.provider.info.id to authorizable.authorizationStatus())
                                    providerAvailabilityStates = providerAvailabilityStates +
                                        (
                                            flowState.provider.info.id to BackupProviderAvailability(
                                                isAvailable = flowState.provider.transport.isAvailable(),
                                                detail = flowState.provider.transport.availabilityDetail(),
                                            )
                                            )
                                    authorizationFlowState = null
                                    "${flowState.provider.info.displayName} connected"
                                }
                                is BackupResult.Failure -> result.message
                            }
                            snackbarHostState.showSnackbar(message)
                            isLoading = false
                        }
                    },
                    enabled = !isLoading,
                ) {
                    Text("I've approved access")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { authorizationFlowState = null },
                    enabled = !isLoading,
                ) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun BackupProviderActions(
    provider: BackupProvider,
    backupPreferences: BackupPreferences,
    authorizationStatus: BackupAuthorizationStatus?,
    availability: BackupProviderAvailability?,
    isLoading: Boolean,
    onExport: () -> Unit,
    onImport: () -> Unit,
    automaticRestoreSelectedProviderId: String?,
    onAutomaticRestoreChange: (Boolean) -> Unit,
    onConfigureAuthorization: () -> Unit,
    onBeginAuthorization: () -> Unit,
    onDisconnectAuthorization: () -> Unit,
) {
    val providerPreferences = backupPreferences.providerPreferences(provider.info.id)
    val resolvedAuthorizationStatus = authorizationStatus
        ?: BackupAuthorizationStatus(BackupAuthorizationState.CONNECTED)
    val resolvedAvailability = availability ?: BackupProviderAvailability(isAvailable = true)
    Column {
        Text(
            text = provider.info.displayName,
            style = MaterialTheme.typography.titleSmall,
        )
        provider.info.description?.let { description ->
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
        }
        if (providerPreferences.lastSuccessfulBackupAtEpochSeconds != null ||
            providerPreferences.lastSuccessfulRestoreAtEpochSeconds != null
        ) {
            val backupText = providerPreferences.lastSuccessfulBackupAtEpochSeconds
                ?.let { "Last backup: ${timestampDisplayText(it)}" }
            val restoreText = providerPreferences.lastSuccessfulRestoreAtEpochSeconds
                ?.let { "Last restore: ${timestampDisplayText(it)}" }
            Text(
                text = listOfNotNull(backupText, restoreText).joinToString(" • "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        Text(
            text = "Availability: ${backupAvailabilityLabel(resolvedAvailability)}",
            style = MaterialTheme.typography.bodySmall,
            color = if (resolvedAvailability.isAvailable) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.error
            },
            modifier = Modifier.padding(bottom = 8.dp),
        )
        if (provider.info.requiresAuthentication) {
            Text(
                text = resolvedAuthorizationStatus.detail ?: when (resolvedAuthorizationStatus.state) {
                    BackupAuthorizationState.NOT_CONFIGURED -> "Provider authentication needs setup."
                    BackupAuthorizationState.DISCONNECTED -> "Provider is ready to connect."
                    BackupAuthorizationState.CONNECTED -> "Provider is connected."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onConfigureAuthorization,
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        when (resolvedAuthorizationStatus.state) {
                            BackupAuthorizationState.NOT_CONFIGURED -> "Set Client ID"
                            BackupAuthorizationState.DISCONNECTED -> "Update Client ID"
                            BackupAuthorizationState.CONNECTED -> "Change Client ID"
                        }
                    )
                }
                if (resolvedAuthorizationStatus.state == BackupAuthorizationState.CONNECTED) {
                    OutlinedButton(
                        onClick = onDisconnectAuthorization,
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Disconnect")
                    }
                } else {
                    Button(
                        onClick = onBeginAuthorization,
                        enabled = !isLoading &&
                            resolvedAuthorizationStatus.state != BackupAuthorizationState.NOT_CONFIGURED,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Connect")
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onExport,
                modifier = Modifier.weight(1f),
                enabled = provider.info.supportsManualBackup &&
                    resolvedAvailability.isAvailable &&
                    !isLoading &&
                    (!provider.info.requiresAuthentication || resolvedAuthorizationStatus.state == BackupAuthorizationState.CONNECTED),
            ) {
                Text("Export")
            }
            OutlinedButton(
                onClick = onImport,
                modifier = Modifier.weight(1f),
                enabled = provider.info.supportsManualRestore &&
                    resolvedAvailability.isAvailable &&
                    !isLoading &&
                    (!provider.info.requiresAuthentication || resolvedAuthorizationStatus.state == BackupAuthorizationState.CONNECTED),
            ) {
                Text("Import")
            }
        }
        if (provider.info.supportsAutomaticRestore) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Automatic Restore",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = "Keep exactly one provider selected for future restore-on-startup checks. Automatic restore stays disabled until you explicitly choose a supported provider here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                RadioButton(
                    selected = automaticRestoreSelectedProviderId == provider.info.id,
                    onClick = {
                        onAutomaticRestoreChange(automaticRestoreSelectedProviderId != provider.info.id)
                    },
                    enabled = !isLoading,
                )
            }
        }
    }
}

private suspend fun restoreBackup(
    backupService: BackupService?,
    backupPreferencesManager: BackupPreferencesManager?,
    provider: BackupProvider,
    backup: BackupDescriptor,
    companionSyncCoordinator: CompanionSyncCoordinator?,
    snackbarHostState: SnackbarHostState,
) {
    if (backupService == null) {
        snackbarHostState.showSnackbar("Backup service is unavailable")
        return
    }

    val result = backupService.restoreBackup(provider.transport, backup.id)
    val message = when (result) {
        is BackupResult.Success ->
            "Imported ${result.value} account(s) from ${provider.info.displayName}"
        is BackupResult.Failure ->
            "Import failed: ${result.message}"
    }
    if (result is BackupResult.Success) {
        backupPreferencesManager?.recordRestoreSuccess(provider, backup)
        companionSyncCoordinator?.onAccountsChanged()
    }
    snackbarHostState.showSnackbar(message)
}

internal fun timestampDisplayText(epochSeconds: Long): String {
    return if (epochSeconds > 0) {
        "Unix epoch second $epochSeconds"
    } else {
        "unknown time"
    }
}

@Preview
@Composable
private fun SettingsScreenPreview() {
    SettingsScreen()
}
