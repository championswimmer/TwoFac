package tech.arnav.twofac.components.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import tech.arnav.twofac.lib.backup.BackupProvider
import tech.arnav.twofac.theme.TwoFacTheme

@Composable
fun BackupProviderRow(
    provider: BackupProvider,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
    ) {
        Text(
            text = provider.displayName,
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = provider.availabilitySummary(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = onExportClick,
                enabled = !isLoading && provider.isAvailable && provider.supportsManualBackup,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Upload,
                    contentDescription = null,
                )
                Text(
                    text = "Export",
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
            OutlinedButton(
                onClick = onImportClick,
                enabled = !isLoading && provider.isAvailable && provider.supportsManualRestore,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Download,
                    contentDescription = null,
                )
                Text(
                    text = "Import",
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
    }
}

private fun BackupProvider.availabilitySummary(): String = buildString {
    append(if (isAvailable) "Available" else "Unavailable")
    append(" • ")
    append(id)
    if (requiresAuthentication && !isAvailable) {
        append(" • Sign-in required")
    }
}

@Preview
@Composable
private fun BackupProviderRowPreview() {
    TwoFacTheme {
        BackupProviderRow(
            provider = BackupProvider(
                id = "drive",
                displayName = "Google Drive",
                supportsManualBackup = true,
                supportsManualRestore = true,
                supportsAutomaticRestore = false,
                requiresAuthentication = true,
                isAvailable = true,
            ),
            onExportClick = {},
            onImportClick = {},
            isLoading = false,
        )
    }
}
