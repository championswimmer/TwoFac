package tech.arnav.twofac.di

import org.koin.core.qualifier.named
import org.koin.dsl.module
import tech.arnav.twofac.backup.LocalFileBackupTransport
import tech.arnav.twofac.lib.backup.BackupTransport
import tech.arnav.twofac.onboarding.DesktopOnboardingContributor
import tech.arnav.twofac.onboarding.PlatformOnboardingStepContributor
import tech.arnav.twofac.qr.ClipboardQRCodeReader
import tech.arnav.twofac.qr.DesktopClipboardQRCodeReader
import tech.arnav.twofac.settings.DesktopSettingsManager

val desktopBackupModule = module {
    single<BackupTransport>(named("local")) {
        LocalFileBackupTransport()
    }
}

val desktopQrModule = module {
    single<ClipboardQRCodeReader> { DesktopClipboardQRCodeReader() }
}

val desktopSettingsModule = module {
    single<DesktopSettingsManager> { DesktopSettingsManager() }
}

val desktopOnboardingModule = module {
    single<PlatformOnboardingStepContributor> { DesktopOnboardingContributor() }
}
