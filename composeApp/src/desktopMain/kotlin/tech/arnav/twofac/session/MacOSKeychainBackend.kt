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

    constructor() : this(MacOSKeychainNative.load())

    override fun isAvailable(): Boolean {
        return native != null && native.twofac_keychain_is_available() == 1
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
        if (!isAvailable()) return false

        clearPasskey()

        val bytes = passkey.toByteArray(Charsets.UTF_8)
        val result = native!!.twofac_keychain_store(bytes, bytes.size)

        println("MacOSKeychainBackend: enrollPasskey result=$result")
        return result == 0
    }
}
