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
import tech.arnav.twofac.lib.storage.StoredTag
import tech.arnav.twofac.lib.storage.TagColor
import tech.arnav.twofac.theme.TwoFacTheme

@Composable
fun AccountsListContent(
    accounts: List<StoredAccount.DisplayAccount>,
    onAccountClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    searchQuery: String = "",
    selectedTagId: String? = null,
) {
    val filtered = accounts.filter { account ->
        val matchesSearch = searchQuery.isBlank() ||
            account.accountLabel.contains(searchQuery, ignoreCase = true) ||
            account.issuer?.contains(searchQuery, ignoreCase = true) == true
        val matchesTag = selectedTagId == null ||
            account.tags.any { it.tagId == selectedTagId }
        matchesSearch && matchesTag
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(filtered) { account ->
            AccountListItem(
                accountLabel = account.accountLabel,
                issuer = account.issuer,
                tags = account.tags,
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
                    accountLabel = "arnav@example.com",
                    issuer = "GitHub",
                    tags = listOf(StoredTag("1", "Work", TagColor.BLUE)),
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
