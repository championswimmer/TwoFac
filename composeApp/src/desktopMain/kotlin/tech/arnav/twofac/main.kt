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
import androidx.compose.ui.res.painterResource
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
import javax.swing.JDialog
import javax.swing.JMenuItem
import javax.swing.JPopupMenu
import javax.swing.SwingUtilities
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import androidx.compose.foundation.isSystemInDarkTheme
import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect

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
                icon = painterResource("twofac_icon.png"),
            ) {
                App(onQuit = { exitApplication() })
            }
        } else if (!isTrayEnabled) {
            exitApplication()
        }

        if (isTrayEnabled) {
            val os = System.getProperty("os.name").lowercase()
            val isMac = os.contains("mac")
            val isDark = isSystemInDarkTheme()
            val trayIconPath = if (isMac) {
                if (isDark) "tray_lock_monochrome_dark.svg" else "tray_lock_monochrome_light.svg"
            } else {
                "tray_lock_color.svg"
            }

            var ignoreNextAction by remember { mutableStateOf(false) }
            val closeApp = { exitApplication() }
            
            LaunchedEffect(isMac, isTrayEnabled) {
                if (isMac && isTrayEnabled) {
                    delay(500)
                    val tray = java.awt.SystemTray.getSystemTray()
                    if (tray.trayIcons.isNotEmpty()) {
                        val trayIcon = tray.trayIcons.last()
                        
                        val popup = JPopupMenu()
                        val openItem = JMenuItem("Open TwoFac")
                        openItem.addActionListener { 
                            isMainWindowOpen = true
                            isTrayPopupVisible = false
                        }
                        val quitItem = JMenuItem("Quit TwoFac")
                        quitItem.addActionListener { 
                            closeApp() 
                        }
                        popup.add(openItem)
                        popup.add(quitItem)

                        val hiddenDialog = JDialog()
                        hiddenDialog.isUndecorated = true
                        hiddenDialog.setSize(0, 0)

                        // Using a boolean array to safely toggle ignore state from the listener
                        val ignoreState = BooleanArray(1) { false }

                        trayIcon.addMouseListener(object : MouseAdapter() {
                            override fun mousePressed(e: MouseEvent) {
                                if (SwingUtilities.isRightMouseButton(e) || e.isPopupTrigger) {
                                    ignoreNextAction = true
                                    hiddenDialog.location = e.locationOnScreen
                                    hiddenDialog.isVisible = true
                                    popup.show(hiddenDialog, 0, 0)
                                    
                                    popup.addPopupMenuListener(object : javax.swing.event.PopupMenuListener {
                                        override fun popupMenuWillBecomeVisible(e: javax.swing.event.PopupMenuEvent?) {}
                                        override fun popupMenuWillBecomeInvisible(e: javax.swing.event.PopupMenuEvent?) {
                                            hiddenDialog.isVisible = false
                                        }
                                        override fun popupMenuCanceled(e: javax.swing.event.PopupMenuEvent?) {
                                            hiddenDialog.isVisible = false
                                        }
                                    })
                                }
                            }
                        })
                    }
                }
            }

            if (isMac) {
                Tray(
                    icon = painterResource(trayIconPath),
                    tooltip = "TwoFac",
                    onAction = {
                        if (ignoreNextAction) {
                            ignoreNextAction = false
                            return@Tray
                        }
                        if (!isTrayPopupVisible) {
                            trayWindowState.position = TrayPositionCalculator.calculatePopupPosition(trayWindowState.size)
                        }
                        isTrayPopupVisible = !isTrayPopupVisible
                    }
                )
            } else {
                Tray(
                    icon = painterResource(trayIconPath),
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
            }

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
