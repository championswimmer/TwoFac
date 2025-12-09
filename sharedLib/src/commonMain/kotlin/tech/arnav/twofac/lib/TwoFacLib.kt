package tech.arnav.twofac.lib

import dev.whyoleg.cryptography.CryptographyProvider
import kotlinx.coroutines.CancellationException
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
import kotlin.concurrent.Volatile
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class InvalidPasskeyException(message: String = "Incorrect passkey", cause: Throwable? = null) :
    Exception(message, cause)

class TwoFacLib private constructor(
    val storage: Storage,
    @Volatile private var passKey: String?,
) {

    companion object {

        fun initialise(
            storage: Storage = MemoryStorage(), passKey: String? = null
        ): TwoFacLib {
            passKey?.let {
                require(it.isNotBlank()) { "Password key cannot be blank" }
            }
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

    /**
     * Unlocks the library with the provided passkey
     */
    fun unlock(passKey: String) {
        require(passKey.isNotBlank()) { "Password key cannot be blank" }
        this.passKey = passKey
    }

    fun lock() {
        this.passKey = null
    }

    /**
     * Checks if the library is unlocked (passkey is set)
     */
    fun isUnlocked(): Boolean {
        return passKey != null
    }

    suspend fun getAllAccounts(): List<StoredAccount.DisplayAccount> {
        return storage.getAccountList().map(StoredAccount::forDisplay)
    }

    @OptIn(ExperimentalTime::class)
    suspend fun getAllAccountOTPs(): List<Pair<StoredAccount.DisplayAccount, String>> {
        check(isUnlocked()) { "TwoFacLib is not unlocked. Call unlock() with a valid passkey first." }
        val currentPassKey = passKey!! // Safe to use !! after isUnlocked() check
        val accounts = storage.getAccountList()
        return accounts.map { account ->
            val otpGen = try {
                account.toOTP(
                    cryptoTools.createSigningKey(currentPassKey, account.salt.toByteString()),
                )
            } catch (e: Exception) {
                when (e) {
                    is CancellationException -> throw e
                    is IllegalArgumentException, is IllegalStateException -> throw InvalidPasskeyException(
                        cause = e
                    )

                    else -> throw e
                }
            }
            val timeNow = Clock.System.now().epochSeconds
            val otpString: String = when (otpGen) {
                is HOTP -> otpGen.generateOTP(0)
                is TOTP -> otpGen.generateOTP(timeNow)
                else -> throw IllegalArgumentException("Unknown OTP type: ${otpGen::class.simpleName}")
            }
            val nextCodeAt = when (otpGen) {
                is HOTP -> 0L // HOTP does not have a next code time
                is TOTP -> otpGen.nextCodeAt(timeNow)
                else -> throw IllegalArgumentException("Unknown OTP type: ${otpGen::class.simpleName}")
            }
            return@map Pair(
                account.forDisplay(nextCodeAt), otpString
            )
        }
    }

    suspend fun addAccount(accountURI: String): Boolean {
        check(isUnlocked()) { "TwoFacLib is not unlocked. Call unlock() with a valid passkey first." }
        val currentPassKey = passKey!! // Safe to use !! after isUnlocked() check
        val otp = OtpAuthURI.parse(accountURI)
        val signingKey = cryptoTools.createSigningKey(currentPassKey)
        val account = otp.toStoredAccount(signingKey)
        return storage.saveAccount(account)
    }
}
