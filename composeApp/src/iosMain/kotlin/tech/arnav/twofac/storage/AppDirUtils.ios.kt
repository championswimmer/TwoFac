package tech.arnav.twofac.storage

import io.github.xxfast.kstore.KStore
import io.github.xxfast.kstore.file.storeOf
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import tech.arnav.twofac.lib.storage.StoredAccount

private fun getDocumentDirectory(): String {
    val documentDirectories = NSSearchPathForDirectoriesInDomains(
        directory = NSDocumentDirectory,
        domainMask = NSUserDomainMask,
        expandTilde = true
    )
    return documentDirectories.firstOrNull() as? String ?: ""
}

actual fun createAccountsStore(): KStore<List<StoredAccount>> {
    val dir = getDocumentDirectory()
    SystemFileSystem.createDirectories(Path(dir))
    val filePath = Path(dir, "accounts.json")

    return storeOf(
        file = filePath,
        default = emptyList()
    )
}

actual fun getStoragePath(): String {
    val dir = getDocumentDirectory()
    return Path(dir, "accounts.json").toString()
}