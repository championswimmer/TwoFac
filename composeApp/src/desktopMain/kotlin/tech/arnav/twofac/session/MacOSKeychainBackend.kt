package tech.arnav.twofac.session

import tech.arnav.twofac.session.native.MacOSKeychainNative
import com.sun.jna.ptr.IntByReference

/**
 * macOS Keychain backend for secure unlock.
 *
 * Uses the native TwoFacKeychain helper library to store the vault passkey
 * in the macOS Keychain with biometric (Touch ID) access control.
 *
 * This provides Tier A secure unlock: OS-enforced user-presence protection
 * on every secret retrieval.
 */
class MacOSKeychainBackend(
    private val native: MacOSKeychainNative?
) : DesktopSecureUnlockBackend {

    constructor() : this(loadNativeWithLogging())

    override fun isAvailable(): Boolean {
        val logFile = java.io.File(System.getProperty("user.home"), "twofac-native-debug.log")
        val nativeNotNull = native != null
        val availableResult = if (nativeNotNull) native!!.twofac_keychain_is_available() else -1
        val result = nativeNotNull && availableResult == 1
        logFile.appendText("[${java.time.Instant.now()}] MacOSKeychainBackend.isAvailable(): native=$nativeNotNull, availableResult=$availableResult, result=$result\n")
        return result
    }

    override fun supportsStrongUserPresence(): Boolean {
        return isAvailable()
    }

    override fun currentMode(): DesktopSecureUnlockMode {
        return if (isAvailable()) {
            DesktopSecureUnlockMode.MACOS_KEYCHAIN_BIOMETRIC
        } else {
            DesktopSecureUnlockMode.NONE
        }
    }

    override suspend fun getSavedPasskey(): String? {
        if (!isAvailable()) return null

        val buffer = ByteArray(1024)
        val outLen = IntByReference()

        val result = native!!.twofac_keychain_retrieve(buffer, buffer.size, outLen)
        if (result != 0) {
            println("MacOSKeychainBackend: Failed to retrieve passkey, error code: $result")
            return null
        }

        return String(buffer, 0, outLen.value, Charsets.UTF_8)
    }

    override fun savePasskey(passkey: String) {
        if (!isAvailable()) return

        val bytes = passkey.toByteArray(Charsets.UTF_8)
        val result = native!!.twofac_keychain_store(bytes, bytes.size)
        if (result != 0) {
            println("MacOSKeychainBackend: Failed to save passkey, error code: $result")
        }
    }

    override fun clearPasskey() {
        if (!isAvailable()) return

        val result = native!!.twofac_keychain_delete()
        if (result != 0) {
            println("MacOSKeychainBackend: Failed to delete passkey, error code: $result")
        }
    }

    override fun hasStoredPasskey(): Boolean {
        if (!isAvailable()) return false
        return native!!.twofac_keychain_exists() == 1
    }

    override suspend fun enrollPasskey(passkey: String): Boolean {
        val logFile = java.io.File(System.getProperty("user.home"), "twofac-native-debug.log")
        logFile.appendText("[${java.time.Instant.now()}] MacOSKeychainBackend.enrollPasskey(): starting\n")
        
        if (!isAvailable()) {
            logFile.appendText("[${java.time.Instant.now()}] MacOSKeychainBackend.enrollPasskey(): isAvailable=false, aborting\n")
            return false
        }

        logFile.appendText("[${java.time.Instant.now()}] MacOSKeychainBackend.enrollPasskey(): calling clearPasskey()\n")
        clearPasskey()

        val bytes = passkey.toByteArray(Charsets.UTF_8)
        logFile.appendText("[${java.time.Instant.now()}] MacOSKeychainBackend.enrollPasskey(): calling twofac_keychain_store with ${bytes.size} bytes\n")
        val result = native!!.twofac_keychain_store(bytes, bytes.size)

        logFile.appendText("[${java.time.Instant.now()}] MacOSKeychainBackend.enrollPasskey(): result=$result\n")
        return result == 0
    }

    companion object {
        private fun loadNativeWithLogging(): MacOSKeychainNative? {
            val logFile = java.io.File(System.getProperty("user.home"), "twofac-native-debug.log")
            logFile.appendText("[${java.time.Instant.now()}] MacOSKeychainBackend: About to call MacOSKeychainNative.load()\n")
            return MacOSKeychainNative.load().also {
                logFile.appendText("[${java.time.Instant.now()}] MacOSKeychainBackend: MacOSKeychainNative.load() returned: $it\n")
            }
        }
    }
}
