package tech.arnav.twofac.di

import android.content.Context
import androidx.fragment.app.FragmentActivity
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.dsl.module
import tech.arnav.twofac.backup.GoogleDriveBackupTransport
import tech.arnav.twofac.companion.CompanionSyncCoordinator
import tech.arnav.twofac.lib.backup.BackupProvider
import tech.arnav.twofac.lib.backup.BackupTransportRegistry
import tech.arnav.twofac.lib.backup.GoogleDriveAppDataBackupProviderInfo
import tech.arnav.twofac.lib.backup.backupTransportRegistryOf
import tech.arnav.twofac.qr.AndroidCameraQRCodeReader
import tech.arnav.twofac.qr.CameraQRCodeReader
import tech.arnav.twofac.session.AndroidBiometricSessionManager
import tech.arnav.twofac.session.BiometricSessionManager
import tech.arnav.twofac.session.SecureSessionManager
import tech.arnav.twofac.session.SessionManager
import tech.arnav.twofac.storage.createGoogleDriveAuthStore
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

val androidBackupModule = module {
    single {
        HttpClient(OkHttp)
    }
    single<BackupTransportRegistry> {
        backupTransportRegistryOf(
            BackupProvider(
                info = GoogleDriveAppDataBackupProviderInfo,
                transport = GoogleDriveBackupTransport(
                    authStore = createGoogleDriveAuthStore(),
                    httpClient = get(),
                ),
            ),
        )
    }
}
