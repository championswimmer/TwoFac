package tech.arnav.twofac.cli.di

import org.koin.core.qualifier.named
import org.koin.dsl.module
import tech.arnav.twofac.cli.backup.FileLocalBackupStore
import tech.arnav.twofac.cli.storage.AppDirUtils
import tech.arnav.twofac.cli.storage.FileStorage
import tech.arnav.twofac.cli.viewmodels.AccountsViewModel
import tech.arnav.twofac.lib.TwoFacLib
import tech.arnav.twofac.lib.backup.BackupService
import tech.arnav.twofac.lib.backup.LocalBackupTransport
import tech.arnav.twofac.lib.storage.Storage

val storageModule = module {

    single(named<StorageFilePath>()) {
        AppDirUtils.getStorageFilePath(forceCreate = true)
    }

    single(named<BackupDirPath>()) {
        AppDirUtils.getBackupDir(forceCreate = true)
    }

    single<Storage> {
        FileStorage(get(named<StorageFilePath>()))
    }
}

val appModule = module {

    single {
        BackupService(storage = get())
    }

    single {
        LocalBackupTransport(
            store = FileLocalBackupStore(
                baseDir = get(named<BackupDirPath>())
            )
        )
    }

    single<TwoFacLib> {
        TwoFacLib.initialise(
            storage = get(),
        )
    }

    single<AccountsViewModel> {
        AccountsViewModel(twoFacLib = get())
    }
}
