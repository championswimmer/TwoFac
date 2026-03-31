package tech.arnav.twofac.di

import android.content.Context
import androidx.fragment.app.FragmentActivity
import org.koin.core.qualifier.named
import org.koin.dsl.binds
import org.koin.dsl.module
import tech.arnav.twofac.onboarding.PlatformOnboardingStepContributor
import tech.arnav.twofac.onboarding.SecureUnlockOnboardingContributor
import twofac.composeapp.generated.resources.Res
import twofac.composeapp.generated.resources.onboarding_step_secure_unlock_android_description
import tech.arnav.twofac.backup.GoogleDriveAppDataBackupTransport
import tech.arnav.twofac.backup.LocalFileBackupTransport
import tech.arnav.twofac.companion.CompanionSyncCoordinator
import tech.arnav.twofac.lib.backup.BackupProviderIds
import tech.arnav.twofac.lib.backup.BackupTransport
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
    single {
        AndroidBiometricSessionManager(
            context = appContext,
            activityProvider = activityProvider,
        )
    } binds arrayOf(
        BiometricSessionManager::class,
        SecureSessionManager::class,
        SessionManager::class,
    )
}

val androidQrModule = module {
    single<CameraQRCodeReader> { AndroidCameraQRCodeReader() }
}

val androidOnboardingModule = module {
    single<PlatformOnboardingStepContributor> { SecureUnlockOnboardingContributor(Res.string.onboarding_step_secure_unlock_android_description) }
}

fun androidBackupModule(
    appContext: Context,
    activityProvider: () -> FragmentActivity,
) = module {
    single<BackupTransport>(named(BackupProviderIds.LOCAL)) {
        LocalFileBackupTransport()
    }
    single<BackupTransport>(named(BackupProviderIds.GOOGLE_DRIVE_APPDATA)) {
        GoogleDriveAppDataBackupTransport(
            appContext = appContext,
            activityProvider = activityProvider,
        )
    }
}
