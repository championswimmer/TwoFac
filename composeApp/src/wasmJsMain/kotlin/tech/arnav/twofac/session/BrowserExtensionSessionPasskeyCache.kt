@file:OptIn(ExperimentalWasmJsInterop::class)

package tech.arnav.twofac.session

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import tech.arnav.twofac.session.interop.BrowserExtensionSessionStorageClient
import tech.arnav.twofac.session.interop.ExtensionSessionStorageClient

private const val BROWSER_EXTENSION_SESSION_PASSKEY_KEY = "twofac_session_retained_passkey"

internal fun supportsBrowserExtensionSessionRetention(
    storageClient: ExtensionSessionStorageClient = BrowserExtensionSessionStorageClient(),
): Boolean = storageClient.isAvailable()

internal fun defaultBrowserSessionPasskeyCache(
    storageClient: ExtensionSessionStorageClient = BrowserExtensionSessionStorageClient(),
): SessionPasskeyCache {
    return if (storageClient.isAvailable()) {
        BrowserExtensionSessionPasskeyCache(storageClient)
    } else {
        InMemorySessionPasskeyCache()
    }
}

internal class BrowserExtensionSessionPasskeyCache(
    private val storageClient: ExtensionSessionStorageClient = BrowserExtensionSessionStorageClient(),
    private val storageKey: String = BROWSER_EXTENSION_SESSION_PASSKEY_KEY,
) : SessionPasskeyCache {
    private val scope = CoroutineScope(SupervisorJob())
    private var inMemoryPasskey: String? = null

    override suspend fun read(): String? {
        inMemoryPasskey?.let { return it }
        if (!storageClient.isAvailable()) return null

        val persistedPasskey = runCatching {
            storageClient.getItem(storageKey)
        }.getOrNull()

        if (!persistedPasskey.isNullOrBlank()) {
            inMemoryPasskey = persistedPasskey
        }
        return persistedPasskey
    }

    override fun write(passkey: String) {
        inMemoryPasskey = passkey
        if (!storageClient.isAvailable()) return

        scope.launch {
            runCatching {
                storageClient.setItem(storageKey, passkey)
            }
        }
    }

    override fun clear() {
        inMemoryPasskey = null
        if (!storageClient.isAvailable()) return

        scope.launch {
            runCatching {
                storageClient.removeItem(storageKey)
            }
        }
    }
}
