package tech.arnav.twofac.storage

import io.github.xxfast.kstore.KStore
import tech.arnav.twofac.lib.storage.Storage
import tech.arnav.twofac.lib.storage.StoredAccount
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class FileStorage(
    private val kstore: KStore<List<StoredAccount>>
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
}