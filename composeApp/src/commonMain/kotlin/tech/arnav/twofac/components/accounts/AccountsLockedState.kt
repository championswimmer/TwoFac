package tech.arnav.twofac.components.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import tech.arnav.twofac.theme.TwoFacTheme

@Composable
fun AccountsLockedState(
    onUnlockClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Vault is locked. Unlock to manage accounts.",
            modifier = Modifier.padding(16.dp),
        )
        Button(onClick = onUnlockClick) {
            Text("Unlock Vault")
        }
    }
}

@Preview
@Composable
private fun AccountsLockedStatePreview() {
    TwoFacTheme {
        AccountsLockedState(
            onUnlockClick = {},
        )
    }
}
