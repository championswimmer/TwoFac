package tech.arnav.twofac.di

import org.koin.core.qualifier.named
import org.koin.dsl.module
import tech.arnav.twofac.backup.ICloudBackupTransport
import tech.arnav.twofac.backup.LocalFileBackupTransport
import tech.arnav.twofac.lib.backup.BackupProviderIds
import tech.arnav.twofac.companion.CompanionSyncCoordinator
import tech.arnav.twofac.companion.IosCompanionSyncCoordinator
import tech.arnav.twofac.lib.backup.BackupTransport
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

val iosBiometricModule = module {
    single<BiometricSessionManager> { IosBiometricSessionManager() }
    single<SecureSessionManager> { get<BiometricSessionManager>() }
    single<SessionManager> { get<BiometricSessionManager>() }
}

val iosQrModule = module {
    single<CameraQRCodeReader> { IosCameraQRCodeReader() }
}

val iosBackupModule = module {
    single<BackupTransport>(named(BackupProviderIds.LOCAL)) {
        LocalFileBackupTransport()
    }
    single<BackupTransport>(named(BackupProviderIds.ICLOUD)) {
        ICloudBackupTransport()
    }
}
