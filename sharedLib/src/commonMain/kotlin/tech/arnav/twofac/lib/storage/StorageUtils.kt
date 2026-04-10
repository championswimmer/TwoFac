@file:OptIn(ExperimentalUuidApi::class)

package tech.arnav.twofac.lib.storage

import dev.whyoleg.cryptography.CryptographyProvider
import kotlinx.io.bytestring.decodeToString
import kotlinx.io.bytestring.encodeToByteString
import tech.arnav.twofac.lib.crypto.CryptoTools
import tech.arnav.twofac.lib.crypto.DefaultCryptoTools
import tech.arnav.twofac.lib.crypto.Encoding.toByteString
import tech.arnav.twofac.lib.crypto.Encoding.toHexString
import tech.arnav.twofac.lib.otp.OTP
import tech.arnav.twofac.lib.uri.OtpAuthURI
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

object StorageUtils {

    private val cryptoTools = DefaultCryptoTools(CryptographyProvider.Default)

    suspend fun OTP.toStoredAccount(signingKey: CryptoTools.SigningKey, iterations: Int = CryptoTools.TARGET_HASH_ITERATIONS): StoredAccount {
        val accountID = Uuid.fromByteArray(signingKey.salt.toByteArray())
        val otpAuthUriByteString = OtpAuthURI.create(this).encodeToByteString()

        val encryptedURI = cryptoTools.encrypt(signingKey.key, otpAuthUriByteString)
        return StoredAccount(
            accountID = accountID,
            accountLabel = "${issuer?.let { "$it:" } ?: ""}${accountName}",
            salt = signingKey.salt.toHexString(),
            encryptedURI = encryptedURI.toHexString(),
            iterations = iterations,
        )
    }

    suspend fun StoredAccount.toOTP(signingKey: CryptoTools.SigningKey): OTP {
        val decryptedURI = cryptoTools.decrypt(encryptedURI.toByteString(), signingKey.key)
        return OtpAuthURI.parse(decryptedURI.decodeToString())
    }

    suspend fun StoredAccount.toDecryptedURI(signingKey: CryptoTools.SigningKey): String {
        val decryptedURI = cryptoTools.decrypt(encryptedURI.toByteString(), signingKey.key)
        return decryptedURI.decodeToString()
    }

    suspend fun decryptURI(encryptedURI: String, passKey: String, salt: String, iterations: Int = CryptoTools.LEGACY_HASH_ITERATIONS): String {
        val signingKey = cryptoTools.createSigningKey(passKey, salt.toByteString(), iterations)
        val decryptedURI = cryptoTools.decrypt(encryptedURI.toByteString(), signingKey.key)
        return decryptedURI.decodeToString()
    }
}
