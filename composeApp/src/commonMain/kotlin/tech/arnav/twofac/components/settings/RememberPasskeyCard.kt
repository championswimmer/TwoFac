package tech.arnav.twofac.components.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import twofac.composeapp.generated.resources.*
import tech.arnav.twofac.theme.TwoFacTheme

@Composable
fun RememberPasskeyCard(
    title: String,
    description: String,
    isRememberPasskeyEnabled: Boolean,
    onRememberPasskeyChanged: (Boolean) -> Unit,
    showBiometricToggle: Boolean,
    isBiometricEnabled: Boolean,
    onBiometricChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    biometricTitle: String = stringResource(Res.string.settings_biometric_title),
    biometricDescription: String = stringResource(Res.string.settings_biometric_description),
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                )
                Switch(
                    checked = isRememberPasskeyEnabled,
                    onCheckedChange = onRememberPasskeyChanged,
                )
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )

            if (showBiometricToggle) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                        Text(
                            text = biometricTitle,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = biometricDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = isBiometricEnabled,
                        onCheckedChange = onBiometricChanged,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun RememberPasskeyCardPreview() {
    TwoFacTheme {
        RememberPasskeyCard(
            title = "Remember Passkey",
            description = "Keep the passkey saved so you don't have to enter it every time the extension is opened. Only enable this on devices you trust.",
            isRememberPasskeyEnabled = true,
            onRememberPasskeyChanged = {},
            showBiometricToggle = true,
            isBiometricEnabled = false,
            onBiometricChanged = {},
        )
    }
}
