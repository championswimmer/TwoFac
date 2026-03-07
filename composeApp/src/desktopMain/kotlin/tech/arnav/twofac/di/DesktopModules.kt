package tech.arnav.twofac.di

import ca.gosyer.appdirs.AppDirs
import io.ktor.client.HttpClient
import io.ktor.client.engine.java.Java
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import org.koin.dsl.module
import tech.arnav.twofac.backup.GoogleDriveBackupTransport
import tech.arnav.twofac.backup.LocalFileBackupTransport
import tech.arnav.twofac.lib.backup.BackupProvider
import tech.arnav.twofac.lib.backup.BackupTransportRegistry
import tech.arnav.twofac.lib.backup.GoogleDriveAppDataBackupProviderInfo
import tech.arnav.twofac.lib.backup.LocalBackupProviderInfo
import tech.arnav.twofac.lib.backup.backupTransportRegistryOf
import tech.arnav.twofac.qr.ClipboardQRCodeReader
import tech.arnav.twofac.qr.DesktopClipboardQRCodeReader
import tech.arnav.twofac.settings.DesktopSettingsManager
import tech.arnav.twofac.storage.createGoogleDriveAuthStore

private val appDirs = AppDirs {
    appName = "TwoFac"
    appAuthor = "tech.arnav"
    macOS.useSpaceBetweenAuthorAndApp = false
}

private fun getBackupDir(): Path {
    val dir = Path(appDirs.getUserDataDir(), "backups")
    SystemFileSystem.createDirectories(dir)
    return dir
}

val desktopBackupModule = module {
    single {
        HttpClient(Java)
    }
    single<BackupTransportRegistry> {
        backupTransportRegistryOf(
            BackupProvider(
                info = LocalBackupProviderInfo,
                transport = LocalFileBackupTransport(getBackupDir()),
            ),
            BackupProvider(
                info = GoogleDriveAppDataBackupProviderInfo,
                transport = GoogleDriveBackupTransport(
                    authStore = createGoogleDriveAuthStore(),
                    httpClient = get(),
                ),
            ),
        )
    }
}

val desktopQrModule = module {
    single<ClipboardQRCodeReader> { DesktopClipboardQRCodeReader() }
}

val desktopSettingsModule = module {
    single<DesktopSettingsManager> { DesktopSettingsManager() }
}
