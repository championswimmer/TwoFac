package tech.arnav.twofac.lib.importer

import tech.arnav.twofac.lib.PublicApi

/**
 * Base interface for import adapters that convert various 2FA export formats
 * into otpauth:// URIs that can be imported into TwoFacLib.
 *
 * This allows extending the system to support different authenticator apps
 * using the adapter pattern.
 */
@PublicApi
interface ImportAdapter {
    /**
     * Parses the export file content and converts it to otpauth:// URIs
     *
     * @param fileContent The raw content of the export file as a string
     * @param password Optional password for encrypted exports (e.g., Ente Auth)
     * @return ImportResult containing either the list of URIs or an error
     */
    suspend fun parse(fileContent: String, password: String? = null): ImportResult

    /**
     * Returns the name of the authenticator app this adapter supports
     */
    fun getName(): String

    /**
     * Returns whether this adapter requires a password for decryption
     */
    fun requiresPassword(): Boolean = false
}
