package tech.arnav.twofac.components.otp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.arnav.twofac.lib.otp.OtpCodes
import twofac.composeapp.generated.resources.*
import tech.arnav.twofac.lib.storage.StoredAccount
import tech.arnav.twofac.theme.TwoFacTheme

@Composable
fun HomeOtpListSection(
    accountsWithOtps: List<Pair<StoredAccount.DisplayAccount, OtpCodes>>,
    listState: LazyListState,
    onCopyOtp: (String) -> Unit = {},
    heading: String = stringResource(Res.string.home_otp_heading),
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = heading,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        items(accountsWithOtps, key = { (account, _) -> account.accountID }) { (account, otpCode) ->
            OTPCard(
                account = account,
                otpCode = otpCode.currentOTP,
                nextOtp = otpCode.nextOTP,
                timeInterval = 30L,
                onCopyOtp = onCopyOtp,
            )
        }
    }
}

@Preview
@Composable
fun HomeOtpListSectionPreview() {
    TwoFacTheme {
        HomeOtpListSection(
            accountsWithOtps = previewAccountsWithOtps,
            listState = rememberLazyListState(),
        )
    }
}

private val previewAccountsWithOtps = listOf(
    StoredAccount.DisplayAccount(
        accountID = "google",
        accountLabel = "arnav@gmail.com",
        issuer = "Google",
    ) to OtpCodes(currentOTP = "123456"),
    StoredAccount.DisplayAccount(
        accountID = "github",
        accountLabel = "championswimmer",
        issuer = "GitHub",
    ) to OtpCodes(currentOTP = "654321"),
    StoredAccount.DisplayAccount(
        accountID = "unknown",
        accountLabel = "team@example.com",
    ) to OtpCodes(currentOTP = "987654", nextOTP = "0004568")
)
