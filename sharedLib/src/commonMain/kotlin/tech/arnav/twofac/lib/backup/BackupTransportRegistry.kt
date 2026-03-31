package tech.arnav.twofac.lib.backup

import kotlin.coroutines.cancellation.CancellationException
import tech.arnav.twofac.lib.PublicApi

/**
 * Maps a [BackupTransport] to its [BackupProvider] snapshot DTO.
 *
 * Centralises the field-by-field projection so that adding a new capability to
 * [BackupTransport] only requires updating this one function rather than
 * both [BackupTransportRegistry.providerInfo] and [BackupProvider].
 *
 * @param isAvailable Runtime availability queried by the caller.
 */
fun BackupTransport.toProvider(isAvailable: Boolean): BackupProvider = BackupProvider(
    id = id,
    displayName = displayName,
    supportsManualBackup = supportsManualBackup,
    supportsManualRestore = supportsManualRestore,
    supportsAutomaticRestore = supportsAutomaticRestore,
    requiresAuthentication = requiresAuthentication,
    isAvailable = isAvailable,
)

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
            transport.toProvider(available)
        }
    }
}
