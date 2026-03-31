package tech.arnav.twofac.lib

import dev.whyoleg.cryptography.CryptographyProvider
import tech.arnav.twofac.lib.crypto.DefaultCryptoTools
import tech.arnav.twofac.lib.crypto.Encoding.toByteString
import tech.arnav.twofac.lib.importer.ImportAdapter
import tech.arnav.twofac.lib.importer.ImportResult
import tech.arnav.twofac.lib.backup.EncryptedAccountEntry
import tech.arnav.twofac.lib.otp.HOTP
import tech.arnav.twofac.lib.otp.TOTP
import tech.arnav.twofac.lib.storage.MemoryStorage
import tech.arnav.twofac.lib.storage.Storage
import tech.arnav.twofac.lib.storage.StorageUtils.decryptURI
import tech.arnav.twofac.lib.storage.StorageUtils.toDecryptedURI
import tech.arnav.twofac.lib.storage.StorageUtils.toOTP
import tech.arnav.twofac.lib.storage.StorageUtils.toStoredAccount
import tech.arnav.twofac.lib.storage.StoredAccount
import tech.arnav.twofac.lib.uri.OtpAuthURI
import kotlin.concurrent.Volatile
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@PublicApi
class TwoFacLib private constructor(
    val storage: Storage,
    @Volatile private var passKey: String?,
    @Volatile private var storeHasAccounts: Boolean?,
) {

    /**
     * Cached best-effort indicator of whether the store contains accounts.
     *
     * This value is `false` both when the store is empty **and** when the state has
     * not yet been determined (i.e. before the first [unlock] or [getAllAccounts] call).
     * Do not rely on it for authoritative UI decisions before a suspend-context check
     * has been performed; it will be populated lazily the first time
     * [checkUnlockedOrThrow] runs.
     */
    val isStoreInitialized: Boolean
        get() = storeHasAccounts == true

    companion object {
        private const val MISSING_STORE_MESSAGE =
            "No account store found. Enter password to create a new store."
        private const val LOCKED_STORE_MESSAGE =
            "Secrets store is locked. Enter password to unlock it."

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
            return TwoFacLib(storage, passKey, null)
        }
    }

    private val cryptoTools = DefaultCryptoTools(CryptographyProvider.Default)

    @Volatile
    private var accountList: List<StoredAccount>? = null

    private suspend fun checkUnlockedOrThrow() {
        if (!isUnlocked()) {
            if (storeHasAccounts == null) {
                storeHasAccounts = storage.getAccountList().isNotEmpty()
            }
            // Now we know the state, so lockedStateMessage() will be correct
            error(lockedStateMessage())
        }
    }

    /**
     * Unlocks the library with the provided passkey and loads accounts into memory
     */
    suspend fun unlock(passKey: String) {
        require(passKey.isNotBlank()) { "Password key cannot be blank" }
        this.passKey = passKey
        // Load accounts from storage into memory
        val accounts = storage.getAccountList()
        this.accountList = accounts
        this.storeHasAccounts = accounts.isNotEmpty()
    }

    /**
     * Checks if the library is unlocked (passkey is set)
     */
    fun isUnlocked(): Boolean {
        return passKey != null
    }

    suspend fun getAllAccounts(): List<StoredAccount.DisplayAccount> {
        checkUnlockedOrThrow()
        val accounts = accountList ?: error("Account list is not loaded. This should not happen when unlocked.")
        return accounts.map(StoredAccount::forDisplay)
    }

    @OptIn(ExperimentalTime::class)
    suspend fun getAllAccountOTPs(): List<Pair<StoredAccount.DisplayAccount, String>> {
        checkUnlockedOrThrow()
        val currentPassKey = passKey!! // Safe to use !! after isUnlocked() check
        val accounts = accountList ?: error("Account list is not loaded. This should not happen when unlocked.")
        return accounts.map { account ->
            val otpGen = account.toOTP(
                cryptoTools.createSigningKey(currentPassKey, account.salt.toByteString()),
            )
            val timeNow = Clock.System.now().epochSeconds
            val otpString: String = when (otpGen) {
                is HOTP -> otpGen.generateOTP(otpGen.initialCounter)
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
        checkUnlockedOrThrow()
        val currentPassKey = passKey!! // Safe to use !! after isUnlocked() check
        val otp = OtpAuthURI.parse(accountURI)
        val signingKey = cryptoTools.createSigningKey(currentPassKey)
        val account = otp.toStoredAccount(signingKey)
        val success = storage.saveAccount(account)
        if (success) {
            // Refresh the in-memory account list
            accountList = storage.getAccountList()
            storeHasAccounts = accountList?.isNotEmpty() == true
        }
        return success
    }

    @OptIn(ExperimentalUuidApi::class)
    suspend fun deleteAccount(accountId: String): Boolean {
        checkUnlockedOrThrow()
        val parsedAccountId = Uuid.parse(accountId)
        val success = storage.deleteAccount(parsedAccountId)
        if (success) {
            accountList = storage.getAccountList()
            storeHasAccounts = accountList?.isNotEmpty() == true
        }
        return success
    }

    suspend fun deleteAllAccountsFromStorage(): Boolean {
        val success = storage.deleteAllAccounts()
        if (success) {
            accountList = emptyList()
            storeHasAccounts = false
        }
        return success
    }

    /**
     * Import accounts from an external authenticator app using an ImportAdapter
     *
     * @param adapter The ImportAdapter for the specific authenticator app format
     * @param fileContent The raw content of the export file
     * @param password Optional password for encrypted exports
     * @return ImportResult containing success count or failure information
     */
    suspend fun importAccounts(
        adapter: ImportAdapter,
        fileContent: String,
        password: String? = null
    ): ImportResult {
        checkUnlockedOrThrow()

        // Parse the export file using the adapter
        val parseResult = adapter.parse(fileContent, password)

        return when (parseResult) {
            is ImportResult.Success -> {
                // Import each URI
                val successfulImports = mutableListOf<String>()
                val failedImports = mutableListOf<String>()

                parseResult.otpAuthUris.forEach { uri ->
                    try {
                        val success = addAccount(uri)
                        if (success) {
                            successfulImports.add(uri)
                        } else {
                            failedImports.add(uri)
                        }
                    } catch (e: Exception) {
                        failedImports.add(uri)
                    }
                }

                if (failedImports.isEmpty()) {
                    ImportResult.Success(successfulImports)
                } else {
                    ImportResult.Failure(
                        "Imported ${successfulImports.size} accounts, but ${failedImports.size} failed"
                    )
                }
            }
            is ImportResult.Failure -> parseResult
        }
    }

    /**
     * Exports all stored account URIs as decrypted otpauth:// strings.
     *
     * Requires the library to be unlocked so secrets can be decrypted.
     *
     * @return List of plaintext otpauth:// URIs for all accounts
     */
    suspend fun exportAccountsPlaintext(): List<String> {
        checkUnlockedOrThrow()
        val currentPassKey = passKey!! // Safe to use !! after isUnlocked() check
        val accounts = accountList ?: error("Account list is not loaded. This should not happen when unlocked.")
        return accounts.map { account ->
            account.toDecryptedURI(
                cryptoTools.createSigningKey(currentPassKey, account.salt.toByteString())
            )
        }
    }

    /**
     * Returns encrypted stored accounts exactly as they exist in storage.
     */
    suspend fun exportAccountsEncrypted(): List<StoredAccount> {
        checkUnlockedOrThrow()
        return accountList ?: error("Account list is not loaded. This should not happen when unlocked.")
    }

    private fun lockedStateMessage(): String =
        if (isStoreInitialized) LOCKED_STORE_MESSAGE else MISSING_STORE_MESSAGE

    suspend fun decryptEncryptedBackupAccount(
        entry: EncryptedAccountEntry,
        passKey: String,
    ): String {
        require(passKey.isNotBlank()) { "Passkey cannot be blank" }
        return decryptURI(
            encryptedURI = entry.encryptedURI,
            passKey = passKey,
            salt = entry.salt,
        )
    }
}
