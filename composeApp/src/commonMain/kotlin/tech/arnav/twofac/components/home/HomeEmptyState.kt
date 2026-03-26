package tech.arnav.twofac.components.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import tech.arnav.twofac.theme.TwoFacTheme
import twofac.composeapp.generated.resources.*

@Composable
fun HomeEmptyState(
    onManageAccounts: () -> Unit,
    onOpenGettingStarted: (() -> Unit)? = null,
    title: String = stringResource(Res.string.app_name),
    emptyTitle: String = stringResource(Res.string.home_empty_title),
    emptySubtitle: String = stringResource(Res.string.home_empty_subtitle),
    manageAccountsLabel: String = stringResource(Res.string.home_empty_manage_accounts),
    gettingStartedLabel: String = stringResource(Res.string.home_empty_getting_started),
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
    ) {
        Image(
            painter = painterResource(Res.drawable.app_logo),
            contentDescription = title,
            modifier = Modifier.size(120.dp),
        )
        Text(
            text = emptyTitle,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = emptySubtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = onManageAccounts,
            modifier = Modifier.padding(top = 16.dp),
        ) {
            Text(manageAccountsLabel)
        }
        onOpenGettingStarted?.let { openGuide ->
            Button(
                onClick = openGuide,
            ) {
                Text(gettingStartedLabel)
            }
        }
    }
}

@Preview
@Composable
fun HomeEmptyStatePreview() {
    TwoFacTheme {
        HomeEmptyState(onManageAccounts = {})
    }
}
