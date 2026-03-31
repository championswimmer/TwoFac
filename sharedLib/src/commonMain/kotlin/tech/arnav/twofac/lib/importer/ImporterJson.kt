package tech.arnav.twofac.lib.importer

import kotlinx.serialization.json.Json

/**
 * Shared [Json] instance for all import adapters.
 *
 * Configuration:
 * - `ignoreUnknownKeys = true` — adapters must tolerate extra fields from newer app versions.
 * - `isLenient = true` — real-world exports often have minor formatting quirks.
 *
 * **Do not** reuse this instance for the watch-sync codec; that path requires
 * `encodeDefaults = true` which is intentionally absent here.
 */
internal val ImporterJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
}
