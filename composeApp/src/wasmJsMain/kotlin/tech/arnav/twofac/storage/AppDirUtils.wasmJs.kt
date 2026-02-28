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

actual suspend fun deleteAccountsStorage(): Boolean {
    return try {
        localStorageRemoveItem(ACCOUNTS_STORAGE_KEY)
        true
    } catch (_: Throwable) {
        false
    }
}

@JsFun("(key) => { window.localStorage.removeItem(key); }")
private external fun localStorageRemoveItem(key: String)
