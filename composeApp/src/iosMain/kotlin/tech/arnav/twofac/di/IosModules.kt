package tech.arnav.twofac.di

import org.koin.dsl.module
import tech.arnav.twofac.backup.ICloudBackupTransport
import tech.arnav.twofac.companion.CompanionSyncCoordinator
import tech.arnav.twofac.companion.IosCompanionSyncCoordinator
import tech.arnav.twofac.lib.backup.BackupProvider
import tech.arnav.twofac.lib.backup.BackupTransportRegistry
import tech.arnav.twofac.lib.backup.ICloudBackupProviderInfo
import tech.arnav.twofac.lib.backup.backupTransportRegistryOf
import tech.arnav.twofac.qr.CameraQRCodeReader
import tech.arnav.twofac.qr.IosCameraQRCodeReader
import tech.arnav.twofac.session.BiometricSessionManager
import tech.arnav.twofac.session.IosBiometricSessionManager
import tech.arnav.twofac.session.SecureSessionManager
import tech.arnav.twofac.session.SessionManager

fun iosCompanionSyncModule() = module {
    single<CompanionSyncCoordinator> {
        IosCompanionSyncCoordinator(twoFacLib = get())
    }
}

val iosBackupModule = module {
    single<BackupTransportRegistry> {
        backupTransportRegistryOf(
            BackupProvider(
                info = ICloudBackupProviderInfo,
                transport = ICloudBackupTransport(),
            )
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
