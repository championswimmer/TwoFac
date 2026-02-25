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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.getKoin
import tech.arnav.twofac.components.PasskeyDialog
import tech.arnav.twofac.lib.TwoFacLib
import tech.arnav.twofac.lib.backup.BackupResult
import tech.arnav.twofac.lib.backup.BackupService
import tech.arnav.twofac.lib.backup.BackupTransport
import tech.arnav.twofac.session.SessionManager
import tech.arnav.twofac.storage.getStoragePath
import tech.arnav.twofac.wear.WatchSyncCoordinator
import tech.arnav.twofac.wear.isSyncToWatchEnabled

private enum class BackupAction { EXPORT, IMPORT, SYNC_WATCH }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Optional backup dependencies – only registered on platforms that support local file backup
    val koin = getKoin()
    val backupService = remember { koin.getOrNull<BackupService>() }
    val backupTransport = remember { koin.getOrNull<BackupTransport>() }
    val twoFacLib = remember { koin.getOrNull<TwoFacLib>() }
    val watchSyncCoordinator = remember { koin.getOrNull<WatchSyncCoordinator>() }
    val sessionManager = remember { koin.getOrNull<SessionManager>() }

    var pendingAction by remember { mutableStateOf<BackupAction?>(null) }
    var passkeyError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isWatchCompanionActive by remember { mutableStateOf(false) }
    var isWatchSyncInProgress by remember { mutableStateOf(false) }
    var isWatchDiscoveryInProgress by remember { mutableStateOf(false) }
    var isRememberPasskeyEnabled by remember {
        mutableStateOf(sessionManager?.isRememberPasskeyEnabled() ?: false)
    }

    LaunchedEffect(watchSyncCoordinator) {
        if (watchSyncCoordinator != null) {
            isWatchCompanionActive = watchSyncCoordinator.isCompanionActive()
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
                                }
                            )
                        }
                        Text(
                            text = "Keep the passkey saved so you don't have to enter it every time the extension is opened. Only enable this on devices you trust.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
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

            if (watchSyncCoordinator != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Watch Sync",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = if (isWatchCompanionActive) {
                                "Watch companion is active."
                            } else {
                                "Watch companion is not active."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Button(
                            onClick = {
                                if (twoFacLib != null && !twoFacLib.isUnlocked()) {
                                    pendingAction = BackupAction.SYNC_WATCH
                                    return@Button
                                }
                                coroutineScope.launch {
                                    try {
                                        isWatchSyncInProgress = true
                                        val synced = watchSyncCoordinator.syncNow(manual = true)
                                        if (synced) {
                                            snackbarHostState.showSnackbar("Sync sent to watch")
                                        } else {
                                            snackbarHostState.showSnackbar("Unable to sync to watch right now")
                                        }
                                        isWatchCompanionActive = watchSyncCoordinator.isCompanionActive()
                                    } finally {
                                        isWatchSyncInProgress = false
                                    }
                                }
                            },
                            enabled = isSyncToWatchEnabled(
                                isCompanionActive = isWatchCompanionActive,
                                isSyncInProgress = isWatchSyncInProgress,
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Sync to Watch")
                        }
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    try {
                                        isWatchDiscoveryInProgress = true
                                        isWatchCompanionActive =
                                            watchSyncCoordinator.forceDiscoverCompanion()
                                        val message = if (isWatchCompanionActive) {
                                            "Watch app discovered"
                                        } else {
                                            "Unable to discover watch app"
                                        }
                                        snackbarHostState.showSnackbar(message)
                                    } finally {
                                        isWatchDiscoveryInProgress = false
                                    }
                                }
                            },
                            enabled = !isWatchSyncInProgress && !isWatchDiscoveryInProgress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Text("Force Discover Watch App")
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
                                val message = when (result) {
                                    is BackupResult.Success ->
                                        "Backup exported: ${result.value.id}"
                                    is BackupResult.Failure ->
                                        "Export failed: ${result.message}"
                                }
                                snackbarHostState.showSnackbar(message)
                                pendingAction = null
                            }
                            BackupAction.IMPORT -> {
                                val listResult = backupTransport!!.listBackups()
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
                                    watchSyncCoordinator?.onAccountsChanged()
                                }
                                pendingAction = null
                            }

                            BackupAction.SYNC_WATCH -> {
                                if (watchSyncCoordinator == null || twoFacLib == null) {
                                    snackbarHostState.showSnackbar("Watch sync is unavailable")
                                    pendingAction = null
                                    return@launch
                                }
                                val hasAccounts = twoFacLib.getAllAccounts().isNotEmpty()
                                if (!hasAccounts) {
                                    snackbarHostState.showSnackbar("No accounts to sync to watch")
                                    pendingAction = null
                                    return@launch
                                }
                                try {
                                    isWatchSyncInProgress = true
                                    val synced = watchSyncCoordinator.syncNow(manual = true)
                                    val message = if (synced) {
                                        "Sync sent to watch"
                                    } else {
                                        "Unable to sync to watch right now"
                                    }
                                    snackbarHostState.showSnackbar(message)
                                    isWatchCompanionActive =
                                        watchSyncCoordinator.isCompanionActive()
                                } finally {
                                    isWatchSyncInProgress = false
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
}

@Preview
@Composable
private fun SettingsScreenPreview() {
    SettingsScreen(onNavigateBack = {})
}
