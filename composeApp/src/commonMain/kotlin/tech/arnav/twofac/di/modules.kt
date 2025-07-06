package tech.arnav.twofac.di

import org.koin.dsl.module
import tech.arnav.twofac.lib.TwoFacLib
import tech.arnav.twofac.lib.storage.Storage
import tech.arnav.twofac.storage.FileStorage
import tech.arnav.twofac.storage.createAccountsStore
import tech.arnav.twofac.viewmodels.AccountsViewModel

val storageModule = module {
    single<Storage> {
        FileStorage(createAccountsStore())
    }
}

val appModule = module {
    single<TwoFacLib> {
        TwoFacLib.initialise(storage = get())
    }
}

val viewModelModule = module {
    single<AccountsViewModel> {
        AccountsViewModel(twoFacLib = get())
    }
}