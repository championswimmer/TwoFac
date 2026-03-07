package tech.arnav.twofac.di

import org.koin.dsl.module
import tech.arnav.twofac.backup.BackupPreferencesManager
import tech.arnav.twofac.companion.CompanionSyncCoordinator
import tech.arnav.twofac.lib.TwoFacLib
import tech.arnav.twofac.lib.backup.BackupService
import tech.arnav.twofac.lib.storage.Storage
import tech.arnav.twofac.qr.CameraQRCodeReader
import tech.arnav.twofac.qr.ClipboardQRCodeReader
import tech.arnav.twofac.storage.createBackupPreferencesStore
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
    single<BackupService> {
        BackupService(get())
    }
    single<BackupPreferencesManager> {
        BackupPreferencesManager(createBackupPreferencesStore())
    }
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
}
