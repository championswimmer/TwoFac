package tech.arnav.twofac.session

/**
 * Represents the secure unlock mode available on a desktop platform.
 *
 * This enum captures the reality that desktop OSes have different capabilities
 * for protecting secrets at rest and gating access with user presence.
 */
enum class DesktopSecureUnlockMode {
    /** No secure unlock available - manual passkey entry only */
    NONE,

    /** macOS Keychain with biometric (Touch ID) or device passcode protection */
    MACOS_KEYCHAIN_BIOMETRIC,

    /** Windows DPAPI-protected storage (user-profile bound) */
    WINDOWS_SECURE_STORAGE,

    /** Windows DPAPI with explicit Windows Hello consent gate */
    WINDOWS_SECURE_STORAGE_WITH_CONSENT,

    /** Linux Secret Service (gnome-keyring/KWallet) - session-bound */
    LINUX_SECRET_SERVICE,
}
