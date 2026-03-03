package tech.arnav.twofac.di

import android.content.Context
import androidx.fragment.app.FragmentActivity
import org.koin.dsl.module
import tech.arnav.twofac.companion.CompanionSyncCoordinator
import tech.arnav.twofac.qr.AndroidCameraQRCodeReader
import tech.arnav.twofac.qr.CameraQRCodeReader
import tech.arnav.twofac.session.AndroidBiometricSessionManager
import tech.arnav.twofac.session.BiometricSessionManager
import tech.arnav.twofac.session.SecureSessionManager
import tech.arnav.twofac.session.SessionManager
import tech.arnav.twofac.wear.AndroidWatchSyncCoordinator
import tech.arnav.twofac.wear.WearSyncDataLayerClient

fun androidWearSyncModule(appContext: Context) = module {
    single { appContext }
    single { WearSyncDataLayerClient(context = get()) }
    single<CompanionSyncCoordinator> {
        AndroidWatchSyncCoordinator(
            appContext = get(),
            twoFacLib = get(),
            dataLayerClient = get(),
        )
    }
}

fun androidBiometricModule(
    appContext: Context,
    activityProvider: () -> FragmentActivity,
) = module {
    single<BiometricSessionManager> {
        AndroidBiometricSessionManager(
            context = appContext,
            activityProvider = activityProvider,
        )
    }
    single<SecureSessionManager> { get<BiometricSessionManager>() }
    single<SessionManager> { get<BiometricSessionManager>() }
}

val androidQrModule = module {
    single<CameraQRCodeReader> { AndroidCameraQRCodeReader() }
}
