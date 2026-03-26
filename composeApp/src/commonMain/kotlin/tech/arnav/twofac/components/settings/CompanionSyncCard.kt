package tech.arnav.twofac.components.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import twofac.composeapp.generated.resources.*
import tech.arnav.twofac.theme.TwoFacTheme

@Composable
fun CompanionSyncCard(
    companionDisplayName: String,
    isCompanionActive: Boolean,
    isSyncEnabled: Boolean,
    isDiscoveryEnabled: Boolean,
    onSyncClick: () -> Unit,
    onDiscoverClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = stringResource(Res.string.settings_companion_sync_title),
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
                text = if (isCompanionActive) {
                    stringResource(Res.string.settings_companion_active, companionDisplayName)
                } else {
                    stringResource(Res.string.settings_companion_inactive, companionDisplayName)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Button(
                onClick = onSyncClick,
                enabled = isSyncEnabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.settings_companion_sync_button, companionDisplayName))
            }
            OutlinedButton(
                onClick = onDiscoverClick,
                enabled = isDiscoveryEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                Text(stringResource(Res.string.settings_companion_discover))
            }
        }
    }
}

@Preview
@Composable
private fun CompanionSyncCardPreview() {
    TwoFacTheme {
        CompanionSyncCard(
            companionDisplayName = "Watch",
            isCompanionActive = true,
            isSyncEnabled = true,
            isDiscoveryEnabled = true,
            onSyncClick = {},
            onDiscoverClick = {},
        )
    }
}
