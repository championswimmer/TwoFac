package tech.arnav.twofac.components.settings

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.stringResource
import twofac.composeapp.generated.resources.*
import tech.arnav.twofac.theme.TwoFacTheme

@Composable
fun DeleteStorageDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    isDeleteInProgress: Boolean,
    title: String = stringResource(Res.string.settings_delete_dialog_title),
    message: String = stringResource(Res.string.settings_delete_dialog_message),
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
                Text(if (isDeleteInProgress) stringResource(Res.string.settings_delete_dialog_deleting) else stringResource(Res.string.action_delete))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest,
                enabled = !isDeleteInProgress,
            ) {
                Text(stringResource(Res.string.action_cancel))
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
