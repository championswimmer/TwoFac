package tech.arnav.twofac.storage

import io.github.xxfast.kstore.KStore
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import tech.arnav.twofac.lib.storage.StoredAccount
import tech.arnav.twofac.lib.storage.StoredTag

const val ACCOUNTS_STORAGE_KEY = "twofac_accounts"
const val ACCOUNTS_STORAGE_FILE = "accounts.json"

const val TAGS_STORAGE_KEY = "twofac_tags"
const val TAGS_STORAGE_FILE = "tags.json"

expect fun createAccountsStore(): KStore<List<StoredAccount>>

expect fun createTagsStore(): KStore<List<StoredTag>>

expect fun getStoragePath(): String

expect suspend fun deleteAccountsStorage(): Boolean

internal fun ensureStorageFileExists(filePath: Path) {
    if (SystemFileSystem.exists(filePath)) return
    SystemFileSystem.sink(filePath).buffered().use { sink ->
        sink.write("[]".encodeToByteArray())
        sink.flush()
    }
}

internal fun deleteStorageFile(filePath: Path): Boolean {
    return try {
        if (SystemFileSystem.exists(filePath)) {
            SystemFileSystem.delete(filePath)
        }
        true
    } catch (_: Exception) {
        false
    }
}
