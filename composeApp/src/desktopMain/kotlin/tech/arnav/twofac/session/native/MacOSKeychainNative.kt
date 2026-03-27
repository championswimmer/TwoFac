package tech.arnav.twofac.session.native

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.ptr.IntByReference
import java.io.File

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
     * Store a passkey in the Keychain, prompting Touch ID first.
     * @param passkey The passkey bytes (UTF-8 encoded)
     * @param passkeyLen Length of the passkey
     * @return 0 on success, non-zero error code on failure
     */
    fun twofac_keychain_store(passkey: ByteArray, passkeyLen: Int): Int

    /**
     * Retrieve a passkey from the Keychain, prompting Touch ID first.
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
         * 4. jpackage app bundle locations (Contents/Resources, Contents/app)
         */
        fun load(): MacOSKeychainNative? {
            // First try default JNA behavior (system paths, java.library.path, etc.)
            try {
                return Native.load(LIBRARY_NAME, MacOSKeychainNative::class.java)
            } catch (e: UnsatisfiedLinkError) {
                // fall through to manual search
            }

            val possiblePaths = mutableListOf<String>()

            // 1. Check java.library.path
            System.getProperty("java.library.path")?.split(File.pathSeparator)?.forEach {
                possiblePaths.add(it)
            }

            // 2. For jpackage apps, check relative to the app location
            try {
                val codeSourceLocation = MacOSKeychainNative::class.java.protectionDomain.codeSource?.location
                if (codeSourceLocation != null) {
                    val appDir = File(codeSourceLocation.toURI()).parentFile
                    possiblePaths.add(appDir.absolutePath)
                    // TwoFac.app/Contents/Resources
                    appDir.parentFile?.resolve("Resources")?.let { possiblePaths.add(it.absolutePath) }
                    // TwoFac.app/Contents/app
                    appDir.parentFile?.resolve("app")?.let { possiblePaths.add(it.absolutePath) }
                    // TwoFac.app/Contents/app/resources
                    appDir.resolve("resources")?.let { possiblePaths.add(it.absolutePath) }
                }
            } catch (e: Exception) {
                // ignore
            }

            // 3. Check user.dir (current working directory)
            System.getProperty("user.dir")?.let { possiblePaths.add(it) }

            // 4. For IDE/gradle runs, check the build directory where prepareMacNativeLibraries copies the dylib
            try {
                val userDir = System.getProperty("user.dir")
                if (userDir != null) {
                    val buildDir = File(userDir, "build/generated/nativeDistributions/resources/macOS")
                    if (buildDir.exists()) possiblePaths.add(buildDir.absolutePath)

                    val composeAppBuildDir = File(userDir, "composeApp/build/generated/nativeDistributions/resources/macOS")
                    if (composeAppBuildDir.exists()) possiblePaths.add(composeAppBuildDir.absolutePath)
                }
            } catch (e: Exception) {
                // ignore
            }

            // Try each path
            val originalJnaPath = System.getProperty("jna.library.path")
            for (path in possiblePaths) {
                if (!File(path, "libtwofac_keychain.dylib").exists()) continue
                try {
                    System.setProperty("jna.library.path", path)
                    return Native.load(LIBRARY_NAME, MacOSKeychainNative::class.java)
                } catch (e: UnsatisfiedLinkError) {
                    continue
                }
            }

            // Restore original jna.library.path
            if (originalJnaPath != null) {
                System.setProperty("jna.library.path", originalJnaPath)
            } else {
                System.clearProperty("jna.library.path")
            }

            return null
        }
    }
}
