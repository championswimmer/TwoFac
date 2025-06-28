package tech.arnav.twofac.lib.storage

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

interface Storage {

    fun getAccountList(): List<StoredAccount>

    fun getAccount(accountLabel: String): StoredAccount?

    @OptIn(ExperimentalUuidApi::class)
    fun getAccount(accountID: Uuid): StoredAccount?

    fun saveAccount(account: StoredAccount): Boolean

}