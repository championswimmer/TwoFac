package tech.arnav.twofac.lib.storage

import tech.arnav.twofac.lib.PublicApi
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@PublicApi
interface Storage {

    suspend fun getAccountList(): List<StoredAccount>

    suspend fun getAccount(accountLabel: String): StoredAccount?

    @OptIn(ExperimentalUuidApi::class)
    suspend fun getAccount(accountID: Uuid): StoredAccount?

    suspend fun saveAccount(account: StoredAccount): Boolean

    @OptIn(ExperimentalUuidApi::class)
    suspend fun deleteAccount(accountID: Uuid): Boolean

    suspend fun deleteAllAccounts(): Boolean

    // Tag operations — default no-op implementations allow existing Storage
    // implementations to compile without change.

    suspend fun getTagList(): List<StoredTag> = emptyList()

    suspend fun getTag(tagId: String): StoredTag? = null

    suspend fun saveTag(tag: StoredTag): Boolean = false

    suspend fun deleteTag(tagId: String): Boolean = false

}
