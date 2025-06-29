package tech.arnav.twofac.lib.storage

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

interface Storage {

    suspend fun getAccountList(): List<StoredAccount>

    suspend fun getAccount(accountLabel: String): StoredAccount?

    @OptIn(ExperimentalUuidApi::class)
    suspend fun getAccount(accountID: Uuid): StoredAccount?

    suspend fun saveAccount(account: StoredAccount): Boolean

}