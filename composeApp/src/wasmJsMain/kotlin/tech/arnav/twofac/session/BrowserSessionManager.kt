package tech.arnav.twofac.session

/**
 * Browser-extension implementation of [SessionManager].
 *
 * Uses the browser's `localStorage` to persist the "remember passkey" preference
 * and the passkey itself so the user is not prompted every time the extension is opened.
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

    override fun getSavedPasskey(): String? {
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

@JsFun("(key) => { const v = window.localStorage.getItem(key); return v === null ? '' : v; }")
private external fun localStorageGetItemRaw(key: String): String

@JsFun("(key, value) => { window.localStorage.setItem(key, value); }")
private external fun localStorageSetItem(key: String, value: String)

@JsFun("(key) => { window.localStorage.removeItem(key); }")
private external fun localStorageRemoveItem(key: String)

@JsFun("(key) => { return window.localStorage.getItem(key) !== null; }")
private external fun localStorageHasItem(key: String): Boolean

private fun localStorageGetItem(key: String): String? {
    return if (localStorageHasItem(key)) localStorageGetItemRaw(key) else null
}

@JsFun("() => { try { window.localStorage.setItem('twofac_ls_test', '1'); window.localStorage.removeItem('twofac_ls_test'); return true; } catch(e) { return false; } }")
private external fun isLocalStorageAccessible(): Boolean
