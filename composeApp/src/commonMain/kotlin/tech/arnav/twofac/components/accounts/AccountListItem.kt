package tech.arnav.twofac.components.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import tech.arnav.twofac.components.icons.IssuerBrandIcon
import tech.arnav.twofac.theme.TwoFacTheme

@Composable
fun AccountListItem(
    accountLabel: String,
    issuer: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IssuerBrandIcon(
                issuer = issuer,
                size = 28.dp,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = accountLabel,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Preview
@Composable
private fun AccountListItemPreview() {
    TwoFacTheme {
        AccountListItem(
            accountLabel = "arnav@example.com",
            issuer = "GitHub",
            onClick = {},
        )
    }
}
