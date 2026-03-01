package tech.arnav.twofac.cli.di

import org.koin.core.qualifier.named
import org.koin.dsl.module
import tech.arnav.twofac.cli.storage.AppDirUtils
import tech.arnav.twofac.cli.storage.FileStorage
import tech.arnav.twofac.lib.storage.MemoryStorage
import tech.arnav.twofac.lib.storage.Storage

val testStorageModule = module {


    single<Storage> {
        MemoryStorage()
    }
}
