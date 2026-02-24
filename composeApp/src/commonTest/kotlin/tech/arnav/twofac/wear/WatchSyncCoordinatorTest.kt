package tech.arnav.twofac.wear

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WatchSyncCoordinatorTest {
    @Test
    fun testBuildWatchSyncSnapshotMapsIssuerWhenUriValid() {
        val snapshot = buildWatchSyncSnapshot(
            sourceAccounts = listOf(
                WatchSyncSourceAccount(
                    accountId = "acc-1",
                    accountLabel = "user@example.com",
                    otpAuthUri = "otpauth://totp/GitHub:user@example.com?secret=JBSWY3DPEHPK3PXP&issuer=GitHub",
                ),
            ),
            generatedAtEpochSec = 1700000000L,
        )

        assertEquals(1700000000L, snapshot.generatedAtEpochSec)
        assertEquals("GitHub", snapshot.accounts.single().issuer)
    }

    @Test
    fun testBuildWatchSyncSnapshotKeepsNullIssuerWhenUriInvalid() {
        val snapshot = buildWatchSyncSnapshot(
            sourceAccounts = listOf(
                WatchSyncSourceAccount(
                    accountId = "acc-1",
                    accountLabel = "user@example.com",
                    otpAuthUri = "invalid-uri",
                ),
            ),
            generatedAtEpochSec = 1700000000L,
        )

        assertNull(snapshot.accounts.single().issuer)
    }

    @Test
    fun testIsSyncToWatchEnabledRequiresActiveCompanionAndIdleState() {
        assertTrue(isSyncToWatchEnabled(isCompanionActive = true, isSyncInProgress = false))
        assertFalse(isSyncToWatchEnabled(isCompanionActive = false, isSyncInProgress = false))
        assertFalse(isSyncToWatchEnabled(isCompanionActive = true, isSyncInProgress = true))
    }
}
