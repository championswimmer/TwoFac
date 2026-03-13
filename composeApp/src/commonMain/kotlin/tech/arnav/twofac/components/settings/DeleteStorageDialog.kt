package tech.arnav.twofac.components.settings

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import tech.arnav.twofac.theme.TwoFacTheme

@Composable
fun DeleteStorageDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    isDeleteInProgress: Boolean,
    title: String = "Delete all accounts?",
    message: String = "This deletes all existing accounts and cannot be undone unless you have a backup.\n\nA fresh storage file will be created on next run/use.",
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isDeleteInProgress,
            ) {
                Text(if (isDeleteInProgress) "Deleting..." else "Delete")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest,
                enabled = !isDeleteInProgress,
            ) {
                Text("Cancel")
            }
        },
    )
}

@Preview
@Composable
private fun DeleteStorageDialogPreview() {
    TwoFacTheme {
        DeleteStorageDialog(
            onDismissRequest = {},
            onConfirm = {},
            isDeleteInProgress = false,
        )
    }
}
