package tech.arnav.twofac

import androidx.compose.ui.window.ComposeUIViewController
import tech.arnav.twofac.di.iosCompanionSyncModule

fun MainViewController() = ComposeUIViewController {
    initKoin {
        modules(iosCompanionSyncModule())
    }
    App()
}
