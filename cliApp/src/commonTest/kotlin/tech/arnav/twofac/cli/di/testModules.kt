package tech.arnav.twofac.cli.di

import org.koin.dsl.module
import tech.arnav.twofac.lib.storage.MemoryStorage
import tech.arnav.twofac.lib.storage.Storage

val testStorageModule = module {


    single<Storage> {
        MemoryStorage()
    }
}
