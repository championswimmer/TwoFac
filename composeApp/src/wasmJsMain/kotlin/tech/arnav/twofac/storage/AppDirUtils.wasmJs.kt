package tech.arnav.twofac.storage

import io.github.xxfast.kstore.KStore
import io.github.xxfast.kstore.storage.storeOf
import tech.arnav.twofac.lib.storage.StoredAccount
import tech.arnav.twofac.lib.storage.StoredTag

actual fun createAccountsStore(): KStore<List<StoredAccount>> {
    return storeOf(
        key = ACCOUNTS_STORAGE_KEY,
        default = emptyList()
    )
}

actual fun createTagsStore(): KStore<List<StoredTag>> {
    return storeOf(
        key = TAGS_STORAGE_KEY,
        default = emptyList()
    )
}

actual fun getStoragePath(): String {
    return "Browser LocalStorage (key: $ACCOUNTS_STORAGE_KEY)"
}

actual suspend fun deleteAccountsStorage(): Boolean {
    return try {
        // Use the kstore's own delete function for platform-agnostic deletion
        createAccountsStore().delete()
        true
    } catch (_: Throwable) {
        false
    }
}
