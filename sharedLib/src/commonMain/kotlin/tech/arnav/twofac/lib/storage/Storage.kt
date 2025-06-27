package tech.arnav.twofac.lib.storage

interface Storage {

    fun getAccountList(): List<StoredAccount>

    fun getAccount(accountLabel: String): StoredAccount?

    fun saveAccount(account: StoredAccount): Boolean

}