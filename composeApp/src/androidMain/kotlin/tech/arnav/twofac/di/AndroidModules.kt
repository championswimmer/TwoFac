package tech.arnav.twofac.di

import android.content.Context
import org.koin.dsl.module
import tech.arnav.twofac.wear.AndroidWatchSyncCoordinator
import tech.arnav.twofac.wear.WatchSyncCoordinator
import tech.arnav.twofac.wear.WearSyncDataLayerClient

fun androidWearSyncModule(appContext: Context) = module {
    single { appContext }
    single { WearSyncDataLayerClient(context = get()) }
    single<WatchSyncCoordinator> {
        AndroidWatchSyncCoordinator(
            appContext = get(),
            twoFacLib = get(),
            dataLayerClient = get(),
        )
    }
}
