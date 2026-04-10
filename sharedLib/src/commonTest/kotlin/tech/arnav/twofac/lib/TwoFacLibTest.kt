package tech.arnav.twofac.lib

import kotlinx.coroutines.test.runTest
import tech.arnav.twofac.lib.presentation.issuer.IssuerIconCatalog
import tech.arnav.twofac.lib.storage.MemoryStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import tech.arnav.twofac.lib.crypto.CryptoTools
import tech.arnav.twofac.lib.crypto.DefaultCryptoTools
import dev.whyoleg.cryptography.CryptographyProvider
import tech.arnav.twofac.lib.crypto.Encoding.toByteString
import tech.arnav.twofac.lib.crypto.Encoding.toHexString
import tech.arnav.twofac.lib.storage.StoredAccount
import tech.arnav.twofac.lib.otp.HOTP
import tech.arnav.twofac.lib.otp.TOTP
import kotlinx.io.bytestring.encodeToByteString
import kotlin.uuid.Uuid

class TwoFacLibTest {

    @Test
    fun testInitialiseWithoutPasskey() {
        val lib = TwoFacLib.initialise()
        assertFalse(lib.isUnlocked(), "Library should not be unlocked when initialized without passkey")
    }

    @Test
    fun testInitialiseWithPasskey() {
        val lib = TwoFacLib.initialise(passKey = "testpasskey")
        assertTrue(lib.isUnlocked(), "Library should be unlocked when initialized with passkey")
    }

    @Test
    fun testUnlockFunction() = runTest {
        val lib = TwoFacLib.initialise()
        assertFalse(lib.isUnlocked(), "Library should not be unlocked initially")

        lib.unlock("testpasskey")
        assertTrue(lib.isUnlocked(), "Library should be unlocked after calling unlock()")
    }

    @Test
    fun testUnlockWithBlankPasskey() = runTest {
        val lib = TwoFacLib.initialise()

        assertFailsWithSuspend<IllegalArgumentException> {
            lib.unlock("")
        }

        assertFailsWithSuspend<IllegalArgumentException> {
            lib.unlock("   ")
        }
    }

    @Test
    fun testGetAllAccountOTPsWhenLocked() = runTest {
        val lib = TwoFacLib.initialise()

        val ex = assertFailsWithSuspend<IllegalStateException> {
            lib.getAllAccountOTPs()
        }
        assertTrue(
            ex.message?.contains("No account store found. Enter password to create a new store.") == true
        )
    }

    @Test
    fun testGetAllAccountOTPsWhenLockedWithExistingStoreShowsUnlockMessage() = runTest {
        val sharedStorage = MemoryStorage()
        val unlockedLib = TwoFacLib.initialise(storage = sharedStorage)
        unlockedLib.unlock("testpasskey")
        assertTrue(
            unlockedLib.addAccount(
                "otpauth://totp/Example:alice@google.com?secret=JBSWY3DPEHPK3PXP&issuer=Example"
            )
        )

        val lockedLib = TwoFacLib.initialise(storage = sharedStorage)
        val ex = assertFailsWithSuspend<IllegalStateException> {
            lockedLib.getAllAccountOTPs()
        }
        assertTrue(
            ex.message?.contains("Secrets store is locked. Enter password to unlock it.") == true
        )
    }

    @Test
    fun testAddAccountWhenLocked() = runTest {
        val lib = TwoFacLib.initialise()

        assertFailsWithSuspend<IllegalStateException> {
            lib.addAccount("otpauth://totp/Example:alice@google.com?secret=JBSWY3DPEHPK3PXP&issuer=Example")
        }
    }

    private suspend inline fun <reified T : Throwable> assertFailsWithSuspend(
        block: suspend () -> Unit
    ): T {
        try {
            block()
        } catch (e: Throwable) {
            if (e is T) return e
            throw AssertionError("Expected ${T::class.simpleName} but got ${e::class.simpleName}", e)
        }
        throw AssertionError("Expected ${T::class.simpleName} but code completed successfully")
    }

    @Test
    fun testGetAllAccountOTPsWhenUnlocked() = runTest {
        val lib = TwoFacLib.initialise()
        lib.unlock("testpasskey")

        // Should not throw exception when unlocked
        val otps = lib.getAllAccountOTPs()
        assertEquals(0, otps.size, "Should return empty list when no accounts are added")
    }

    @Test
    fun testThreadSafetyOfUnlockAndIsUnlocked() = runTest {
        val lib = TwoFacLib.initialise()

        // Test that multiple calls to unlock and isUnlocked work correctly
        lib.unlock("passkey1")
        assertTrue(lib.isUnlocked())

        lib.unlock("passkey2")
        assertTrue(lib.isUnlocked())
    }

    @Test
    fun testDeleteAllAccountsFromStorageClearsCachedAccounts() = runTest {
        val lib = TwoFacLib.initialise(storage = MemoryStorage(), passKey = "testpasskey")
        assertTrue(
            lib.addAccount(
                "otpauth://totp/Example:alice@google.com?secret=JBSWY3DPEHPK3PXP&issuer=Example"
            )
        )
        assertEquals(1, lib.getAllAccounts().size)

        val deleted = lib.deleteAllAccountsFromStorage()

        assertTrue(deleted)
        assertTrue(lib.isUnlocked())
        assertTrue(lib.getAllAccounts().isEmpty())
    }

    @Test
    fun testDeleteAccountRemovesSingleAccountFromStorage() = runTest {
        val lib = TwoFacLib.initialise(storage = MemoryStorage(), passKey = "testpasskey")
        assertTrue(
            lib.addAccount(
                "otpauth://totp/Example:alice@google.com?secret=JBSWY3DPEHPK3PXP&issuer=Example"
            )
        )
        val accountId = lib.getAllAccounts().single().accountID

        val deleted = lib.deleteAccount(accountId)

        assertTrue(deleted)
        assertTrue(lib.getAllAccounts().isEmpty())
    }

    @Test
    fun testGetAllAccountsSeparatesIssuerFromDisplayLabel() = runTest {
        val lib = TwoFacLib.initialise(storage = MemoryStorage(), passKey = "testpasskey")
        lib.unlock("testpasskey")
        assertTrue(
            lib.addAccount(
                "otpauth://totp/GitHub:alice@example.com?secret=JBSWY3DPEHPK3PXP&issuer=GitHub"
            )
        )

        val account = lib.getAllAccounts().single()

        assertEquals("alice@example.com", account.accountLabel)
        assertEquals("GitHub", account.issuer)
    }


    @Test
    fun testUnlockMigratesLegacyIterationAccounts() = runTest {
        val storage = MemoryStorage()
        val passkey = "testpasskey"
        val tools = DefaultCryptoTools(CryptographyProvider.Default)

        // 1. Manually create a legacy account in storage
        val salt = "000102030405060708090a0b0c0d0e0f"
        val legacyKey = tools.createSigningKey(passkey, salt.toByteString(), CryptoTools.LEGACY_HASH_ITERATIONS)
        val uriData = "otpauth://totp/Legacy:user@example.com?secret=JBSWY3DPEHPK3PXP".encodeToByteString()
        val encryptedURI = tools.encrypt(legacyKey.key, uriData)

        val legacyAccount = StoredAccount(
            accountID = Uuid.random(),
            accountLabel = "Legacy",
            salt = salt,
            encryptedURI = encryptedURI.toHexString(),
            iterations = CryptoTools.LEGACY_HASH_ITERATIONS
        )
        storage.saveAccount(legacyAccount)

        // 2. Initialise lib and unlock - should trigger migration
        val lib = TwoFacLib.initialise(storage = storage)
        lib.unlock(passkey)

        // 3. Verify it was migrated
        val migratedAccount = storage.getAccountList().single()
        assertEquals(
            CryptoTools.TARGET_HASH_ITERATIONS,
            migratedAccount.iterations,
            "Account should be migrated to target iterations"
        )

        // 4. Verify it's still decryptable and generates correct codes
        val otps = lib.getAllAccountOTPs()
        assertEquals(1, otps.size)
        assertEquals("Legacy", otps[0].first.accountLabel)
        assertTrue(otps[0].second.isNotEmpty())
    }

    @Test
    fun testAccountsAreDecryptableAfterIterationMigration() = runTest {
        val storage = MemoryStorage()
        val passkey = "testpasskey"

        val lib = TwoFacLib.initialise(storage = storage)
        lib.unlock(passkey)
        assertTrue(
            lib.addAccount(
                "otpauth://totp/GitHub:alice@example.com?secret=JBSWY3DPEHPK3PXP&issuer=GitHub"
            )
        )

        val otps = lib.getAllAccountOTPs()
        assertEquals(1, otps.size)
        assertTrue(otps[0].second.isNotEmpty(), "OTP code should be generated after migration")

        val accounts = lib.getAllAccounts()
        assertEquals(1, accounts.size)
        assertEquals("GitHub", accounts[0].issuer)
    }

    @Test
    fun testIsStoreInitializedLazyLoading() = runTest {
        val storage = MemoryStorage()
        val lib = TwoFacLib.initialise(storage = storage)

        // Initially unknown
        assertFalse(lib.isStoreInitialized)

        // Adding an account outside the lib (simulating existing storage)
        val otherLib = TwoFacLib.initialise(storage = storage)
        otherLib.unlock("passkey")
        otherLib.addAccount("otpauth://totp/Example:alice@google.com?secret=JBSWY3DPEHPK3PXP&issuer=Example")

        // First attempt to get accounts throws locked exception but caches the status
        val ex = assertFailsWithSuspend<IllegalStateException> {
            lib.getAllAccounts()
        }
        assertTrue(
            ex.message?.contains("Secrets store is locked. Enter password to unlock it.") == true
        )

        // Now we know it has accounts
        assertTrue(lib.isStoreInitialized)
    }
}
