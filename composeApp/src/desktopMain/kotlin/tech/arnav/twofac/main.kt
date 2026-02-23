package tech.arnav.twofac

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import tech.arnav.twofac.di.desktopBackupModule
import tech.arnav.twofac.screens.DesktopBackupSettings

fun main() {
    initKoin {
        modules(desktopBackupModule)
    }

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "twofac",
        ) {
            App(
                backupSettingsContent = { DesktopBackupSettings() }
            )
        }
    }
}
