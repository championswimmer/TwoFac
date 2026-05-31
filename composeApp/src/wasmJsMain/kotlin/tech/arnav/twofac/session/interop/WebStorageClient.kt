@file:OptIn(ExperimentalWasmJsInterop::class)

package tech.arnav.twofac.session.interop

import kotlinx.coroutines.await
import kotlin.js.Promise

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

internal interface ExtensionSessionStorageClient {
    fun isAvailable(): Boolean
    suspend fun getItem(key: String): String?
    suspend fun setItem(key: String, value: String): Boolean
    suspend fun removeItem(key: String): Boolean
}

internal class BrowserExtensionSessionStorageClient : ExtensionSessionStorageClient {
    override fun isAvailable(): Boolean = StorageInterop.isExtensionSessionStorageAccessible()

    override suspend fun getItem(key: String): String? {
        if (!isAvailable()) return null
        return runCatching {
            StorageInterop.extensionSessionStorageGetItem(key)
                .await<ExtensionSessionStorageGetResult>()
                .value
        }.getOrNull()
    }

    override suspend fun setItem(key: String, value: String): Boolean {
        if (!isAvailable()) return false
        return runCatching {
            StorageInterop.extensionSessionStorageSetItem(key, value)
                .await<ExtensionSessionStorageMutationResult>()
                .success
        }.getOrElse { false }
    }

    override suspend fun removeItem(key: String): Boolean {
        if (!isAvailable()) return false
        return runCatching {
            StorageInterop.extensionSessionStorageRemoveItem(key)
                .await<ExtensionSessionStorageMutationResult>()
                .success
        }.getOrElse { false }
    }
}

private external interface ExtensionSessionStorageGetResult : JsAny {
    val value: String?
}

private external interface ExtensionSessionStorageMutationResult : JsAny {
    val success: Boolean
}

@JsModule("./storage.mjs")
private external object StorageInterop {
    fun localStorageGetItem(key: String): String?
    fun localStorageSetItem(key: String, value: String)
    fun localStorageRemoveItem(key: String)
    fun isLocalStorageAccessible(): Boolean
    fun isExtensionSessionStorageAccessible(): Boolean
    fun extensionSessionStorageGetItem(key: String): Promise<ExtensionSessionStorageGetResult>
    fun extensionSessionStorageSetItem(key: String, value: String): Promise<ExtensionSessionStorageMutationResult>
    fun extensionSessionStorageRemoveItem(key: String): Promise<ExtensionSessionStorageMutationResult>
}
