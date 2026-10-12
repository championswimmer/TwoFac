@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package tech.arnav.twofac.lib.importer.google

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.protobuf.ProtoNumber
import tech.arnav.twofac.lib.PublicApi
import tech.arnav.twofac.lib.crypto.CryptoTools
import tech.arnav.twofac.lib.crypto.Encoding
import tech.arnav.twofac.lib.otp.HOTP
import tech.arnav.twofac.lib.otp.TOTP
import tech.arnav.twofac.lib.uri.OtpAuthURI
import kotlin.io.encoding.Base64

/** A single QR chunk, not necessarily the entire Google export. */
@PublicApi
data class GoogleAuthenticatorMigrationBatch(
    val otpAuthUris: List<String>,
    val version: Int,
    val batchSize: Int,
    val batchIndex: Int,
    val batchId: Int,
)

/** Decodes locally; neither payloads nor secrets are included in exceptions. */
@PublicApi
object GoogleAuthenticatorMigrationDecoder {
    private const val PREFIX = "otpauth-migration://offline?"
    private const val MAX_URI_LENGTH = 65536
    const val MAX_BATCH_SIZE = 100

    fun decode(uri: String): GoogleAuthenticatorMigrationBatch {
        try {
            val input = uri.trim()
            require(input.length <= MAX_URI_LENGTH && input.startsWith(PREFIX, ignoreCase = true))
            require('#' !in input)
            val values = input.substring(PREFIX.length).split('&').map { it.split('=', limit = 2) }
                .filter { it.first() == "data" }
            require(values.size == 1 && values.single().size == 2)
            // '+' is Base64 data, not a form-encoded space. Decode percent escapes exactly once.
            val data = decodeData(values.single()[1]).replace(' ', '+')
            require(data.isNotBlank())
            val bytes = runCatching {
                Base64.Default.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL).decode(data)
            }.getOrElse {
                // Some QR producers use URL-safe Base64 despite the migration URI format.
                Base64.Default.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)
                    .decode(data.replace('-', '+').replace('_', '/'))
            }
            val payload = ProtoBuf.decodeFromByteArray(MigrationPayload.serializer(), bytes)
            // Version is informational; protobuf field numbers keep unknown newer versions
            // forward-compatible. Zero batch_size is proto3's unset/default value.
            require(payload.version >= 0)
            val batchSize = if (payload.batchSize == 0 && payload.batchIndex == 0) 1 else payload.batchSize
            require(batchSize in 1..MAX_BATCH_SIZE)
            require(payload.batchIndex in 0 until batchSize)
            require(payload.accounts.isNotEmpty() && payload.accounts.size <= 1000)
            return GoogleAuthenticatorMigrationBatch(
                payload.accounts.map(::toUri), payload.version, batchSize,
                payload.batchIndex, payload.batchId,
            )
        } catch (_: Exception) {
            // Serialization/Base64 exceptions may quote input. Do not retain them as causes.
            throw IllegalArgumentException("Invalid or unsupported Google Authenticator export")
        }
    }

    private fun decodeData(value: String): String = buildString {
        var index = 0
        while (index < value.length) {
            if (value[index] == '%') {
                require(index + 2 < value.length)
                val high = value[index + 1].digitToIntOrNull(16)
                val low = value[index + 2].digitToIntOrNull(16)
                require(high != null && low != null)
                append(((high shl 4) or low).toChar())
                index += 3
            } else {
                append(value[index++])
            }
        }
    }

    private fun toUri(account: MigrationAccount): String {
        require(account.secret.isNotEmpty() && account.name.isNotBlank())
        val algorithm = when (account.algorithm) {
            0, 1 -> CryptoTools.Algo.SHA1
            2 -> CryptoTools.Algo.SHA256
            3 -> CryptoTools.Algo.SHA512
            else -> error("Unsupported algorithm")
        }
        val digits = when (account.digits) {
            0, 1 -> 6
            2 -> 8
            else -> error("Unsupported digits")
        }
        val issuer = account.issuer.takeIf { it.isNotBlank() }
            ?: account.name.substringBefore(':', "").takeIf { it.isNotBlank() }
        val name = if (issuer != null && account.name.startsWith("$issuer:")) {
            account.name.removePrefix("$issuer:")
        } else account.name
        require(name.isNotBlank() && account.counter >= 0)
        val secret = Encoding.encodeBase32(account.secret).trimEnd('=')
        val otp = when (account.type) {
            0, 2 -> TOTP(digits, algorithm, secret, name, issuer)
            1 -> HOTP(digits, algorithm, secret, name, issuer, initialCounter = account.counter)
            else -> error("Unsupported OTP type")
        }
        return OtpAuthURI.create(otp).also { OtpAuthURI.parse(it) }
    }
}

// Explicit wire numbers and proto3 defaults, independent of Kotlin property order.
@Serializable
private data class MigrationPayload(
    @ProtoNumber(1) val accounts: List<MigrationAccount> = emptyList(),
    @ProtoNumber(2) val version: Int = 0,
    @ProtoNumber(3) val batchSize: Int = 1,
    @ProtoNumber(4) val batchIndex: Int = 0,
    @ProtoNumber(5) val batchId: Int = 0,
)

@Serializable
private data class MigrationAccount(
    @ProtoNumber(1) val secret: ByteArray = byteArrayOf(),
    @ProtoNumber(2) val name: String = "",
    @ProtoNumber(3) val issuer: String = "",
    @ProtoNumber(4) val algorithm: Int = 0,
    @ProtoNumber(5) val digits: Int = 0,
    @ProtoNumber(6) val type: Int = 0,
    @ProtoNumber(7) val counter: Long = 0,
)
