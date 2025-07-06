package tech.arnav.twofac

import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import tech.arnav.twofac.di.appModule
import tech.arnav.twofac.di.storageModule
import tech.arnav.twofac.di.viewModelModule

fun initKoin(appDeclaration: KoinAppDeclaration = {}) =
    startKoin {
        appDeclaration()
        modules(
            storageModule,
            appModule,
            viewModelModule
        )
    }