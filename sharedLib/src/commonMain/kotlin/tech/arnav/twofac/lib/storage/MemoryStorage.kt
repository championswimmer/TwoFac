package tech.arnav.twofac.lib.storage

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class MemoryStorage : Storage {
    // This is a simple in-memory storage implementation.
    // In a real application, you would implement the methods to store and retrieve accounts.

    private val accounts = mutableListOf<StoredAccount>()
    private val tags = mutableListOf<StoredTag>()

    override suspend fun getAccountList(): List<StoredAccount> {
        // Return a copy of the list to prevent external modification
        return accounts.toList()
    }

    override suspend fun getAccount(accountLabel: String): StoredAccount? {
        // Find the account by label
        return accounts.find { it.accountLabel == accountLabel }
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun getAccount(accountID: Uuid): StoredAccount? {
        // Find the account by ID
        return accounts.find { it.accountID == accountID }
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun saveAccount(account: StoredAccount): Boolean {
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

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun deleteAccount(accountID: Uuid): Boolean {
        return accounts.removeAll { it.accountID == accountID }
    }

    override suspend fun deleteAllAccounts(): Boolean {
        accounts.clear()
        return true
    }

    override suspend fun getTagList(): List<StoredTag> = tags.toList()

    override suspend fun getTag(tagId: String): StoredTag? =
        tags.find { it.tagId == tagId }

    override suspend fun saveTag(tag: StoredTag): Boolean {
        val idx = tags.indexOfFirst { it.tagId == tag.tagId }
        return if (idx != -1) {
            tags[idx] = tag
            true
        } else {
            tags.add(tag)
            true
        }
    }

    override suspend fun deleteTag(tagId: String): Boolean =
        tags.removeAll { it.tagId == tagId }
}
