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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import tech.arnav.twofac.companion.CompanionSyncCoordinator
import tech.arnav.twofac.companion.isSyncToCompanionEnabled
import tech.arnav.twofac.components.PasskeyDialog
import tech.arnav.twofac.lib.TwoFacLib
import tech.arnav.twofac.lib.backup.BackupResult
import tech.arnav.twofac.lib.backup.BackupService
import tech.arnav.twofac.lib.backup.BackupTransportRegistry
import tech.arnav.twofac.session.BiometricSessionManager
import tech.arnav.twofac.session.SessionManager
import tech.arnav.twofac.session.WebAuthnSessionManager
import tech.arnav.twofac.storage.getStoragePath

private enum class BackupAction { EXPORT, IMPORT, SYNC_COMPANION }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: (() -> Unit)? = null,
    onQuit: (() -> Unit)? = null
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Optional backup dependencies – registered on platforms that support backup
    val koin = getKoin()
    val backupService = remember { koin.getOrNull<BackupService>() }
    val backupRegistry = remember { koin.getOrNull<BackupTransportRegistry>() }
    val twoFacLib = remember { koin.getOrNull<TwoFacLib>() }
    val companionSyncCoordinator = remember { koin.getOrNull<CompanionSyncCoordinator>() }
    val sessionManager = remember { koin.getOrNull<SessionManager>() }

    var pendingAction by remember { mutableStateOf<BackupAction?>(null) }
    var pendingProviderId by remember { mutableStateOf<String?>(null) }
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

            if (backupService != null && backupRegistry != null && !backupRegistry.isEmpty()) {
                for (provider in backupRegistry.providers()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = provider.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = "Export or import accounts as a plaintext JSON backup file.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (provider.supportsManualBackup) {
                                    Button(
                                        onClick = {
                                            pendingProviderId = provider.id
                                            pendingAction = BackupAction.EXPORT
                                        },
                                        modifier = Modifier.weight(1f),
                                        enabled = !isLoading
                                    ) {
                                        Text("Export")
                                    }
                                }
                                if (provider.supportsManualRestore) {
                                    OutlinedButton(
                                        onClick = {
                                            pendingProviderId = provider.id
                                            pendingAction = BackupAction.IMPORT
                                        },
                                        modifier = Modifier.weight(1f),
                                        enabled = !isLoading
                                    ) {
                                        Text("Import")
                                    }
                                }
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
                                    pendingAction = BackupAction.SYNC_COMPANION
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
                passkeyError = null
                isLoading = true
                coroutineScope.launch {
                    try {
                        twoFacLib?.unlock(passkey)
                        // Resolve the selected backup transport before dispatching
                        val backupTransport = if (action == BackupAction.EXPORT || action == BackupAction.IMPORT) {
                            val transport = pendingProviderId?.let { backupRegistry?.transport(it) }
                            if (transport == null) {
                                snackbarHostState.showSnackbar("Backup provider '${pendingProviderId ?: "unknown"}' is not available")
                                pendingAction = null
                                pendingProviderId = null
                                return@launch
                            }
                            if (backupService == null) {
                                snackbarHostState.showSnackbar("Backup service is not configured")
                                pendingAction = null
                                pendingProviderId = null
                                return@launch
                            }
                            transport
                        } else null
                        when (action) {
                            BackupAction.EXPORT -> {
                                val result = backupService!!.createBackup(backupTransport!!)
                                val message = when (result) {
                                    is BackupResult.Success ->
                                        "Backup exported: ${result.value.id}"
                                    is BackupResult.Failure ->
                                        "Export failed: ${result.message}"
                                }
                                snackbarHostState.showSnackbar(message)
                                pendingAction = null
                                pendingProviderId = null
                            }
                            BackupAction.IMPORT -> {
                                val listResult = backupTransport!!.listBackups()
                                if (listResult is BackupResult.Failure) {
                                    snackbarHostState.showSnackbar("No backups found: ${listResult.message}")
                                    pendingAction = null
                                    pendingProviderId = null
                                    return@launch
                                }
                                val backups = (listResult as BackupResult.Success).value
                                if (backups.isEmpty()) {
                                    snackbarHostState.showSnackbar("No backup files found")
                                    pendingAction = null
                                    pendingProviderId = null
                                    return@launch
                                }
                                val latest = backups.maxBy { it.createdAt }
                                val result = backupService!!.restoreBackup(backupTransport, latest.id)
                                val message = when (result) {
                                    is BackupResult.Success ->
                                        "Imported ${result.value} account(s) from ${latest.id}"
                                    is BackupResult.Failure ->
                                        "Import failed: ${result.message}"
                                }
                                snackbarHostState.showSnackbar(message)
                                if (result is BackupResult.Success) {
                                    companionSyncCoordinator?.onAccountsChanged()
                                }
                                pendingAction = null
                                pendingProviderId = null
                            }

                            BackupAction.SYNC_COMPANION -> {
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
                pendingProviderId = null
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
}

@Preview
@Composable
private fun SettingsScreenPreview() {
    SettingsScreen()
}
