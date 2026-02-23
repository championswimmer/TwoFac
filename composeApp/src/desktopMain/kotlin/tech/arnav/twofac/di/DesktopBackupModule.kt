package tech.arnav.twofac.di

import org.koin.dsl.module
import tech.arnav.twofac.backup.DesktopLocalBackupStore
import tech.arnav.twofac.lib.backup.LocalBackupTransport
import tech.arnav.twofac.storage.getBackupDir

val desktopBackupModule = module {
    single {
        LocalBackupTransport(
            store = DesktopLocalBackupStore(
                baseDir = getBackupDir(forceCreate = true)
            )
        )
    }
}
