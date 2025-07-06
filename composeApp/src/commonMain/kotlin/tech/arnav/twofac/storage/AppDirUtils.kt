package tech.arnav.twofac.storage

import io.github.xxfast.kstore.KStore
import tech.arnav.twofac.lib.storage.StoredAccount

const val ACCOUNTS_STORAGE_KEY = "twofac_accounts"
const val ACCOUNTS_STORAGE_FILE = "accounts.json"

expect fun createAccountsStore(): KStore<List<StoredAccount>>

expect fun getStoragePath(): String