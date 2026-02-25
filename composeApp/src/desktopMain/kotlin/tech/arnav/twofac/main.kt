package tech.arnav.twofac

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import tech.arnav.twofac.di.desktopBackupModule

fun main() {
    initKoin {
        modules(desktopBackupModule)
    }

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "twofac",
            icon = painterResource("twofac_icon.png"),
        ) {
            App()
        }
    }
}