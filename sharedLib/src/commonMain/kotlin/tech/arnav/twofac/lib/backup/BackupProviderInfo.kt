package tech.arnav.twofac.lib.backup

import tech.arnav.twofac.lib.PublicApi

/**
 * Metadata describing a backup transport provider's identity and capabilities.
 *
 * This is used by UI and CLI to enumerate available backup providers and
 * determine which operations each provider supports.
 */
@PublicApi
data class BackupProviderInfo(
    /** Unique identifier for this provider (e.g. "local", "icloud", "gdrive-appdata"). */
    val id: String,
    /** Human-readable display name (e.g. "Local Backup", "iCloud", "Google Drive"). */
    val displayName: String,
    /** Whether this provider supports manual backup (export). */
    val supportsManualBackup: Boolean = true,
    /** Whether this provider supports manual restore (import). */
    val supportsManualRestore: Boolean = true,
    /** Whether this provider supports automatic restore. */
    val supportsAutomaticRestore: Boolean = false,
    /** Whether this provider requires authentication before use. */
    val authRequired: Boolean = false,
)
