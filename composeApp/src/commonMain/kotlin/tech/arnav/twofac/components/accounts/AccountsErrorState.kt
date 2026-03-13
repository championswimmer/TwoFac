package tech.arnav.twofac.components.accounts

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import tech.arnav.twofac.theme.TwoFacTheme

@Composable
fun AccountsErrorState(
    error: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = "Error: $error",
        color = MaterialTheme.colorScheme.error,
        modifier = modifier.padding(16.dp),
    )
}

@Preview
@Composable
private fun AccountsErrorStatePreview() {
    TwoFacTheme {
        AccountsErrorState(
            error = "Could not load accounts",
        )
    }
}
