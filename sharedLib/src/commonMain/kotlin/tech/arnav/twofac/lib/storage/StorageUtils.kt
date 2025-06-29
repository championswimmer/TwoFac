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

    suspend fun OTP.toStoredAccount(signingKey: CryptoTools.SigningKey): StoredAccount {
        val accountID = Uuid.fromByteArray(signingKey.salt.toByteArray())
        val otpAuthUriByteString = OtpAuthURI.create(this).encodeToByteString()

        val encryptedURI = cryptoTools.encrypt(signingKey.key, otpAuthUriByteString)
        return StoredAccount(
            accountID = accountID,
            accountLabel = "${issuer?.let { "$it:" } ?: ""}${accountName}",
            salt = signingKey.salt.toHexString(),
            encryptedURI = encryptedURI.toHexString()
        )
    }

    suspend fun StoredAccount.toOTP(signingKey: CryptoTools.SigningKey): OTP {
        val decryptedURI = cryptoTools.decrypt(encryptedURI.toByteString(), signingKey.key)
        return OtpAuthURI.parse(decryptedURI.decodeToString())
    }
}