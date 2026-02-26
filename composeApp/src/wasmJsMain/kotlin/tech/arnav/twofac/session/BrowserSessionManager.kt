package tech.arnav.twofac.session

/**
 * Browser-extension implementation of [SessionManager].
 *
 * Uses the browser's `localStorage` to persist the "remember passkey" preference
 * and the passkey itself so the user is not prompted every time the extension is opened.
 *
 * ⚠️ **SECURITY WARNING: Plaintext Storage** ⚠️
 *
 * This implementation stores the user's passkey in **plaintext** in `localStorage`.
 * While `localStorage` is origin-sandboxed to `chrome-extension://...`, this approach
 * has known security limitations:
 *
 * **Risks:**
 * - **XSS Vulnerability:** Malicious scripts executing in the extension context can read `localStorage`
 * - **Persistence:** Passkey survives browser restarts and is written to disk
 * - **No Encryption:** No cryptographic protection for the stored passkey
 *
 * **Future Improvements to Consider:**
 *
 * 1. **Service Worker Memory Storage:**
 *    - Store passkey in the memory of a background service worker (Manifest v3)
 *    - Passkey cleared when browser closes, reducing exposure window
 *    - Not accessible via DOM/XSS, safer than `localStorage`
 *    - Implementation: Use service worker as state holder, message passing for popup access
 *
 * 2. **Browser Credential Management API / Web Authentication (WebAuthn):**
 *    - Use `navigator.credentials` API or platform authenticators
 *    - Leverage OS-level secure storage and biometric authentication
 *    - Passkey never exposed to JavaScript as plaintext
 *    - Better user experience with browser-integrated password managers
 *
 * 3. **Encrypted Storage (IndexedDB + Web Crypto):**
 *    - Derive encryption key from user interaction (e.g., PIN) using Web Crypto API
 *    - Store encrypted passkey in IndexedDB
 *    - Adds encryption layer, though key management remains a challenge
 *
 * **Current Trade-off:**
 * This implementation prioritizes **user convenience** for an opt-in feature with
 * explicit UI warnings. Users must acknowledge the risk ("Only enable this on devices you trust").
 * Migrating to service worker memory or credential APIs is recommended for future releases.
 *
 * @see SessionManager for architectural security discussion
 */
class BrowserSessionManager : SessionManager {

    companion object {
        private const val REMEMBER_PASSKEY_KEY = "twofac_remember_passkey"
        private const val SESSION_PASSKEY_KEY = "twofac_session_passkey"
    }

    override fun isAvailable(): Boolean = isLocalStorageAccessible()

    override fun isRememberPasskeyEnabled(): Boolean {
        return try {
            localStorageGetItem(REMEMBER_PASSKEY_KEY) == "true"
        } catch (_: Throwable) {
            false
        }
    }

    override fun setRememberPasskey(enabled: Boolean) {
        try {
            if (enabled) {
                localStorageSetItem(REMEMBER_PASSKEY_KEY, "true")
            } else {
                localStorageRemoveItem(REMEMBER_PASSKEY_KEY)
                clearPasskey()
            }
        } catch (_: Throwable) {
            // localStorage inaccessible – silently ignore
        }
    }

    override suspend fun getSavedPasskey(): String? {
        return try {
            if (!isRememberPasskeyEnabled()) null
            else localStorageGetItem(SESSION_PASSKEY_KEY)
        } catch (_: Throwable) {
            null
        }
    }

    override fun savePasskey(passkey: String) {
        try {
            if (isRememberPasskeyEnabled()) {
                localStorageSetItem(SESSION_PASSKEY_KEY, passkey)
            }
        } catch (_: Throwable) {
            // localStorage inaccessible – silently ignore
        }
    }

    override fun clearPasskey() {
        try {
            localStorageRemoveItem(SESSION_PASSKEY_KEY)
        } catch (_: Throwable) {
            // localStorage inaccessible – silently ignore
        }
    }
}

/* ---- thin JS interop wrappers around window.localStorage ---- */

@JsFun("(key) => window.localStorage.getItem(key)")
private external fun localStorageGetItem(key: String): String?

@JsFun("(key, value) => { window.localStorage.setItem(key, value); }")
private external fun localStorageSetItem(key: String, value: String)

@JsFun("(key) => { window.localStorage.removeItem(key); }")
private external fun localStorageRemoveItem(key: String)

@JsFun("() => { try { window.localStorage.setItem('twofac_ls_test', '1'); window.localStorage.removeItem('twofac_ls_test'); return true; } catch(e) { return false; } }")
private external fun isLocalStorageAccessible(): Boolean
