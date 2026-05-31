package tech.arnav.twofac.viewmodels

import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import tech.arnav.twofac.lib.TwoFacLib
import tech.arnav.twofac.lib.storage.MemoryStorage
import tech.arnav.twofac.lib.theme.AccountColorTag
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class AccountsViewModelTest {
    @Test
    fun `updateAccountColor refreshes accounts and otp state`() = runTest {
        val passkey = "test-passkey"
        val lib = TwoFacLib.initialise(storage = MemoryStorage(), passKey = passkey)
        lib.unlock(passkey)
        lib.addAccount(
            "otpauth://totp/Test:alice@example.com?secret=JBSWY3DPEHPK3PXP&issuer=Test"
        )
        val accountId = lib.getAllAccounts().single().accountID
        val viewModel = AccountsViewModel(twoFacLib = lib)

        var completed = false
        viewModel.updateAccountColor(accountId, AccountColorTag.TEAL) { success ->
            assertTrue(success)
            completed = true
        }
        while (!completed) yield()

        assertEquals(AccountColorTag.TEAL, viewModel.accounts.value.single().color)
        assertEquals(AccountColorTag.TEAL, viewModel.accountOtps.value.single().first.color)
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `getFreshOtpForAccount refreshes stale cached otp`() = runTest {
        val passkey = "test-passkey"
        val lib = TwoFacLib.initialise(storage = MemoryStorage(), passKey = passkey)
        lib.unlock(passkey)
        lib.addAccount(
            "otpauth://totp/Test:alice@example.com?secret=JBSWY3DPEHPK3PXP&issuer=Test&period=1"
        )
        val accountId = lib.getAllAccounts().single().accountID

        val viewModel = AccountsViewModel(twoFacLib = lib)

        val firstOtp = viewModel.getFreshOtpForAccount(accountId)
        assertNotNull(firstOtp)
        assertEquals(firstOtp, viewModel.getOtpForAccount(accountId))

        waitForNextEpochSecond()

        // Cached accessor still returns the previous value until explicit refresh.
        val staleOtp = viewModel.getOtpForAccount(accountId)
        assertEquals(firstOtp, staleOtp)

        val refreshedOtp = viewModel.getFreshOtpForAccount(accountId)
        assertNotNull(refreshedOtp)
        assertNotEquals(firstOtp, refreshedOtp)
        assertEquals(refreshedOtp, viewModel.getOtpForAccount(accountId))
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun waitForNextEpochSecond(timeoutSeconds: Long = 3L) {
        val startedAt = Clock.System.now().epochSeconds
        while (Clock.System.now().epochSeconds == startedAt) {
            if (Clock.System.now().epochSeconds - startedAt > timeoutSeconds) {
                error("Timed out waiting for the next epoch second")
            }
            yield()
        }
    }
}
