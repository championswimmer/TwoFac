package tech.arnav.twofac.storage

import ca.gosyer.appdirs.AppDirs
import io.github.xxfast.kstore.KStore
import io.github.xxfast.kstore.file.storeOf
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import tech.arnav.twofac.lib.storage.StoredAccount

private val appDirs = AppDirs {
    appName = "TwoFac"
    appAuthor = "tech.arnav"
    macOS.useSpaceBetweenAuthorAndApp = false
}

actual fun createAccountsStore(): KStore<List<StoredAccount>> {
    val dir = appDirs.getUserDataDir()
    SystemFileSystem.createDirectories(Path(dir))
    val filePath = Path(dir, "accounts.json")

    return storeOf(
        file = filePath,
        default = emptyList()
    )
}

actual fun getStoragePath(): String {
    val dir = appDirs.getUserDataDir()
    return Path(dir, "accounts.json").toString()
}