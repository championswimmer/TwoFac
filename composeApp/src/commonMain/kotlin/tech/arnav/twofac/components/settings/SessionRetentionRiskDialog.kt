package tech.arnav.twofac.components.settings

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.stringResource
import tech.arnav.twofac.theme.TwoFacTheme
import twofac.composeapp.generated.resources.Res
import twofac.composeapp.generated.resources.action_cancel
import twofac.composeapp.generated.resources.settings_session_retention_browser_warning_confirm
import twofac.composeapp.generated.resources.settings_session_retention_browser_warning_message
import twofac.composeapp.generated.resources.settings_session_retention_browser_warning_title

@Composable
fun SessionRetentionRiskDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(Res.string.settings_session_retention_browser_warning_title)) },
        text = { Text(stringResource(Res.string.settings_session_retention_browser_warning_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(Res.string.settings_session_retention_browser_warning_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(Res.string.action_cancel))
            }
        },
    )
}

@Preview
@Composable
private fun SessionRetentionRiskDialogPreview() {
    TwoFacTheme {
        SessionRetentionRiskDialog(
            onDismissRequest = {},
            onConfirm = {},
        )
    }
}
