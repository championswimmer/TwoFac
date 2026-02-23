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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import tech.arnav.twofac.storage.getStoragePath

private enum class BackupAction { EXPORT, IMPORT }

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

    var pendingAction by remember { mutableStateOf<BackupAction?>(null) }
    var passkeyError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

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
