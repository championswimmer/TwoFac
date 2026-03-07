package tech.arnav.twofac.di

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import org.koin.dsl.module
import tech.arnav.twofac.backup.GoogleDriveBackupTransport
import tech.arnav.twofac.backup.ICloudBackupTransport
import tech.arnav.twofac.companion.CompanionSyncCoordinator
import tech.arnav.twofac.companion.IosCompanionSyncCoordinator
import tech.arnav.twofac.lib.backup.BackupProvider
import tech.arnav.twofac.lib.backup.BackupTransportRegistry
import tech.arnav.twofac.lib.backup.GoogleDriveAppDataBackupProviderInfo
import tech.arnav.twofac.lib.backup.ICloudBackupProviderInfo
import tech.arnav.twofac.lib.backup.backupTransportRegistryOf
import tech.arnav.twofac.qr.CameraQRCodeReader
import tech.arnav.twofac.qr.IosCameraQRCodeReader
import tech.arnav.twofac.session.BiometricSessionManager
import tech.arnav.twofac.session.IosBiometricSessionManager
import tech.arnav.twofac.session.SecureSessionManager
import tech.arnav.twofac.session.SessionManager
import tech.arnav.twofac.storage.createGoogleDriveAuthStore

fun iosCompanionSyncModule() = module {
    single<CompanionSyncCoordinator> {
        IosCompanionSyncCoordinator(twoFacLib = get())
    }
}

val iosBackupModule = module {
    single {
        HttpClient(Darwin)
    }
    single<BackupTransportRegistry> {
        backupTransportRegistryOf(
            BackupProvider(
                info = ICloudBackupProviderInfo,
                transport = ICloudBackupTransport(),
            ),
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

val iosBiometricModule = module {
    single<BiometricSessionManager> { IosBiometricSessionManager() }
    single<SecureSessionManager> { get<BiometricSessionManager>() }
    single<SessionManager> { get<BiometricSessionManager>() }
}

val iosQrModule = module {
    single<CameraQRCodeReader> { IosCameraQRCodeReader() }
}
