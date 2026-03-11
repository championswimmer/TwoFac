package tech.arnav.twofac.lib.backup

import kotlin.coroutines.cancellation.CancellationException
import tech.arnav.twofac.lib.PublicApi

@PublicApi
class BackupTransportRegistry(
    transports: List<BackupTransport> = emptyList(),
) {
    private val transportsById: LinkedHashMap<String, BackupTransport> = linkedMapOf()

    init {
        transports.forEach { transport ->
            require(!transportsById.containsKey(transport.id)) {
                "Duplicate backup transport id: ${transport.id}"
            }
            transportsById[transport.id] = transport
        }
    }

    fun all(): List<BackupTransport> = transportsById.values.toList()

    fun findById(id: String): BackupTransport? = transportsById[id]

    suspend fun providerInfo(): List<BackupProvider> {
        return all().map { transport ->
            val available = try {
                transport.isAvailable()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                false
            }

            BackupProvider(
                id = transport.id,
                displayName = transport.displayName,
                supportsManualBackup = transport.supportsManualBackup,
                supportsManualRestore = transport.supportsManualRestore,
                supportsAutomaticRestore = transport.supportsAutomaticRestore,
                requiresAuthentication = transport.requiresAuthentication,
                isAvailable = available,
            )
        }
    }
}
