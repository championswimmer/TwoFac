package tech.arnav.twofac.lib.importer

import tech.arnav.twofac.lib.PublicApi

/**
 * Represents the result of an import operation
 */
@PublicApi
sealed class ImportResult {
    /**
     * Import succeeded with the given OTP auth URIs
     * @param otpAuthUris List of otpauth:// URIs that were successfully parsed
     */
    data class Success(val otpAuthUris: List<String>) : ImportResult()

    /**
     * Import failed with an error
     * @param message Error message describing what went wrong
     * @param cause Optional exception that caused the failure
     */
    data class Failure(val message: String, val cause: Throwable? = null) : ImportResult()
}
