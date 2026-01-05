package tech.arnav.twofac.lib.importer.adapters

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import tech.arnav.twofac.lib.PublicApi
import tech.arnav.twofac.lib.importer.ImportAdapter
import tech.arnav.twofac.lib.importer.ImportResult

/**
 * Import adapter for Ente Auth export files
 *
 * Currently supports plaintext exports (newline-separated otpauth:// URIs).
 *
 * Ente Auth also supports encrypted exports with the following format:
 * ```json
 * {
 *   "version": 1,
 *   "kdfParams": {
 *     "memLimit": 4096,
 *     "opsLimit": 3,
 *     "salt": "base64_encoded_salt"
 *   },
 *   "encryptedData": "base64_encoded_data",
 *   "encryptionNonce": "base64_encoded_nonce"
 * }
 * ```
 *
 * Note: Encrypted format support requires Argon2id KDF and XChaCha20-Poly1305 encryption,
 * which are not currently implemented in this library. For encrypted exports,
 * users should decrypt them using Ente Auth first, then export as plaintext.
 */
@PublicApi
class EnteImportAdapter : ImportAdapter {

    @Serializable
    private data class EnteEncryptedExport(
        val version: Int,
        val kdfParams: KdfParams? = null,
        val encryptedData: String? = null,
        val encryptionNonce: String? = null
    )

    @Serializable
    private data class KdfParams(
        val memLimit: Int? = null,
        val opsLimit: Int? = null,
        val salt: String? = null
    )

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override suspend fun parse(fileContent: String, password: String?): ImportResult {
        return try {
            // First, try to detect if this is an encrypted export by checking for JSON
            if (fileContent.trimStart().startsWith("{")) {
                parseEncryptedExport(fileContent, password)
            } else {
                parsePlaintextExport(fileContent)
            }
        } catch (e: Exception) {
            ImportResult.Failure("Failed to parse Ente Auth export file: ${e.message}", e)
        }
    }

    private fun parsePlaintextExport(fileContent: String): ImportResult {
        // Plaintext exports are newline-separated otpauth:// URIs
        val uris = fileContent.lines()
            .map { it.trim() }
            .filter { it.startsWith("otpauth://") }

        return if (uris.isEmpty()) {
            ImportResult.Failure("No valid otpauth:// URIs found in the Ente Auth plaintext export")
        } else {
            ImportResult.Success(uris)
        }
    }

    private fun parseEncryptedExport(fileContent: String, password: String?): ImportResult {
        // Try to parse as JSON to detect encrypted format
        val export = try {
            json.decodeFromString<EnteEncryptedExport>(fileContent)
        } catch (e: Exception) {
            return ImportResult.Failure("Failed to parse Ente Auth export JSON: ${e.message}", e)
        }

        // Check if this is an encrypted export
        if (export.encryptedData != null) {
            return ImportResult.Failure(
                "Encrypted Ente Auth exports are not yet supported. " +
                        "Please export as plaintext (otpauth:// URIs) from Ente Auth settings, " +
                        "or decrypt the export file using Ente Auth first. " +
                        "Encrypted format requires Argon2id KDF and XChaCha20-Poly1305 encryption " +
                        "which are not currently implemented in this library."
            )
        }

        return ImportResult.Failure("Unrecognized Ente Auth export format")
    }

    override fun getName(): String = "Ente Auth"

    override fun requiresPassword(): Boolean = false // Only for plaintext exports currently
}
