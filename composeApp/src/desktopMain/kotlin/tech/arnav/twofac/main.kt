package tech.arnav.twofac

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.runBlocking
import tech.arnav.twofac.di.desktopBackupModule
import tech.arnav.twofac.di.desktopQrModule
import tech.arnav.twofac.di.desktopSettingsModule
import tech.arnav.twofac.settings.DesktopSettingsManager
import androidx.compose.foundation.isSystemInDarkTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import twofac.composeapp.generated.resources.Res
import twofac.composeapp.generated.resources.tray_lock_color
import twofac.composeapp.generated.resources.tray_lock_monochrome_dark
import twofac.composeapp.generated.resources.tray_lock_monochrome_light
import twofac.composeapp.generated.resources.twofac_icon

fun main() = runBlocking {
    val koinApp = initKoin {
        modules(desktopBackupModule, desktopQrModule, desktopSettingsModule)
    }
    val settingsManager = koinApp.koin.get<DesktopSettingsManager>()
    val initialTrayEnabled = try {
        settingsManager.isTrayIconEnabled()
    } catch (e: Exception) {
        println("Failed to fetch initial tray icon setting: ${e.message}")
        false
    }

    application {
        val isTrayEnabled by settingsManager.isTrayIconEnabledFlow.collectAsState(initial = initialTrayEnabled)
        var isMainWindowOpen by remember { mutableStateOf(true) }
        var isTrayPopupVisible by remember { mutableStateOf(false) }
        
        val trayWindowState = rememberWindowState(
            width = 360.dp,
            height = 500.dp,
        )

        if (isMainWindowOpen) {
            Window(
                onCloseRequest = {
                    isMainWindowOpen = false
                    if (!isTrayEnabled) {
                        exitApplication()
                    }
                },
                title = "TwoFac",
                icon = painterResource(Res.drawable.twofac_icon),
            ) {
                App(onQuit = { exitApplication() })
            }
        } else if (!isTrayEnabled) {
            exitApplication()
        }

        if (isTrayEnabled) {
            val os = System.getProperty("os.name").lowercase()
            val isMac = os.contains("mac")
            val isLinux = os.contains("linux") || os.contains("nix")
            val isDark = isSystemInDarkTheme()
            val trayIcon: DrawableResource = if (isMac || isLinux) {
                if (isDark) Res.drawable.tray_lock_monochrome_dark else Res.drawable.tray_lock_monochrome_light
            } else {
                Res.drawable.tray_lock_color
            }

            Tray(
                icon = painterResource(trayIcon),
                tooltip = "TwoFac",
                onAction = {
                    if (!isTrayPopupVisible) {
                        trayWindowState.position = TrayPositionCalculator.calculatePopupPosition(trayWindowState.size)
                    }
                    isTrayPopupVisible = !isTrayPopupVisible
                },
                menu = {
                    Item(
                        text = "Open TwoFac",
                        onClick = {
                            isMainWindowOpen = true
                            isTrayPopupVisible = false
                        }
                    )
                    Item(
                        text = "Quit TwoFac",
                        onClick = ::exitApplication
                    )
                }
            )

            Window(
                onCloseRequest = { isTrayPopupVisible = false },
                state = trayWindowState,
                visible = isTrayPopupVisible,
                undecorated = true,
                transparent = true,
                resizable = false,
                alwaysOnTop = true,
                title = "TwoFac Tray",
            ) {
                val window = this.window
                DisposableEffect(window) {
                    val listener = object : java.awt.event.WindowFocusListener {
                        override fun windowGainedFocus(e: java.awt.event.WindowEvent?) {}
                        override fun windowLostFocus(e: java.awt.event.WindowEvent?) {
                            isTrayPopupVisible = false
                        }
                    }
                    window.addWindowFocusListener(listener)
                    onDispose {
                        window.removeWindowFocusListener(listener)
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.background,
                    shadowElevation = 8.dp
                ) {
                    App(onQuit = { exitApplication() })
                }
            }
        }
    }
}
