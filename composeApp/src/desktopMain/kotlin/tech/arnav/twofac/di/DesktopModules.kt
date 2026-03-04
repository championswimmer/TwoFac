package tech.arnav.twofac.di

import ca.gosyer.appdirs.AppDirs
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import org.koin.dsl.module
import tech.arnav.twofac.backup.LocalFileBackupTransport
import tech.arnav.twofac.lib.backup.BackupService
import tech.arnav.twofac.lib.backup.BackupTransport
import tech.arnav.twofac.lib.TwoFacLib
import tech.arnav.twofac.qr.ClipboardQRCodeReader
import tech.arnav.twofac.qr.DesktopClipboardQRCodeReader
import tech.arnav.twofac.settings.DesktopSettingsManager

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
    single<BackupTransport> {
        LocalFileBackupTransport(getBackupDir())
    }
    single<BackupService> {
        BackupService(get<TwoFacLib>())
    }
}

val desktopQrModule = module {
    single<ClipboardQRCodeReader> { DesktopClipboardQRCodeReader() }
}

val desktopSettingsModule = module {
    single<DesktopSettingsManager> { DesktopSettingsManager() }
}
