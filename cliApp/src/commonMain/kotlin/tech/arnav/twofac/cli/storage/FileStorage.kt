package tech.arnav.twofac.cli.storage

import tech.arnav.twofac.lib.storage.Storage
import tech.arnav.twofac.lib.storage.StoredAccount
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class FileStorage() : Storage {
    override fun getAccountList(): List<StoredAccount> {
        TODO("Not yet implemented")
    }

    override fun getAccount(accountLabel: String): StoredAccount? {
        TODO("Not yet implemented")
    }

    @OptIn(ExperimentalUuidApi::class)
    override fun getAccount(accountID: Uuid): StoredAccount? {
        TODO("Not yet implemented")
    }

    override fun saveAccount(account: StoredAccount): Boolean {
        TODO("Not yet implemented")
    }
}