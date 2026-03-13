package tech.arnav.twofac.components.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import tech.arnav.twofac.lib.storage.StoredAccount
import tech.arnav.twofac.theme.TwoFacTheme

@Composable
fun AccountsListContent(
    accounts: List<StoredAccount.DisplayAccount>,
    onAccountClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(accounts) { account ->
            AccountListItem(
                accountLabel = account.accountLabel,
                onClick = { onAccountClick(account.accountID) },
            )
        }
    }
}

@Preview
@Composable
private fun AccountsListContentPreview() {
    TwoFacTheme {
        AccountsListContent(
            accounts = listOf(
                StoredAccount.DisplayAccount(
                    accountID = "github-id",
                    accountLabel = "GitHub",
                ),
                StoredAccount.DisplayAccount(
                    accountID = "google-id",
                    accountLabel = "Google",
                ),
            ),
            onAccountClick = {},
        )
    }
}
