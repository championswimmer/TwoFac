package tech.arnav.twofac

import androidx.compose.ui.window.ComposeUIViewController
import tech.arnav.twofac.di.iosBackupModule
import tech.arnav.twofac.di.iosBiometricModule
import tech.arnav.twofac.di.iosCompanionSyncModule
import tech.arnav.twofac.di.iosQrModule

fun MainViewController() = ComposeUIViewController {
    initKoin {
        modules(
            iosCompanionSyncModule(),
            iosBiometricModule,
            iosQrModule,
            iosBackupModule,
        )
    }
    App()
}
