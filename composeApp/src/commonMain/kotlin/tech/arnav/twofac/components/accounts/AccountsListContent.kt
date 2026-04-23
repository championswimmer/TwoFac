package tech.arnav.twofac.components.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.arnav.twofac.lib.storage.StoredAccount
import tech.arnav.twofac.theme.TwoFacTheme
import twofac.composeapp.generated.resources.Res
import twofac.composeapp.generated.resources.accounts_search_no_results

@Composable
fun AccountsListContent(
    accounts: List<StoredAccount.DisplayAccount>,
    onAccountClick: (String) -> Unit,
    searchQuery: String = "",
    modifier: Modifier = Modifier,
) {
    val query = searchQuery.trim()
    val filteredAccounts = remember(accounts, query) {
        if (query.isBlank()) {
            accounts
        } else {
            accounts.filter { account ->
                account.accountLabel.contains(query, ignoreCase = true) ||
                    (account.issuer?.contains(query, ignoreCase = true) == true)
            }
        }
    }

    if (filteredAccounts.isEmpty() && query.isNotBlank()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(Res.string.accounts_search_no_results),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(filteredAccounts) { account ->
                AccountListItem(
                    accountLabel = account.accountLabel,
                    issuer = account.issuer,
                    onClick = { onAccountClick(account.accountID) },
                )
            }
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
                    accountLabel = "arnav@example.com",
                    issuer = "GitHub",
                ),
                StoredAccount.DisplayAccount(
                    accountID = "unknown-id",
                    accountLabel = "team@example.com",
                ),
            ),
            onAccountClick = {},
        )
    }
}

@Preview
@Composable
private fun AccountsListContentSearchPreview() {
    TwoFacTheme {
        AccountsListContent(
            accounts = listOf(
                StoredAccount.DisplayAccount(
                    accountID = "github-id",
                    accountLabel = "arnav@example.com",
                    issuer = "GitHub",
                ),
                StoredAccount.DisplayAccount(
                    accountID = "unknown-id",
                    accountLabel = "team@example.com",
                ),
            ),
            onAccountClick = {},
            searchQuery = "github",
        )
    }
}

@Preview
@Composable
private fun AccountsListContentEmptySearchPreview() {
    TwoFacTheme {
        AccountsListContent(
            accounts = listOf(
                StoredAccount.DisplayAccount(
                    accountID = "github-id",
                    accountLabel = "arnav@example.com",
                    issuer = "GitHub",
                ),
            ),
            onAccountClick = {},
            searchQuery = "twitter",
        )
    }
}
