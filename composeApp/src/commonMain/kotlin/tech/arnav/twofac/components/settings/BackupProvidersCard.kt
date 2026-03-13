package tech.arnav.twofac.components.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import tech.arnav.twofac.lib.backup.BackupProvider
import tech.arnav.twofac.theme.TwoFacTheme

@Composable
fun BackupProvidersCard(
    providers: List<BackupProvider>,
    isLoading: Boolean,
    onExportClick: (BackupProvider) -> Unit,
    onImportClick: (BackupProvider) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Backups",
    description: String = "Create or restore account snapshots from available backup providers.",
    emptyStateMessage: String = "No backup providers are currently available on this platform.",
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            if (providers.isEmpty()) {
                Text(
                    text = emptyStateMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                providers.forEach { provider ->
                    BackupProviderRow(
                        provider = provider,
                        onExportClick = { onExportClick(provider) },
                        onImportClick = { onImportClick(provider) },
                        isLoading = isLoading,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun BackupProvidersCardPreview() {
    val providers = listOf(
        BackupProvider(
            id = "drive",
            displayName = "Google Drive",
            supportsManualBackup = true,
            supportsManualRestore = true,
            supportsAutomaticRestore = false,
            requiresAuthentication = true,
            isAvailable = true,
        ),
        BackupProvider(
            id = "icloud",
            displayName = "iCloud",
            supportsManualBackup = true,
            supportsManualRestore = false,
            supportsAutomaticRestore = true,
            requiresAuthentication = false,
            isAvailable = true,
        ),
    )

    TwoFacTheme {
        BackupProvidersCard(
            providers = providers,
            isLoading = false,
            onExportClick = {},
            onImportClick = {},
        )
    }
}
