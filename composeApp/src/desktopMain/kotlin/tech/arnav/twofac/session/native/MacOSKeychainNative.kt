package tech.arnav.twofac.session.native

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.ptr.IntByReference

/**
 * JNA interface to the native macOS Keychain helper library.
 *
 * The native library (libtwofac_keychain.dylib) must be loadable from:
 * 1. The java.library.path
 * 2. Bundled within the application (jpackage will include it)
 * 3. The classpath under darwin-{arch}/libtwofac_keychain.dylib
 */
interface MacOSKeychainNative : Library {
    /**
     * Check if Touch ID / biometric authentication is available.
     * @return 1 if available, 0 otherwise
     */
    fun twofac_keychain_is_available(): Int

    /**
     * Store a passkey in the Keychain with biometric protection.
     * @param passkey The passkey bytes (UTF-8 encoded)
     * @param passkeyLen Length of the passkey
     * @return 0 on success, non-zero error code on failure
     */
    fun twofac_keychain_store(passkey: ByteArray, passkeyLen: Int): Int

    /**
     * Retrieve a passkey from the Keychain.
     * This will trigger a Touch ID prompt if the item is biometric-protected.
     * @param buffer Buffer to receive the passkey
     * @param bufferLen Size of the buffer
     * @param outLen Pointer to receive the actual length of the passkey
     * @return 0 on success, non-zero error code on failure
     */
    fun twofac_keychain_retrieve(buffer: ByteArray, bufferLen: Int, outLen: IntByReference): Int

    /**
     * Delete the stored passkey from the Keychain.
     * @return 0 on success, non-zero error code on failure
     */
    fun twofac_keychain_delete(): Int

    /**
     * Check if a passkey exists in the Keychain (without triggering auth).
     * @return 1 if exists, 0 otherwise
     */
    fun twofac_keychain_exists(): Int

    companion object {
        private const val LIBRARY_NAME = "twofac_keychain"

        /**
         * Load the native library.
         *
         * JNA will search:
         * 1. System library paths
         * 2. java.library.path
         * 3. Bundled resources (darwin-aarch64, darwin-x86-64)
         */
        fun load(): MacOSKeychainNative? {
            return try {
                Native.load(LIBRARY_NAME, MacOSKeychainNative::class.java)
            } catch (e: UnsatisfiedLinkError) {
                println("MacOSKeychainNative: Failed to load native library: ${e.message}")
                null
            }
        }
    }
}
