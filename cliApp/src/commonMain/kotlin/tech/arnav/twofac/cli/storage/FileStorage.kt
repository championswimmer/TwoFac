package tech.arnav.twofac.cli.storage

import io.github.xxfast.kstore.extensions.getOrEmpty
import io.github.xxfast.kstore.file.extensions.listStoreOf
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import tech.arnav.twofac.lib.storage.Storage
import tech.arnav.twofac.lib.storage.StoredAccount
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class FileStorage(
    private val storageFilePath: Path
) : Storage {
    private val kstore = listStoreOf<StoredAccount>(storageFilePath)

    override suspend fun getAccountList(): List<StoredAccount> =
        kstore.getOrEmpty()

    override suspend fun getAccount(accountLabel: String): StoredAccount? =
        kstore.getOrEmpty().firstOrNull { it.accountLabel == accountLabel }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun getAccount(accountID: Uuid): StoredAccount? =
        kstore.getOrEmpty().firstOrNull { it.accountID == accountID }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun saveAccount(account: StoredAccount): Boolean {
        return try {
            val currentAccounts = kstore.getOrEmpty().toMutableList()
            val existingIndex = currentAccounts.indexOfFirst { it.accountID == account.accountID }
            if (existingIndex >= 0) {
                currentAccounts[existingIndex] = account
            } else {
                currentAccounts.add(account)
            }
            kstore.set(currentAccounts)
            true
        } catch (_: Exception) {
            false
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun deleteAccount(accountID: Uuid): Boolean {
        return try {
            val currentAccounts = kstore.getOrEmpty()
            val updatedAccounts = currentAccounts.filterNot { it.accountID == accountID }
            if (updatedAccounts.size == currentAccounts.size) {
                return false
            }
            kstore.set(updatedAccounts)
            true
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun deleteAllAccounts(): Boolean {
        return try {
            if (SystemFileSystem.exists(storageFilePath)) {
                SystemFileSystem.delete(storageFilePath)
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}
