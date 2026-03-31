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
import tech.arnav.twofac.di.desktopOnboardingModule
import tech.arnav.twofac.di.desktopQrModule
import tech.arnav.twofac.di.desktopSessionModule
import tech.arnav.twofac.di.desktopSettingsModule
import tech.arnav.twofac.settings.DesktopSettingsManager
import tech.arnav.twofac.tray.LinuxNativeTray
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import twofac.composeapp.generated.resources.Res
import twofac.composeapp.generated.resources.tray_lock_color
import twofac.composeapp.generated.resources.tray_lock_monochrome_light
import twofac.composeapp.generated.resources.twofac_icon
import twofac.composeapp.generated.resources.desktop_window_title
import twofac.composeapp.generated.resources.desktop_tray_tooltip
import twofac.composeapp.generated.resources.desktop_tray_open
import twofac.composeapp.generated.resources.desktop_tray_quit
import twofac.composeapp.generated.resources.desktop_tray_popup_title

private val osName = System.getProperty("os.name").lowercase()
private val isMac = osName.contains("mac")
private val isLinux = osName.contains("linux") || osName.contains("nix")

fun main() = runBlocking {
    // On macOS, this enables the system tray icon to be treated as a "template image"
    // (NSImage.isTemplate = true). Template images are pure black + transparent icons
    // that macOS automatically tints for dark/light mode AND dims when the mouse moves
    // to a secondary display. Without this, the icon will not dim on inactive displays.
    System.setProperty("apple.awt.enableTemplateImages", "true")

    val koinApp = initKoin {
        modules(desktopBackupModule, desktopQrModule, desktopSettingsModule, desktopOnboardingModule, desktopSessionModule)
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

        val windowTitle = stringResource(Res.string.desktop_window_title)
        val trayTooltip = stringResource(Res.string.desktop_tray_tooltip)
        val trayOpenText = stringResource(Res.string.desktop_tray_open)
        val trayQuitText = stringResource(Res.string.desktop_tray_quit)
        val trayPopupTitle = stringResource(Res.string.desktop_tray_popup_title)
        
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
                title = windowTitle,
                icon = painterResource(Res.drawable.twofac_icon),
            ) {
                App(onQuit = { exitApplication() })
            }
        } else if (!isTrayEnabled) {
            exitApplication()
        }

        if (isTrayEnabled) {
            if (isLinux) {
                // On Linux, AWT SystemTray uses the legacy xembed protocol which GNOME
                // dropped in 3.26. On Wayland it renders as a white square or is invisible.
                // Use dorkbox/SystemTray which talks native GTK/AppIndicator instead.
                DisposableEffect(Unit) {
                    LinuxNativeTray.show(
                        lightIconPath = "tray_lock_linux_light.png",
                        darkIconPath = "tray_lock_linux_dark.png",
                        tooltip = trayTooltip,
                        openLabel = trayOpenText,
                        quitLabel = trayQuitText,
                        onOpen = { isMainWindowOpen = true },
                        onQuit = ::exitApplication,
                    )
                    onDispose { LinuxNativeTray.shutdown() }
                }
            } else {
                // macOS and Windows — Compose's built-in Tray works correctly.
                val trayIconPainter = when {
                    isMac -> painterResource(Res.drawable.tray_lock_monochrome_light)
                    else -> painterResource(Res.drawable.tray_lock_color)
                }

                Tray(
                    icon = trayIconPainter,
                    tooltip = trayTooltip,
                    onAction = {
                        if (!isTrayPopupVisible) {
                            trayWindowState.position = TrayPositionCalculator.calculatePopupPosition(trayWindowState.size)
                        }
                        isTrayPopupVisible = !isTrayPopupVisible
                    },
                    menu = {
                        Item(
                            text = trayOpenText,
                            onClick = {
                                isMainWindowOpen = true
                                isTrayPopupVisible = false
                            }
                        )
                        Item(
                            text = trayQuitText,
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
                    title = trayPopupTitle,
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
}
