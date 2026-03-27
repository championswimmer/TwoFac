package tech.arnav.twofac.components.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import twofac.composeapp.generated.resources.*
import tech.arnav.twofac.theme.TwoFacTheme

@Composable
fun QrImportActions(
    hasCameraImport: Boolean,
    hasClipboardImport: Boolean,
    isLoading: Boolean,
    isScanning: Boolean,
    isPasting: Boolean,
    onScanWithCamera: () -> Unit,
    onPasteFromClipboard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (hasCameraImport || hasClipboardImport) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (hasCameraImport) {
                Button(
                    onClick = onScanWithCamera,
                    enabled = !isLoading && !isScanning && !isPasting,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.PhotoCamera, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isScanning) stringResource(Res.string.add_account_scanning) else stringResource(Res.string.add_account_scan_qr))
                }
            }

            if (hasClipboardImport) {
                Button(
                    onClick = onPasteFromClipboard,
                    enabled = !isLoading && !isScanning && !isPasting,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.ContentPaste, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isPasting) stringResource(Res.string.add_account_pasting) else stringResource(Res.string.add_account_paste_qr))
                }
            }
        }
    }
}

@Preview
@Composable
private fun QrImportActionsPreview() {
    TwoFacTheme {
        QrImportActions(
            hasCameraImport = true,
            hasClipboardImport = true,
            isLoading = false,
            isScanning = false,
            isPasting = false,
            onScanWithCamera = {},
            onPasteFromClipboard = {},
        )
    }
}

@Preview
@Composable
private fun QrImportActionsBusyPreview() {
    TwoFacTheme {
        QrImportActions(
            hasCameraImport = true,
            hasClipboardImport = true,
            isLoading = false,
            isScanning = true,
            isPasting = false,
            onScanWithCamera = {},
            onPasteFromClipboard = {},
        )
    }
}
