package tech.arnav.twofac.tray

import dorkbox.systemTray.MenuItem
import dorkbox.systemTray.SystemTray
import java.awt.event.ActionEvent

/**
 * Native Linux system tray using dorkbox/SystemTray.
 *
 * Compose Desktop's built-in Tray composable uses AWT SystemTray which relies on the
 * legacy xembed protocol. GNOME 3.26+ dropped native tray support, and on Wayland
 * the AWT tray renders icons as white squares or doesn't show at all.
 *
 * dorkbox/SystemTray uses native GTK/AppIndicator, which is the protocol that modern
 * Linux desktops (GNOME w/ AppIndicator extension, KDE, etc.) actually support.
 */
object LinuxNativeTray {

    private var systemTray: SystemTray? = null

    fun show(
        iconResourcePath: String,
        tooltip: String,
        openLabel: String,
        quitLabel: String,
        onOpen: () -> Unit,
        onQuit: () -> Unit,
    ) {
        if (systemTray != null) return

        val tray = SystemTray.get(tooltip) ?: run {
            println("LinuxNativeTray: SystemTray.get() returned null — tray not supported")
            return
        }

        val iconUrl = LinuxNativeTray::class.java.classLoader.getResource(iconResourcePath)
        if (iconUrl != null) {
            tray.setImage(iconUrl)
        }
        tray.setTooltip(tooltip)
        tray.setStatus(tooltip)

        tray.menu.add(MenuItem(openLabel) { _: ActionEvent -> onOpen() })
        tray.menu.add(MenuItem(quitLabel) { _: ActionEvent ->
            onQuit()
            shutdown()
        })

        systemTray = tray
    }

    fun shutdown() {
        systemTray?.shutdown()
        systemTray = null
    }
}
