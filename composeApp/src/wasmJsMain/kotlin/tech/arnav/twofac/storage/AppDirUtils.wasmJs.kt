package tech.arnav.twofac.storage

import io.github.xxfast.kstore.KStore
import io.github.xxfast.kstore.storage.storeOf
import tech.arnav.twofac.lib.storage.StoredAccount

actual fun createAccountsStore(): KStore<List<StoredAccount>> {
    return storeOf(
        key = ACCOUNTS_STORAGE_KEY,
        default = emptyList()
    )
}

actual fun getStoragePath(): String {
    return "Browser LocalStorage (key: $ACCOUNTS_STORAGE_KEY)"
}