package tech.arnav.twofac.viewmodels

import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import tech.arnav.twofac.lib.TwoFacLib
import tech.arnav.twofac.lib.storage.MemoryStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class AccountsViewModelTest {
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

    @Test
    fun `getOtpAuthUriForAccount sets error when account is missing`() = runTest {
        val passkey = "test-passkey"
        val lib = TwoFacLib.initialise(storage = MemoryStorage(), passKey = passkey)
        lib.unlock(passkey)
        val viewModel = AccountsViewModel(twoFacLib = lib)

        val uri = viewModel.getOtpAuthUriForAccount("missing-account-id")

        assertNull(uri)
        assertEquals("Account not found", viewModel.error.value)
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
