package tech.arnav.twofac.wear

import tech.arnav.twofac.companion.CompanionSyncSourceAccount
import tech.arnav.twofac.companion.buildCompanionSyncSnapshot
import tech.arnav.twofac.lib.theme.AccountColorTag
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WatchSyncCoordinatorTest {
    @Test
    fun testBuildWatchSyncSnapshotMapsIssuerWhenUriValid() {
        val snapshot = buildCompanionSyncSnapshot(
            sourceAccounts = listOf(
                CompanionSyncSourceAccount(
                    accountId = "acc-1",
                    accountLabel = "user@example.com",
                    otpAuthUri = "otpauth://totp/GitHub:user@example.com?secret=JBSWY3DPEHPK3PXP&issuer=GitHub",
                    color = AccountColorTag.TEAL,
                ),
            ),
            generatedAtEpochSec = 1700000000L,
        )

        assertEquals(1700000000L, snapshot.generatedAtEpochSec)
        assertEquals("GitHub", snapshot.accounts.single().issuer)
        assertEquals(AccountColorTag.TEAL, snapshot.accounts.single().color)
    }

    @Test
    fun testBuildWatchSyncSnapshotKeepsNullIssuerWhenUriInvalid() {
        val snapshot = buildCompanionSyncSnapshot(
            sourceAccounts = listOf(
                CompanionSyncSourceAccount(
                    accountId = "acc-1",
                    accountLabel = "user@example.com",
                    otpAuthUri = "invalid-uri",
                ),
            ),
            generatedAtEpochSec = 1700000000L,
        )

        assertNull(snapshot.accounts.single().issuer)
    }
}
