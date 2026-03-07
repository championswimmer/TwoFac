package tech.arnav.twofac.lib.backup

import tech.arnav.twofac.lib.PublicApi

/**
 * Registry that holds multiple [BackupTransport] instances along with their
 * [BackupProviderInfo] metadata.
 *
 * Platform DI modules register transports additively into the registry.
 * UI and CLI code enumerate available providers through this registry
 * instead of depending on a single nullable [BackupTransport].
 */
@PublicApi
class BackupTransportRegistry {

    private val entries = mutableListOf<Entry>()

    /** A paired transport + provider info entry. */
    private data class Entry(val transport: BackupTransport, val info: BackupProviderInfo)

    /** Register a transport with its provider metadata. */
    fun register(transport: BackupTransport, info: BackupProviderInfo) {
        require(entries.none { it.info.id == info.id }) {
            "A transport with id '${info.id}' is already registered"
        }
        entries.add(Entry(transport, info))
    }

    /** All registered provider metadata entries. */
    fun providers(): List<BackupProviderInfo> = entries.map { it.info }

    /** Look up a transport by its provider id. Returns null if not registered. */
    fun transport(id: String): BackupTransport? = entries.firstOrNull { it.info.id == id }?.transport

    /** Look up provider info by id. Returns null if not registered. */
    fun providerInfo(id: String): BackupProviderInfo? = entries.firstOrNull { it.info.id == id }?.info

    /** Number of registered transports. */
    val size: Int get() = entries.size

    /** Whether no transports are registered. */
    fun isEmpty(): Boolean = entries.isEmpty()
}
