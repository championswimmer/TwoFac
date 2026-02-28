package tech.arnav.twofac.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import com.ocnyang.compose_toast.Toast
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.getKoin
import tech.arnav.twofac.components.PasskeyDialog
import tech.arnav.twofac.lib.TwoFacLib
import tech.arnav.twofac.lib.backup.BackupResult
import tech.arnav.twofac.lib.backup.BackupService
import tech.arnav.twofac.lib.backup.BackupTransport
import tech.arnav.twofac.session.BiometricSessionManager
import tech.arnav.twofac.session.SessionManager
import tech.arnav.twofac.storage.getStoragePath
import tech.arnav.twofac.companion.CompanionSyncCoordinator
import tech.arnav.twofac.companion.isSyncToCompanionEnabled

private enum class BackupAction { EXPORT, IMPORT, SYNC_COMPANION }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    // Optional backup dependencies – only registered on platforms that support local file backup
    val koin = getKoin()
    val backupService = remember { koin.getOrNull<BackupService>() }
    val backupTransport = remember { koin.getOrNull<BackupTransport>() }
    val twoFacLib = remember { koin.getOrNull<TwoFacLib>() }
    val companionSyncCoordinator = remember { koin.getOrNull<CompanionSyncCoordinator>() }
    val sessionManager = remember { koin.getOrNull<SessionManager>() }

    var pendingAction by remember { mutableStateOf<BackupAction?>(null) }
    var passkeyError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
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
    var isBiometricEnabled by remember {
        mutableStateOf(biometricSessionManager?.isBiometricEnabled() ?: false)
    }
    var showBiometricEnrollmentDialog by remember { mutableStateOf(false) }
    var biometricEnrollmentError by remember { mutableStateOf<String?>(null) }

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
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
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
                    Text(
                        text = "Storage Location",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
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
                                text = "Remember Passkey",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Switch(
                                checked = isRememberPasskeyEnabled,
                                onCheckedChange = { enabled ->
                                    sessionManager.setRememberPasskey(enabled)
                                    isRememberPasskeyEnabled = enabled
                                    if (!enabled) {
                                        biometricSessionManager?.setBiometricEnabled(false)
                                        isBiometricEnabled = false
                                    }
                                }
                            )
                        }
                        Text(
                            text = "Keep the passkey saved so you don't have to enter it every time the extension is opened. Only enable this on devices you trust.",
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

            if (backupService != null && backupTransport != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Local Backup",
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
                            Button(
                                onClick = { pendingAction = BackupAction.EXPORT },
                                modifier = Modifier.weight(1f),
                                enabled = !isLoading
                            ) {
                                Text("Export")
                            }
                            OutlinedButton(
                                onClick = { pendingAction = BackupAction.IMPORT },
                                modifier = Modifier.weight(1f),
                                enabled = !isLoading
                            ) {
                                Text("Import")
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
                                        val synced = companionSyncCoordinator.syncNow(manual = true)
                                        if (synced) {
                                            Toast.showSuccess("Sync sent to $companionDisplayName")
                                        } else {
                                            Toast.showWarning("Unable to sync to $companionDisplayName right now")
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
                                        if (isCompanionActive) Toast.showSuccess(message) else Toast.showWarning(message)
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
        }
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
                        when (action) {
                            BackupAction.EXPORT -> {
                                val result = backupService!!.createBackup(backupTransport!!)
                                when (result) {
                                    is BackupResult.Success ->
                                        Toast.showSuccess("Backup exported: ${result.value.id}")
                                    is BackupResult.Failure ->
                                        Toast.showError("Export failed: ${result.message}")
                                }
                                pendingAction = null
                            }
                            BackupAction.IMPORT -> {
                                val listResult = backupTransport!!.listBackups()
                                if (listResult is BackupResult.Failure) {
                                    Toast.showError("No backups found: ${listResult.message}")
                                    pendingAction = null
                                    return@launch
                                }
                                val backups = (listResult as BackupResult.Success).value
                                if (backups.isEmpty()) {
                                    Toast.showWarning("No backup files found")
                                    pendingAction = null
                                    return@launch
                                }
                                val latest = backups.maxBy { it.createdAt }
                                val result = backupService!!.restoreBackup(backupTransport, latest.id)
                                when (result) {
                                    is BackupResult.Success ->
                                        Toast.showSuccess("Imported ${result.value} account(s) from ${latest.id}")
                                    is BackupResult.Failure ->
                                        Toast.showError("Import failed: ${result.message}")
                                }
                                if (result is BackupResult.Success) {
                                    companionSyncCoordinator?.onAccountsChanged()
                                }
                                pendingAction = null
                            }

                            BackupAction.SYNC_COMPANION -> {
                                if (companionSyncCoordinator == null || twoFacLib == null) {
                                    Toast.showError("Companion sync is unavailable")
                                    pendingAction = null
                                    return@launch
                                }
                                val hasAccounts = twoFacLib.getAllAccounts().isNotEmpty()
                                if (!hasAccounts) {
                                    Toast.showWarning("No accounts to sync to $companionDisplayName")
                                    pendingAction = null
                                    return@launch
                                }
                                try {
                                    isCompanionSyncInProgress = true
                                    val synced = companionSyncCoordinator.syncNow(manual = true)
                                    if (synced) {
                                        Toast.showSuccess("Sync sent to $companionDisplayName")
                                    } else {
                                        Toast.showWarning("Unable to sync to $companionDisplayName right now")
                                    }
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
                            Toast.showSuccess("Biometric unlock enabled")
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
    SettingsScreen(onNavigateBack = {})
}
