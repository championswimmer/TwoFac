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

@JsModule("storage.js")
private external fun localStorageGetItem(key: String): String?

@JsModule("storage.js")
private external fun localStorageSetItem(key: String, value: String)

@JsModule("storage.js")
private external fun localStorageRemoveItem(key: String)

@JsModule("storage.js")
private external fun isLocalStorageAccessible(): Boolean
