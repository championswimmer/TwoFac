package tech.arnav.twofac

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import tech.arnav.twofac.di.desktopBackupModule
import tech.arnav.twofac.di.desktopQrModule

fun main() {
    initKoin {
        modules(desktopBackupModule, desktopQrModule)
    }

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "TwoFac",
            icon = painterResource("twofac_icon.png"),
        ) {
            App()
        }
    }
}
