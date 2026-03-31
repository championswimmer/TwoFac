package tech.arnav.twofac.tray

import dorkbox.systemTray.MenuItem
import dorkbox.systemTray.SystemTray
import java.awt.event.ActionEvent
import java.io.BufferedReader

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

    /**
     * Detect whether the desktop is using a dark theme by checking GTK and GNOME settings.
     * Returns true if a dark theme is detected, false otherwise.
     */
    private fun isDarkTheme(): Boolean {
        // 1. Check GNOME/freedesktop color-scheme preference (most reliable on modern GNOME)
        try {
            val proc = ProcessBuilder(
                "dbus-send", "--session", "--print-reply=literal",
                "--dest=org.freedesktop.portal.Desktop",
                "/org/freedesktop/portal/desktop",
                "org.freedesktop.portal.Settings.Read",
                "string:org.freedesktop.appearance",
                "string:color-scheme",
            ).redirectErrorStream(true).start()
            val output = proc.inputStream.bufferedReader().use(BufferedReader::readText)
            proc.waitFor()
            // color-scheme: 1 = prefer dark, 2 = prefer light, 0 = no preference
            if (output.contains("uint32 1")) return true
            if (output.contains("uint32 2")) return false
        } catch (_: Exception) {
            // dbus not available, try next method
        }

        // 2. Check gsettings for GTK theme name containing "dark"
        try {
            val proc = ProcessBuilder(
                "gsettings", "get", "org.gnome.desktop.interface", "gtk-theme",
            ).redirectErrorStream(true).start()
            val output = proc.inputStream.bufferedReader().use(BufferedReader::readText).trim()
            proc.waitFor()
            if (output.contains("dark", ignoreCase = true)) return true
        } catch (_: Exception) {
            // gsettings not available
        }

        // 3. Default: assume dark (most modern Linux desktops default to dark panels)
        return true
    }

    fun show(
        lightIconPath: String,
        darkIconPath: String,
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

        // Pick icon based on desktop theme: white icon for dark panels, black for light
        val isDark = isDarkTheme()
        val iconResourcePath = if (isDark) darkIconPath else lightIconPath
        println("LinuxNativeTray: dark theme=$isDark, using icon=$iconResourcePath")

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
