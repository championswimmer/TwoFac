package tech.arnav.twofac.viewmodels

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import androidx.lifecycle.ViewModelStore
import tech.arnav.twofac.companion.CompanionSyncCoordinator
import tech.arnav.twofac.lib.importer.BulkImportResult
import kotlinx.coroutines.yield
import tech.arnav.twofac.lib.TwoFacLib
import tech.arnav.twofac.lib.storage.MemoryStorage
import tech.arnav.twofac.lib.storage.Storage
import tech.arnav.twofac.lib.storage.StoredAccount
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
    fun `bulk import unlocks refreshes state and syncs once`() = runTest {
        val lib = TwoFacLib.initialise(MemoryStorage())
        var syncs = 0
        val coordinator = object : CompanionSyncCoordinator {
            override suspend fun isCompanionActive() = true
            override suspend fun syncNow(manual: Boolean) = true
            override suspend fun onAccountsUnlocked() = Unit
            override suspend fun onAccountsChanged() { syncs++ }
        }
        val viewModel = AccountsViewModel(lib, companionSyncCoordinator = coordinator)
        val store = ViewModelStore().apply { put("test", viewModel) }
        try {
            val completion = CompletableDeferred<BulkImportResult?>()
            val uris = listOf("alice", "bob").map {
                "otpauth://totp/Example:$it?secret=JBSWY3DPEHPK3PXP&issuer=Example"
            }
            viewModel.importAccounts(uris, "123456", completion::complete)
            assertEquals(listOf(0, 1), completion.await()?.importedIndices)
            assertEquals(2, viewModel.accounts.value.size)
            assertEquals(2, viewModel.accountOtps.value.size)
            assertEquals(1, syncs)
        } finally { store.clear() }
    }

    @Test
    fun `bulk import without passkey rejects locked vault`() = runTest {
        val lib = TwoFacLib.initialise(MemoryStorage())
        val viewModel = AccountsViewModel(lib)
        val store = ViewModelStore().apply { put("test", viewModel) }
        try {
            val completion = CompletableDeferred<BulkImportResult?>()
            viewModel.importAccounts(listOf("secret payload"), null, completion::complete)
            assertEquals(null, completion.await())
            assertTrue(viewModel.accounts.value.isEmpty())
            val error = assertNotNull(viewModel.error.value)
            assertTrue(error.isNotBlank())
            assertTrue(!error.contains("secret payload"))
        } finally { store.clear() }
    }

    @Test
    fun `partial bulk import refreshes persisted accounts and reports retry indices`() = runTest {
        val memory = MemoryStorage()
        var writes = 0
        val storage = object : Storage by memory {
            override suspend fun saveAccount(account: StoredAccount): Boolean {
                if (++writes == 2) return false
                return memory.saveAccount(account)
            }
        }
        val lib = TwoFacLib.initialise(storage, "123456")
        lib.unlock("123456")
        val viewModel = AccountsViewModel(lib)
        val store = ViewModelStore().apply { put("test", viewModel) }
        try {
            val completion = CompletableDeferred<BulkImportResult?>()
            val uris = listOf("alice", "bob").map {
                "otpauth://totp/Example:$it?secret=JBSWY3DPEHPK3PXP&issuer=Example"
            }
            viewModel.importAccounts(uris, null, completion::complete)
            val result = completion.await()
            assertEquals(listOf(0), result?.importedIndices)
            assertEquals(listOf(1), result?.failedIndices)
            assertEquals(1, viewModel.accounts.value.size)
            assertEquals(1, viewModel.accountOtps.value.size)
        } finally { store.clear() }
    }

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
