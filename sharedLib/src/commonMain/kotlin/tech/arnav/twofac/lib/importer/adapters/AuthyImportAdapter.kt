package tech.arnav.twofac.lib.importer.adapters

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import tech.arnav.twofac.lib.PublicApi
import tech.arnav.twofac.lib.crypto.CryptoTools
import tech.arnav.twofac.lib.importer.ImportAdapter
import tech.arnav.twofac.lib.importer.ImportResult
import tech.arnav.twofac.lib.uri.OtpAuthURI

/**
 * Import adapter for Authy export files
 *
 * Supports the JSON format typically exported from Authy via community tools.
 * Format example:
 * ```json
 * [
 *   {
 *     "secret": "JBSWY3DPEHPK3PXP",
 *     "digits": 6,
 *     "name": "Account Name",
 *     "issuer": "Provider Name",
 *     "type": "totp",
 *     "period": 30
 *   }
 * ]
 * ```
 */
@PublicApi
class AuthyImportAdapter : ImportAdapter {

    @Serializable
    private data class AuthyToken(
        val secret: String,
        val digits: Int? = null,
        val name: String,
        val issuer: String? = null,
        val type: String? = null,
        val period: Int? = null,
        val algorithm: String? = null
    )

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override suspend fun parse(fileContent: String, password: String?): ImportResult {
        return try {
            val tokens = json.decodeFromString<List<AuthyToken>>(fileContent)
            val uris = tokens.mapNotNull { token ->
                convertToOtpAuthUri(token)
            }

            if (uris.isEmpty()) {
                ImportResult.Failure("No valid tokens found in the Authy export file")
            } else {
                ImportResult.Success(uris)
            }
        } catch (e: Exception) {
            ImportResult.Failure("Failed to parse Authy export file: ${e.message}", e)
        }
    }

    private fun convertToOtpAuthUri(token: AuthyToken): String? {
        try {
            val digits = token.digits ?: OtpAuthURI.DEFAULT_DIGITS
            val period = (token.period ?: OtpAuthURI.DEFAULT_PERIOD).toLong()
            val algorithm = when ((token.algorithm ?: "SHA1").uppercase()) {
                "SHA1" -> CryptoTools.Algo.SHA1
                "SHA256" -> CryptoTools.Algo.SHA256
                "SHA512" -> CryptoTools.Algo.SHA512
                else -> CryptoTools.Algo.SHA1
            }

            val issuer = token.issuer ?: token.name
            val accountName = token.name

            // Determine type (TOTP is default for Authy)
            val type = when (token.type?.lowercase()) {
                "hotp" -> OtpAuthURI.Type.HOTP
                else -> OtpAuthURI.Type.TOTP
            }

            val builder = OtpAuthURI.Builder()
                .type(type)
                .label("$issuer:$accountName")
                .secret(token.secret)
                .issuer(issuer)
                .algorithm(algorithm)
                .digits(digits)

            if (type == OtpAuthURI.Type.TOTP) {
                builder.period(period)
            } else {
                builder.counter(0) // Default counter for HOTP
            }

            return builder.build()
        } catch (e: Exception) {
            // Skip invalid entries
            return null
        }
    }

    override fun getName(): String = "Authy"
}
