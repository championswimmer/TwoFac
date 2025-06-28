package tech.arnav.twofac.lib.storage

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class MemoryStorage : Storage {
    // This is a simple in-memory storage implementation.
    // In a real application, you would implement the methods to store and retrieve accounts.


    private val accounts = mutableListOf<StoredAccount>()

    override fun getAccountList(): List<StoredAccount> {
        // Return a copy of the list to prevent external modification
        return accounts.toList()
    }

    override fun getAccount(accountLabel: String): StoredAccount? {
        // Find the account by label
        return accounts.find { it.accountLabel == accountLabel }
    }

    @OptIn(ExperimentalUuidApi::class)
    override fun getAccount(accountID: Uuid): StoredAccount? {
        // Find the account by ID
        return accounts.find { it.accountID == accountID }
    }

    @OptIn(ExperimentalUuidApi::class)
    override fun saveAccount(account: StoredAccount): Boolean {
        // Check if the account already exists
        val existingAccountIndex = accounts.indexOfFirst { it.accountID == account.accountID }

        return if (existingAccountIndex != -1) {
            // Update existing account
            accounts[existingAccountIndex] = account
            true
        } else {
            // Add new account
            accounts.add(account)
            true
        }
    }
}