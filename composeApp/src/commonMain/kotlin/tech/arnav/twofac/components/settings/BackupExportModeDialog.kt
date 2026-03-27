package tech.arnav.twofac.components.settings

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.stringResource
import twofac.composeapp.generated.resources.*
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
            Text(stringResource(Res.string.backup_export_dialog_title))
        },
        text = {
            Text(stringResource(Res.string.backup_export_dialog_body))
        },
        confirmButton = {
            TextButton(onClick = onEncryptedSelected) {
                Text(stringResource(Res.string.backup_export_encrypted))
            }
        },
        dismissButton = {
            TextButton(onClick = onPlaintextSelected) {
                Text(stringResource(Res.string.backup_export_plaintext))
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
