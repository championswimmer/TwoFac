package tech.arnav.twofac.storage

import io.github.xxfast.kstore.KStore
import tech.arnav.twofac.lib.storage.Storage
import tech.arnav.twofac.lib.storage.StoredAccount
import tech.arnav.twofac.lib.storage.StoredTag
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class FileStorage(
    private val kstore: KStore<List<StoredAccount>>,
    private val tagStore: KStore<List<StoredTag>>,
) : Storage {

    override suspend fun getAccountList(): List<StoredAccount> {
        return kstore.get() ?: emptyList()
    }

    override suspend fun getAccount(accountLabel: String): StoredAccount? {
        return getAccountList().find { it.accountLabel == accountLabel }
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun getAccount(accountID: Uuid): StoredAccount? {
        return getAccountList().find { it.accountID == accountID }
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun saveAccount(account: StoredAccount): Boolean {
        return try {
            val currentAccounts = getAccountList().toMutableList()
            val existingIndex = currentAccounts.indexOfFirst { it.accountID == account.accountID }

            if (existingIndex >= 0) {
                currentAccounts[existingIndex] = account
            } else {
                currentAccounts.add(account)
            }

            kstore.set(currentAccounts)
            true
        } catch (e: Exception) {
            false
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun deleteAccount(accountID: Uuid): Boolean {
        return try {
            val currentAccounts = getAccountList()
            val updatedAccounts = currentAccounts.filterNot { it.accountID == accountID }
            if (updatedAccounts.size == currentAccounts.size) {
                return false
            }
            kstore.set(updatedAccounts)
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun deleteAllAccounts(): Boolean {
        return deleteAccountsStorage()
    }

    // ─── Tag operations ───────────────────────────────────────────────────────

    override suspend fun getTagList(): List<StoredTag> = tagStore.get() ?: emptyList()

    override suspend fun getTag(tagId: String): StoredTag? =
        getTagList().find { it.tagId == tagId }

    override suspend fun saveTag(tag: StoredTag): Boolean {
        return try {
            val current = getTagList().toMutableList()
            val idx = current.indexOfFirst { it.tagId == tag.tagId }
            if (idx >= 0) current[idx] = tag else current.add(tag)
            tagStore.set(current)
            true
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun deleteTag(tagId: String): Boolean {
        return try {
            val current = getTagList()
            val updated = current.filterNot { it.tagId == tagId }
            if (updated.size == current.size) return false
            tagStore.set(updated)
            true
        } catch (_: Exception) {
            false
        }
    }
}
