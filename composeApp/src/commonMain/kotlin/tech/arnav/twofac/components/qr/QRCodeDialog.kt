package tech.arnav.twofac.components.qr

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import org.jetbrains.compose.resources.stringResource
import twofac.composeapp.generated.resources.Res
import twofac.composeapp.generated.resources.action_close
import twofac.composeapp.generated.resources.account_detail_show_qr_dialog_title
import twofac.composeapp.generated.resources.account_detail_show_qr_warning

@Composable
fun QRCodeDialog(
    otpAuthUri: String,
    onDismiss: () -> Unit,
) {
    QrCodeScreenshotProtectionEffect(enabled = true)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.account_detail_show_qr_dialog_title)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = rememberQrCodePainter(otpAuthUri),
                    contentDescription = stringResource(Res.string.account_detail_show_qr_dialog_title),
                    modifier = Modifier.size(240.dp),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Icon(
                    imageVector = Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                Text(
                    text = stringResource(Res.string.account_detail_show_qr_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_close))
            }
        },
    )
}
