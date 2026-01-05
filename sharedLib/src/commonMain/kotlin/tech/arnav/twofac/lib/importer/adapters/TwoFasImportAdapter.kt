package tech.arnav.twofac.lib.importer.adapters

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import tech.arnav.twofac.lib.PublicApi
import tech.arnav.twofac.lib.crypto.CryptoTools
import tech.arnav.twofac.lib.importer.ImportAdapter
import tech.arnav.twofac.lib.importer.ImportResult
import tech.arnav.twofac.lib.uri.OtpAuthURI

/**
 * Import adapter for 2FAS Authenticator export files
 *
 * Supports the JSON format exported by 2FAS app with `.2fas` extension.
 * Format example:
 * ```json
 * {
 *   "services": [
 *     {
 *       "name": "ServiceName",
 *       "secret": "JBSWY3DPEHPK3PXP",
 *       "account": "username@example.com",
 *       "digits": 6,
 *       "period": 30,
 *       "algorithm": "SHA1",
 *       "type": "totp"
 *     }
 *   ]
 * }
 * ```
 */
@PublicApi
class TwoFasImportAdapter : ImportAdapter {

    @Serializable
    private data class TwoFasExport(
        val services: List<TwoFasService>
    )

    @Serializable
    private data class TwoFasService(
        val name: String,
        val secret: String,
        val account: String? = null,
        val digits: Int? = null,
        val period: Int? = null,
        val algorithm: String? = null,
        @SerialName("otp")
        val otpSection: TwoFasOtpSection? = null
    )

    @Serializable
    private data class TwoFasOtpSection(
        val account: String? = null,
        val digits: Int? = null,
        val period: Int? = null,
        val algorithm: String? = null
    )

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override suspend fun parse(fileContent: String, password: String?): ImportResult {
        return try {
            val export = json.decodeFromString<TwoFasExport>(fileContent)
            val uris = export.services.mapNotNull { service ->
                convertToOtpAuthUri(service)
            }

            if (uris.isEmpty()) {
                ImportResult.Failure("No valid services found in the 2FAS export file")
            } else {
                ImportResult.Success(uris)
            }
        } catch (e: Exception) {
            ImportResult.Failure("Failed to parse 2FAS export file: ${e.message}", e)
        }
    }

    private fun convertToOtpAuthUri(service: TwoFasService): String? {
        try {
            // Helper function to get value from service or otp section
            fun <T> getField(serviceField: T?, otpField: T?, default: T): T {
                return serviceField ?: otpField ?: default
            }

            // Get values from either top level or otp section
            val digits = getField(service.digits, service.otpSection?.digits, OtpAuthURI.DEFAULT_DIGITS)
            val period = getField(service.period, service.otpSection?.period, OtpAuthURI.DEFAULT_PERIOD).toLong()
            val algorithmStr = getField(service.algorithm, service.otpSection?.algorithm, "SHA1")
            val algorithm = when (algorithmStr.uppercase()) {
                "SHA1" -> CryptoTools.Algo.SHA1
                "SHA256" -> CryptoTools.Algo.SHA256
                "SHA512" -> CryptoTools.Algo.SHA512
                else -> CryptoTools.Algo.SHA1
            }

            val accountName = service.account ?: service.otpSection?.account ?: "Unknown"
            val issuer = service.name

            val builder = OtpAuthURI.Builder()
                .type(OtpAuthURI.Type.TOTP) // 2FAS primarily uses TOTP
                .label("$issuer:$accountName")
                .secret(service.secret)
                .issuer(issuer)
                .algorithm(algorithm)
                .digits(digits)
                .period(period)

            return builder.build()
        } catch (e: Exception) {
            // Skip invalid entries
            return null
        }
    }

    override fun getName(): String = "2FAS Authenticator"
}
