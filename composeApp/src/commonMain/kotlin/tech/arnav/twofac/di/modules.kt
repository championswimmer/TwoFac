package tech.arnav.twofac.di

import org.koin.dsl.module
import tech.arnav.twofac.companion.CompanionSyncCoordinator
import tech.arnav.twofac.lib.TwoFacLib
import tech.arnav.twofac.lib.backup.BackupService
import tech.arnav.twofac.lib.backup.BackupTransport
import tech.arnav.twofac.lib.backup.BackupTransportRegistry
import tech.arnav.twofac.onboarding.BaseCommonOnboardingStepContributor
import tech.arnav.twofac.onboarding.CommonOnboardingStepContributor
import tech.arnav.twofac.onboarding.DefaultOnboardingGuideContextProvider
import tech.arnav.twofac.onboarding.OnboardingAutoShowResolver
import tech.arnav.twofac.onboarding.OnboardingGuideContextProvider
import tech.arnav.twofac.onboarding.OnboardingGuideRegistry
import tech.arnav.twofac.onboarding.OnboardingProgressRepository
import tech.arnav.twofac.onboarding.PlatformOnboardingStepContributor
import tech.arnav.twofac.onboarding.OnboardingProgressStore
import tech.arnav.twofac.onboarding.createOnboardingProgressStore
import tech.arnav.twofac.lib.storage.Storage
import tech.arnav.twofac.qr.CameraQRCodeReader
import tech.arnav.twofac.qr.ClipboardQRCodeReader
import tech.arnav.twofac.storage.FileStorage
import tech.arnav.twofac.storage.createAccountsStore
import tech.arnav.twofac.viewmodels.AccountsViewModel
import tech.arnav.twofac.viewmodels.OnboardingViewModel

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

val backupModule = module {
    single<BackupTransportRegistry> {
        BackupTransportRegistry(
            transports = getAll<BackupTransport>(),
        )
    }
    single<BackupService> {
        BackupService(
            twoFacLib = get(),
            transportRegistry = get(),
        )
    }
}

val onboardingModule = module {
    single<CommonOnboardingStepContributor> { BaseCommonOnboardingStepContributor() }
    single { createOnboardingProgressStore() }
    single { OnboardingProgressStore(store = get()) }
    single<OnboardingProgressRepository> { get<OnboardingProgressStore>() }
    single<OnboardingGuideContextProvider> {
        DefaultOnboardingGuideContextProvider(
            twoFacLib = get(),
            sessionManager = getOrNull(),
            cameraQRCodeReader = getOrNull(),
            clipboardQRCodeReader = getOrNull(),
            backupService = getOrNull(),
            companionSyncCoordinator = getOrNull(),
        )
    }
    single {
        OnboardingGuideRegistry(
            commonContributors = getAll<CommonOnboardingStepContributor>(),
            platformContributors = getAll<PlatformOnboardingStepContributor>(),
        )
    }
    single { OnboardingAutoShowResolver() }
}

val viewModelModule = module {
    single<AccountsViewModel> {
        AccountsViewModel(
            twoFacLib = get(),
            companionSyncCoordinator = getOrNull<CompanionSyncCoordinator>(),
            sessionManager = getOrNull(),
            cameraQRCodeReader = getOrNull<CameraQRCodeReader>(),
            clipboardQRCodeReader = getOrNull<ClipboardQRCodeReader>(),
        )
    }
    single<OnboardingViewModel> {
        OnboardingViewModel(
            contextProvider = get(),
            guideRegistry = get(),
            progressStore = get(),
            autoShowResolver = get(),
        )
    }
}
