@file:OptIn(ExperimentalWasmJsInterop::class)

package tech.arnav.twofac.session.interop

internal interface WebStorageClient {
    fun isAvailable(): Boolean
    fun getItem(key: String): String?
    fun setItem(key: String, value: String)
    fun removeItem(key: String)
}

internal class LocalStorageClient : WebStorageClient {
    override fun isAvailable(): Boolean = isLocalStorageAccessible()

    override fun getItem(key: String): String? = localStorageGetItem(key)

    override fun setItem(key: String, value: String) {
        localStorageSetItem(key, value)
    }

    override fun removeItem(key: String) {
        localStorageRemoveItem(key)
    }
}

@JsFun("(key) => window.localStorage.getItem(key)")
private external fun localStorageGetItem(key: String): String?

@JsFun("(key, value) => { window.localStorage.setItem(key, value); }")
private external fun localStorageSetItem(key: String, value: String)

@JsFun("(key) => { window.localStorage.removeItem(key); }")
private external fun localStorageRemoveItem(key: String)

@JsFun("() => { try { window.localStorage.setItem('twofac_ls_test', '1'); window.localStorage.removeItem('twofac_ls_test'); return true; } catch(e) { return false; } }")
private external fun isLocalStorageAccessible(): Boolean
