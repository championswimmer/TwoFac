package tech.arnav.twofac.cli.di

import org.koin.core.qualifier.named
import org.koin.dsl.module
import tech.arnav.twofac.cli.storage.AppDirUtils
import tech.arnav.twofac.cli.storage.FileStorage
import tech.arnav.twofac.lib.TwoFacLib
import tech.arnav.twofac.lib.storage.Storage

val storageModule = module {

    single(named<StorageFilePath>()) {
        AppDirUtils.getStorageFilePath(forceCreate = true)
    }

    single<Storage> {
        FileStorage(get(named<StorageFilePath>()))
    }
}

val appModule = module {

    single<TwoFacLib> {
        TwoFacLib.initialise(
            storage = get(),
        )
    }
}