package tech.arnav.twofac.lib.storage

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class MemoryStorageTest {

    private fun createTestStoredAccount(
        accountID: Uuid = Uuid.random(),
        accountLabel: String = "Test:user@example.com",
        salt: String = "test-salt",
        encryptedURI: String = "encrypted-uri-data"
    ) = StoredAccount(
        accountID = accountID,
        accountLabel = accountLabel,
        salt = salt,
        encryptedURI = encryptedURI
    )

    @Test
    fun testEmptyStorageReturnsEmptyList() = runTest {
        val storage = MemoryStorage()
        val accounts = storage.getAccountList()
        assertTrue(accounts.isEmpty())
    }

    @Test
    fun testSaveAndRetrieveAccountByLabel() = runTest {
        val storage = MemoryStorage()
        val testAccount = createTestStoredAccount(accountLabel = "GitHub:user@example.com")

        // Save account
        val saveResult = storage.saveAccount(testAccount)
        assertTrue(saveResult)

        // Retrieve by label
        val retrievedAccount = storage.getAccount("GitHub:user@example.com")
        assertEquals(testAccount, retrievedAccount)
    }

    @Test
    fun testSaveAndRetrieveAccountByID() = runTest {
        val storage = MemoryStorage()
        val accountID = Uuid.random()
        val testAccount = createTestStoredAccount(accountID = accountID)

        // Save account
        val saveResult = storage.saveAccount(testAccount)
        assertTrue(saveResult)

        // Retrieve by ID
        val retrievedAccount = storage.getAccount(accountID)
        assertEquals(testAccount, retrievedAccount)
    }

    @Test
    fun testGetNonExistentAccountByLabelReturnsNull() = runTest {
        val storage = MemoryStorage()
        val retrievedAccount = storage.getAccount("NonExistent:account@example.com")
        assertNull(retrievedAccount)
    }

    @Test
    fun testGetNonExistentAccountByIDReturnsNull() = runTest {
        val storage = MemoryStorage()
        val nonExistentID = Uuid.random()
        val retrievedAccount = storage.getAccount(nonExistentID)
        assertNull(retrievedAccount)
    }

    @Test
    fun testSaveMultipleAccountsAndRetrieveList() = runTest {
        val storage = MemoryStorage()
        val account1 = createTestStoredAccount(accountLabel = "GitHub:user1@example.com")
        val account2 = createTestStoredAccount(accountLabel = "Google:user2@example.com")
        val account3 = createTestStoredAccount(accountLabel = "Microsoft:user3@example.com")

        // Save all accounts
        assertTrue(storage.saveAccount(account1))
        assertTrue(storage.saveAccount(account2))
        assertTrue(storage.saveAccount(account3))

        // Retrieve list
        val accountList = storage.getAccountList()
        assertEquals(3, accountList.size)
        assertTrue(accountList.contains(account1))
        assertTrue(accountList.contains(account2))
        assertTrue(accountList.contains(account3))
    }

    @Test
    fun testUpdateExistingAccount() = runTest {
        val storage = MemoryStorage()
        val accountID = Uuid.random()
        val originalAccount = createTestStoredAccount(
            accountID = accountID,
            accountLabel = "GitHub:user@example.com",
            encryptedURI = "original-encrypted-data"
        )

        // Save original account
        assertTrue(storage.saveAccount(originalAccount))

        // Update the account with new data
        val updatedAccount = originalAccount.copy(
            encryptedURI = "updated-encrypted-data",
            salt = "updated-salt"
        )
        assertTrue(storage.saveAccount(updatedAccount))

        // Verify the account was updated
        val retrievedAccount = storage.getAccount(accountID)
        assertEquals(updatedAccount, retrievedAccount)
        assertEquals("updated-encrypted-data", retrievedAccount?.encryptedURI)
        assertEquals("updated-salt", retrievedAccount?.salt)

        // Verify only one account exists in the list
        val accountList = storage.getAccountList()
        assertEquals(1, accountList.size)
        assertEquals(updatedAccount, accountList.first())
    }

    @Test
    fun testAccountListIsImmutable() = runTest {
        val storage = MemoryStorage()
        val testAccount = createTestStoredAccount()

        // Save account
        storage.saveAccount(testAccount)

        // Get the account list
        val accountList = storage.getAccountList()
        val originalSize = accountList.size

        // Try to modify the returned list (this should not affect the storage)
        val mutableList = accountList.toMutableList()
        mutableList.clear()

        // Verify the storage is unchanged
        val newAccountList = storage.getAccountList()
        assertEquals(originalSize, newAccountList.size)
        assertEquals(testAccount, newAccountList.first())
    }

    @Test
    fun testSaveAccountsWithSameLabelButDifferentID() = runTest {
        val storage = MemoryStorage()
        val accountID1 = Uuid.random()
        val accountID2 = Uuid.random()
        val account1 = createTestStoredAccount(
            accountID = accountID1,
            accountLabel = "GitHub:user@example.com"
        )
        val account2 = createTestStoredAccount(
            accountID = accountID2,
            accountLabel = "GitHub:user@example.com" // Same label, different ID
        )

        // Save both accounts
        assertTrue(storage.saveAccount(account1))
        assertTrue(storage.saveAccount(account2))

        // Both accounts should exist
        val accountList = storage.getAccountList()
        assertEquals(2, accountList.size)

        // Retrieve by ID should work for both
        assertEquals(account1, storage.getAccount(accountID1))
        assertEquals(account2, storage.getAccount(accountID2))

        // Retrieve by label should return one of them (implementation dependent)
        val retrievedByLabel = storage.getAccount("GitHub:user@example.com")
        assertTrue(retrievedByLabel == account1 || retrievedByLabel == account2)
    }

    @Test
    fun testLargeNumberOfAccounts() = runTest {
        val storage = MemoryStorage()
        val accounts = mutableListOf<StoredAccount>()

        // Create and save 100 accounts
        repeat(100) { index ->
            val account = createTestStoredAccount(
                accountLabel = "Service$index:user$index@example.com"
            )
            accounts.add(account)
            assertTrue(storage.saveAccount(account))
        }

        // Verify all accounts are stored
        val accountList = storage.getAccountList()
        assertEquals(100, accountList.size)

        // Verify each account can be retrieved
        accounts.forEach { account ->
            assertEquals(account, storage.getAccount(account.accountLabel))
            assertEquals(account, storage.getAccount(account.accountID))
        }
    }
}
