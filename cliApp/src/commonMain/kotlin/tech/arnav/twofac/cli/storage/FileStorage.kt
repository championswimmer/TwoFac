package tech.arnav.twofac.cli.storage

import io.github.xxfast.kstore.extensions.getOrEmpty
import io.github.xxfast.kstore.extensions.plus
import io.github.xxfast.kstore.file.extensions.listStoreOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.io.files.Path
import tech.arnav.twofac.lib.storage.Storage
import tech.arnav.twofac.lib.storage.StoredAccount
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class FileStorage(
    private val storageFilePath: Path
) : Storage {
    private val kstore = listStoreOf<StoredAccount>(storageFilePath)

    // TODO: need to use IO dispatcher for file operations
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    override suspend fun getAccountList(): List<StoredAccount> = coroutineScope.async {
        kstore.getOrEmpty()
    }.await()

    override suspend fun getAccount(accountLabel: String): StoredAccount? = coroutineScope.async {
        kstore.getOrEmpty().firstOrNull { it.accountLabel == accountLabel }
    }.await()

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun getAccount(accountID: Uuid): StoredAccount? = coroutineScope.async {
        kstore.getOrEmpty().firstOrNull { it.accountID == accountID }
    }.await()

    override suspend fun saveAccount(account: StoredAccount): Boolean = coroutineScope.async {
        // TODO: handle duplicates and/or update existing accounts
        kstore.plus(account)
        true
    }.await()
}