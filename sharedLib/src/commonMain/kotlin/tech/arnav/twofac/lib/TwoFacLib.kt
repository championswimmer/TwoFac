package tech.arnav.twofac.lib

import dev.whyoleg.cryptography.CryptographyProvider
import tech.arnav.twofac.lib.crypto.DefaultCryptoTools
import tech.arnav.twofac.lib.crypto.Encoding.toByteString
import tech.arnav.twofac.lib.otp.HOTP
import tech.arnav.twofac.lib.otp.TOTP
import tech.arnav.twofac.lib.storage.MemoryStorage
import tech.arnav.twofac.lib.storage.Storage
import tech.arnav.twofac.lib.storage.StorageUtils.toOTP
import tech.arnav.twofac.lib.storage.StorageUtils.toStoredAccount
import tech.arnav.twofac.lib.storage.StoredAccount
import tech.arnav.twofac.lib.uri.OtpAuthURI
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class TwoFacLib private constructor(
    val storage: Storage,
    val passKey: String,
) {

    companion object {

        fun initialise(
            storage: Storage = MemoryStorage(), passKey: String
        ): TwoFacLib {
            require(passKey.isNotBlank()) { "Password key cannot be blank" }
            if (storage is MemoryStorage) {
                println(
                    """
                    ⚠️[WARNING]: Using in-memory storage. This will not persist data across application restarts.
                    Use a persistent storage implementation for production use.
                """.trimIndent()
                )
            }
            return TwoFacLib(storage, passKey)
        }
    }

    private val cryptoTools = DefaultCryptoTools(CryptographyProvider.Default)

    fun getAllAccounts(): List<StoredAccount.DisplayAccount> {
        return storage.getAccountList().map(StoredAccount::forDisplay)
    }

    @OptIn(ExperimentalTime::class)
    suspend fun getAllAccountOTPs(): List<Pair<StoredAccount.DisplayAccount, String>> {
        return storage.getAccountList().map { account ->
            val otpGen = account.toOTP(
                cryptoTools.createSigningKey(passKey, account.salt.toByteString()),
            )
            val timeNow = Clock.System.now().epochSeconds
            println("Time now: $timeNow")
            val otpString: String = when (otpGen) {
                is HOTP -> otpGen.generateOTP(0)
                is TOTP -> otpGen.generateOTP(timeNow)
                else -> throw IllegalArgumentException("Unknown OTP type: ${otpGen::class.simpleName}")
            }
            return@map Pair(
                account.forDisplay(), otpString
            )
        }
    }

    suspend fun addAccount(accountURI: String): Boolean {
        val otp = OtpAuthURI.parse(accountURI)
        val signingKey = cryptoTools.createSigningKey(passKey)
        val account = otp.toStoredAccount(signingKey)
        return storage.saveAccount(account)
    }
}