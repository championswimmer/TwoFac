package tech.arnav.twofac.cli.di

import org.koin.dsl.module
import tech.arnav.twofac.cli.storage.AppDirUtils
import tech.arnav.twofac.cli.storage.FileStorage
import tech.arnav.twofac.lib.TwoFacLib
import tech.arnav.twofac.lib.storage.Storage

val storageModule = module {

    single {
        AppDirUtils.getStorageFilePath(forceCreate = true)
    }

    single<Storage> {
        FileStorage(get())
    }
}

val appModule = module {

    single<TwoFacLib> {
        TwoFacLib.initialise(
            storage = get(),
        )
    }
}
