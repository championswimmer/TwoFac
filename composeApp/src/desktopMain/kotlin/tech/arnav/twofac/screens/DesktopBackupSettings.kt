package tech.arnav.twofac.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import tech.arnav.twofac.lib.backup.BackupResult
import tech.arnav.twofac.lib.backup.BackupService
import tech.arnav.twofac.lib.backup.LocalBackupTransport
import tech.arnav.twofac.storage.getBackupDir

private const val DEFAULT_BACKUP_FILE_NAME = "twofac-backup.json"

@Composable
fun DesktopBackupSettings(
    backupService: BackupService = koinInject(),
    transport: LocalBackupTransport = koinInject(),
) {
    val scope = rememberCoroutineScope()
    var passkey by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf(DEFAULT_BACKUP_FILE_NAME) }
    var status by remember { mutableStateOf<String?>(null) }
    val backupDir = remember { getBackupDir(forceCreate = true).toString() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Local Backup",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Backups are saved in $backupDir",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = fileName,
                onValueChange = { fileName = it },
                label = { Text("Backup file name") },
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = passkey,
                onValueChange = { passkey = it },
                label = { Text("Passkey") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            val result = backupService.createPlaintextBackup(
                                passkey = passkey,
                                transport = transport,
                                fileName = fileName.ifBlank { DEFAULT_BACKUP_FILE_NAME }
                            )
                            status = when (result) {
                                is BackupResult.Success -> "Backup saved to $backupDir/$fileName"
                                is BackupResult.Failure -> "Backup failed: ${result.error.message}"
                            }
                        }
                    }
                ) {
                    Text("Export backup")
                }

                TextButton(
                    onClick = {
                        scope.launch {
                            val result = backupService.restorePlaintextBackup(
                                passkey = passkey,
                                transport = transport,
                                backupId = fileName.ifBlank { DEFAULT_BACKUP_FILE_NAME }
                            )
                            status = when (result) {
                                is BackupResult.Success -> "Imported ${result.value.imported} accounts (failed: ${result.value.failed})"
                                is BackupResult.Failure -> "Import failed: ${result.error.message}"
                            }
                        }
                    }
                ) {
                    Text("Import backup")
                }
            }

            status?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
