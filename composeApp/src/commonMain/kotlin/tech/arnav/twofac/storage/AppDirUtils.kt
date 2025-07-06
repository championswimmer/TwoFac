package tech.arnav.twofac.storage

import io.github.xxfast.kstore.KStore
import tech.arnav.twofac.lib.storage.StoredAccount

const val ACCOUNTS_STORAGE_KEY = "2fac_accounts"

expect fun createAccountsStore(): KStore<List<StoredAccount>>

expect fun getStoragePath(): String