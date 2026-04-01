package tech.arnav.twofac.watch.otp

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.arnav.twofac.lib.watchsync.WatchSyncSnapshot
import kotlin.time.Duration.Companion.seconds

class WatchOtpProviderTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `buildCodes properly generates TOTP entries from valid snapshot`() = runTest {
        val provider = WatchOtpProvider()
        val account = tech.arnav.twofac.lib.watchsync.WatchSyncAccount(
            accountId = "1",
            issuer = "GitHub",
            accountLabel = "test@example.com",
            otpAuthUri = "otpauth://totp/GitHub:test@example.com?secret=JBSWY3DPEHPK3PXP&issuer=GitHub"
        )
        
        val snapshot = WatchSyncSnapshot(
            accounts = listOf(account),
            generatedAtEpochSec = 1000L
        )

        // specific time for stable TOTP generation
        val now = 1_600_000_000L
        val codes = provider.buildCodes(snapshot, nowEpochSec = now)

        assertEquals(1, codes.size)
        val entry = codes.first()
        assertTrue(entry is WatchOtpEntry.Valid)
        
        val validEntry = entry as WatchOtpEntry.Valid
        assertEquals("GitHub", validEntry.issuer)
        assertEquals("test@example.com", validEntry.account.accountLabel)
        assertEquals(30L, validEntry.periodSec)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `buildCodes handles invalid OTP URIs gracefully`() = runTest {
        val provider = WatchOtpProvider()
        val account = tech.arnav.twofac.lib.watchsync.WatchSyncAccount(
            accountId = "2",
            issuer = "BadIssuer",
            accountLabel = "bad@example.com",
            otpAuthUri = "invalid_uri_string"
        )
        
        val snapshot = WatchSyncSnapshot(
            accounts = listOf(account),
            generatedAtEpochSec = 1000L
        )

        val codes = provider.buildCodes(snapshot, nowEpochSec = 1000L)
        
        assertEquals(1, codes.size)
        val entry = codes.first()
        assertTrue(entry is WatchOtpEntry.Invalid)
        
        val invalidEntry = entry as WatchOtpEntry.Invalid
        assertEquals("BadIssuer", invalidEntry.issuer)
    }
}
