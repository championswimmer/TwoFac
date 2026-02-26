package tech.arnav.twofac.session

/**
 * Manages passkey session persistence.
 *
 * On the browser-extension target this stores the passkey so the user
 * does not have to re-enter it every time the extension popup is opened.
 *
 * ## Security Considerations
 *
 * **Current Implementation (localStorage):**
 * The browser extension currently stores the passkey in **plaintext** in `localStorage`.
 * While `localStorage` is sandboxed to the extension's origin (`chrome-extension://...`),
 * this approach has security limitations:
 * - Vulnerable to XSS attacks if malicious scripts execute in the extension context
 * - Passkey persists on disk, survives browser restarts
 * - No encryption layer protecting the stored passkey
 *
 * **Future Improvement Options:**
 *
 * 1. **Background Service Worker Memory Storage (Recommended for Manifest v3)**
 *    - Store passkey in memory of a persistent background service worker
 *    - Survives popup closes but cleared when browser shuts down
 *    - Not accessible via `localStorage`, reducing XSS attack surface
 *    - Requires implementing message passing between popup and service worker
 *
 * 2. **Browser Credential Management API**
 *    - Use `navigator.credentials.store()` / `navigator.credentials.get()`
 *    - Integrates with browser's password manager
 *    - May provide OS-level encryption and biometric unlock
 *    - Requires user interaction for retrieval (better security UX)
 *
 * 3. **Web Crypto API with IndexedDB**
 *    - Generate a key using Web Crypto API, store encrypted passkey in IndexedDB
 *    - Provides encryption at rest, though key management remains challenging
 *
 * 4. **OS-Provided Secure Storage (Chrome Extension APIs)**
 *    - Some platforms expose native secure storage through Chrome APIs
 *    - May not be universally available across all platforms
 *
 * For now, this implementation prioritizes **user convenience** with an **opt-in** model
 * and explicit security warnings in the UI. Future versions should migrate to a
 * service worker memory model or credential management API for improved security.
 */
interface SessionManager {
    /** Whether this platform supports session persistence at all. */
    fun isAvailable(): Boolean

    /** Whether the user has opted in to remembering the passkey. */
    fun isRememberPasskeyEnabled(): Boolean

    /** Toggle the "remember passkey" preference. Disabling also clears any saved passkey. */
    fun setRememberPasskey(enabled: Boolean)

    /** Whether biometric authentication is available on this platform/device. */
    fun isBiometricAvailable(): Boolean = false

    /** Whether biometric authentication is enabled by user preference. */
    fun isBiometricEnabled(): Boolean = false

    /** Toggle biometric authentication. */
    fun setBiometricEnabled(enabled: Boolean) = Unit

    /** Return the previously saved passkey, or null if none is stored or the feature is disabled. */
    suspend fun getSavedPasskey(): String?

    /** Persist the passkey (only effective when the feature is enabled). */
    fun savePasskey(passkey: String)

    /** Remove any stored passkey. */
    fun clearPasskey()
}
