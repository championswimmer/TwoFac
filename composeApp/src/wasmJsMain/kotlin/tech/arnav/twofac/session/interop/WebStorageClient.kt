@file:OptIn(ExperimentalWasmJsInterop::class)

package tech.arnav.twofac.session.interop

internal interface WebStorageClient {
    fun isAvailable(): Boolean
    fun getItem(key: String): String?
    fun setItem(key: String, value: String)
    fun removeItem(key: String)
}

internal class LocalStorageClient : WebStorageClient {
    override fun isAvailable(): Boolean = StorageInterop.isLocalStorageAccessible()

    override fun getItem(key: String): String? = StorageInterop.localStorageGetItem(key)

    override fun setItem(key: String, value: String) {
        StorageInterop.localStorageSetItem(key, value)
    }

    override fun removeItem(key: String) {
        StorageInterop.localStorageRemoveItem(key)
    }
}

@JsModule("./storage.mjs")
private external object StorageInterop {
    fun localStorageGetItem(key: String): String?
    fun localStorageSetItem(key: String, value: String)
    fun localStorageRemoveItem(key: String)
    fun isLocalStorageAccessible(): Boolean
}
