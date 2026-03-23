package tech.arnav.twofac.components.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import tech.arnav.twofac.theme.TwoFacTheme

@Composable
fun HomeLockedState(
    title: String = "TwoFac",
    subtitle: String = "Two-Factor Authentication Manager",
    onSecureUnlock: (() -> Unit)? = null,
    onManualUnlock: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
        )
        if (onSecureUnlock != null) {
            Spacer(Modifier.height(8.dp))
            Button(onClick = onSecureUnlock) {
                Icon(
                    imageVector = Icons.Rounded.Fingerprint,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Text("Unlock with Passkey")
            }
            if (onManualUnlock != null) {
                TextButton(onClick = onManualUnlock) {
                    Text("Enter passkey manually")
                }
            }
        }
    }
}

@Preview
@Composable
fun HomeLockedStatePreview() {
    TwoFacTheme {
        HomeLockedState()
    }
}

@Preview
@Composable
fun HomeLockedStateWithWebAuthnPreview() {
    TwoFacTheme {
        HomeLockedState(
            onSecureUnlock = {},
            onManualUnlock = {},
        )
    }
}
