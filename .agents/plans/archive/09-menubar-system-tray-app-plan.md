---
name: System Tray / Menu Bar Application (Desktop)
status: Completed
progress:
  - "[✓] Phase 0 - Asset Preparation and Desktop-Only Settings"
  - "[✓] Phase 1 - Basic System Tray implementation in Compose"
  - "[✓] Phase 2 - Dual Window Management (Main App vs Tray Popup)"
  - "[✓] Phase 3 - Dynamic Window Positioning Logic for Popup"
  - "[✓] Phase 4 - App State and UI Refinement for Tray context"
---

# System Tray / Menu Bar Application Plan

## Goal
Enhance the existing Desktop target (Windows, macOS, Linux) to support a System Tray / Menu Bar icon and popup. Crucially, the app must remain a **standard desktop application**: when launched from the OS launcher (Dock/Start Menu), it will open a standard decorated window. The tray icon is an optional, desktop-only setting that users can toggle. When the tray icon is enabled and clicked, it displays the core Compose UI as an undecorated popup anchored to the icon's location.

## Architecture and Technical Decisions

Based on research, we will achieve this using **99% pure Kotlin & Compose Multiplatform**, relying on the underlying JVM (AWT/Swing) when necessary for OS-level coordinate tracking.

1. **Standard Main Window**: The app's primary entry point (`application { Window(...) }`) remains a standard, resizable, decorated OS window.
2. **Desktop-Only Setting**: A toggle will be added to the Settings screen specifically for the `desktopMain` source set. The label for this setting will dynamically reflect the host OS:
   - **macOS**: "Show Menu Bar icon"
   - **Windows**: "Show System Tray icon"
   - **Linux**: "Show System Tray / AppIndicator icon"
3. **Compose Multiplatform `Tray` API**: When the setting is enabled, we use the built-in `Tray` composable within the `application` scope to add the icon to the OS.
4. **Undecorated Popup Window**: Clicking the tray icon opens a *secondary* `Window(undecorated = true, transparent = true)`. This removes standard OS title bars, allowing us to draw a custom panel with rounded corners and shadows that looks like a native tray menu.
5. **Anchor Positioning via AWT**: Compose `Tray` does not directly expose the (x,y) screen coordinates of the tray icon. To position the popup window correctly, we will capture the mouse cursor position at the exact moment of the click event using Java AWT: `java.awt.MouseInfo.getPointerInfo().location`. We will calculate the optimal window placement based on screen bounds (`java.awt.GraphicsEnvironment`).
6. **Dismiss on Focus Loss**: The tray popup needs to feel transient. We will utilize `Window` focus state listening (or `Window.onFocusLost`) to automatically hide the tray popup when the user clicks elsewhere.

---

## Step-by-Step Implementation Roadmap

### Phase 0 - Asset Preparation and Desktop-Only Settings
1. ✅ **Assets**: Prepare monochrome/template icons for the macOS Menu Bar (they must respond to dark/light mode automatically) and standard colored icons for Windows/Linux. Place these in `composeApp/src/desktopMain/resources`.
2. ✅ **Settings Interface**: Introduce a `DesktopSettingsManager` (or extend `SessionManager`) in `desktopMain` to persist the boolean `isTrayIconEnabled`.
3. ✅ **Dynamic OS Strings**: Create a helper function in `desktopMain` to detect the OS (`System.getProperty("os.name")`) and return the technically correct string for the Settings UI:
   - ✅ "Menu Bar" for macOS
   - ✅ "System Tray" for Windows
   - ✅ "System Tray / AppIndicator" for Linux
4. ✅ **Settings UI Integration**: Inject this setting into the `SettingsScreen` only when running on the Desktop target.

### Phase 1 - Basic System Tray implementation in Compose
1. ✅ Modify `composeApp/src/desktopMain/kotlin/tech/arnav/twofac/main.kt`.
2. ✅ Read `isTrayIconEnabled` from the settings store.
3. ✅ If enabled, introduce a `Tray` composable inside the `application` block.
4. ✅ Add a basic right-click fallback menu (e.g., "Open TwoFac", "Quit TwoFac") directly to the `Tray`.
5. ✅ Add an `onAction` callback (left-click on macOS/Windows) to toggle the visibility state of the Tray Popup window.

### Phase 2 - Dual Window Management (Main App vs Tray Popup)
1. ✅ **Main Window**: Continue showing the standard `Window` on launch. If the user closes the main window but the tray icon is enabled, the app should *not* exit (manage application state/exit constraints manually).
2. ✅ **Tray Popup Window**: Declare a *second* `Window` in `main.kt` specifically for the Tray Mode.
3. ✅ Configure the Tray Popup `Window`:
   - ✅ `undecorated = true`
   - ✅ `transparent = true`
   - ✅ `resizable = false`
   - ✅ `alwaysOnTop = true`
   - ✅ `visible = isTrayPopupVisible`
4. ✅ Implement focus loss logic: when the Tray Popup `Window` loses focus (`window.isActive == false` or similar AWT listener), automatically set `isTrayPopupVisible` to false.

### Phase 3 - Dynamic Window Positioning Logic for Popup
1. ✅ Create a positioning helper class `TrayPositionCalculator` in `desktopMain`.
2. ✅ In the `Tray` click handler, capture `java.awt.MouseInfo.getPointerInfo().location`.
3. ✅ Fetch the current screen's usable bounds via `GraphicsEnvironment.getLocalGraphicsEnvironment().maximumWindowBounds`.
4. ✅ Determine the taskbar position (e.g., if the mouse is at the very top of the screen, it's a macOS menubar or top-aligned Linux panel; if at the bottom, it's Windows).
5. ✅ Adjust the `WindowState.position` of the Tray Popup dynamically to spawn aligned with the tray icon but strictly contained within the screen bounds.

### Phase 4 - App State and UI Refinement for Tray context
1. ✅ Ensure the `composeApp` `App()` scales correctly to a fixed, compact mobile-like dimension (e.g., 360x500px) when rendered inside the Tray Popup.
2. ✅ Ensure data and state (like loaded accounts) stay perfectly synced between the Main Window and the Tray Popup.
3. ✅ Wire up the "Quit" button within the Settings or right-click menu to correctly tear down the entire application, ignoring the tray persistence rule.

---

## Risks and Limitations
- **Linux Compatibility**: Some Linux desktop environments (like older GNOME) handle tray icons (AppIndicators) poorly. If click events are swallowed by the DE, we might have to rely exclusively on the right-click menu to trigger the "Open Window" action.
- **macOS Dock Icon**: Because the app is a standard app with a regular window on startup, it *will* have a Dock icon. We will explicitly avoid using the `<key>LSUIElement</key><string>true</string>` hack globally, as that would hide the Dock icon even for the Main Window, leading to confusing UX. The app will behave like Discord/Slack (lives in the Dock while the main window is open, can persist in the Menu Bar when closed).
- **Focus Stealing**: Maintaining proper Z-ordering and closing the window exactly when it loses focus requires careful testing across Windows, macOS, and different Linux display servers (Wayland vs X11).
