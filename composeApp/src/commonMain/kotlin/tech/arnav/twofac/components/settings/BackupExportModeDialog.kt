package tech.arnav.twofac.components.settings

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import tech.arnav.twofac.theme.TwoFacTheme

@Composable
fun BackupExportModeDialog(
    isVisible: Boolean,
    onPlaintextSelected: () -> Unit,
    onEncryptedSelected: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!isVisible) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Choose Backup Type")
        },
        text = {
            Text(
                "Plaintext backups are easier to restore. Encrypted backups keep account secrets encrypted and require the backup passkey when restoring."
            )
        },
        confirmButton = {
            TextButton(onClick = onEncryptedSelected) {
                Text("Encrypted Backup")
            }
        },
        dismissButton = {
            TextButton(onClick = onPlaintextSelected) {
                Text("Plaintext Backup")
            }
        },
    )
}

@Preview
@Composable
private fun BackupExportModeDialogPreview() {
    TwoFacTheme {
        BackupExportModeDialog(
            isVisible = true,
            onPlaintextSelected = {},
            onEncryptedSelected = {},
            onDismiss = {},
        )
    }
}
