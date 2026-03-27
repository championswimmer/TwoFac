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

        private fun debugLog(message: String) {
            val logFile = java.io.File(System.getProperty("user.home"), "twofac-native-debug.log")
            val timestamp = java.time.Instant.now().toString()
            logFile.appendText("[$timestamp] $message\n")
        }

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
            debugLog("=== Starting native library load ===")
            debugLog("os.name: ${System.getProperty("os.name")}")
            debugLog("os.arch: ${System.getProperty("os.arch")}")
            debugLog("java.library.path: ${System.getProperty("java.library.path")}")
            debugLog("user.dir: ${System.getProperty("user.dir")}")
            
            // First try default JNA behavior (system paths, java.library.path, etc.)
            try {
                debugLog("Trying default JNA load...")
                return Native.load(LIBRARY_NAME, MacOSKeychainNative::class.java)
            } catch (e: UnsatisfiedLinkError) {
                debugLog("Default load failed: ${e.message}")
            }

            // Try to find the library in app bundle resources
            val possiblePaths = mutableListOf<String>()

            // 1. Check java.library.path
            System.getProperty("java.library.path")?.split(File.pathSeparator)?.forEach { 
                possiblePaths.add(it) 
            }

            // 2. For jpackage apps, check relative to the app location
            try {
                val codeSourceLocation = MacOSKeychainNative::class.java.protectionDomain.codeSource?.location
                debugLog("codeSourceLocation: $codeSourceLocation")
                if (codeSourceLocation != null) {
                    val appDir = File(codeSourceLocation.toURI()).parentFile
                    debugLog("appDir: ${appDir?.absolutePath}")
                    possiblePaths.add(appDir.absolutePath)
                    // Check Resources directory in app bundle (TwoFac.app/Contents/Resources)
                    appDir.parentFile?.resolve("Resources")?.let { 
                        debugLog("Adding path: ${it.absolutePath}")
                        possiblePaths.add(it.absolutePath) 
                    }
                    // Also check app directory (TwoFac.app/Contents/app)
                    appDir.parentFile?.resolve("app")?.let { 
                        debugLog("Adding path: ${it.absolutePath}")
                        possiblePaths.add(it.absolutePath) 
                    }
                    // Check resources subdirectory within app (TwoFac.app/Contents/app/resources)
                    appDir.resolve("resources")?.let { 
                        debugLog("Adding path: ${it.absolutePath}")
                        possiblePaths.add(it.absolutePath) 
                    }
                }
            } catch (e: Exception) {
                debugLog("Failed to determine code source location: ${e.message}")
            }

            // 3. Check user.dir (current working directory)
            System.getProperty("user.dir")?.let { possiblePaths.add(it) }

            // 4. For IDE/gradle runs, check the build directory where prepareMacNativeLibraries copies the dylib
            try {
                val userDir = System.getProperty("user.dir")
                if (userDir != null) {
                    // Check if we're running from a project directory (IDE/gradle run)
                    val buildDir = File(userDir, "build/generated/nativeDistributions/resources/macOS")
                    if (buildDir.exists()) {
                        debugLog("Adding build dir path: ${buildDir.absolutePath}")
                        possiblePaths.add(buildDir.absolutePath)
                    }
                    // Also check relative to composeApp if running from project root
                    val composeAppBuildDir = File(userDir, "composeApp/build/generated/nativeDistributions/resources/macOS")
                    if (composeAppBuildDir.exists()) {
                        debugLog("Adding composeApp build dir path: ${composeAppBuildDir.absolutePath}")
                        possiblePaths.add(composeAppBuildDir.absolutePath)
                    }
                }
            } catch (e: Exception) {
                debugLog("Failed to check build directories: ${e.message}")
            }

            debugLog("Possible paths to search: ${possiblePaths.size}")
            possiblePaths.forEachIndexed { index, path -> 
                debugLog("  [$index] $path")
            }

            // Try each path
            val originalJnaPath = System.getProperty("jna.library.path")
            for (path in possiblePaths) {
                val dylibFile = File(path, "libtwofac_keychain.dylib")
                debugLog("Checking path: $path (dylib exists: ${dylibFile.exists()})")
                try {
                    System.setProperty("jna.library.path", path)
                    debugLog("Set jna.library.path to: $path")
                    val result = Native.load(LIBRARY_NAME, MacOSKeychainNative::class.java)
                    debugLog("SUCCESS! Loaded library from $path")
                    return result
                } catch (e: UnsatisfiedLinkError) {
                    debugLog("Failed to load from $path: ${e.message}")
                    continue
                }
            }

            // Restore original jna.library.path
            if (originalJnaPath != null) {
                System.setProperty("jna.library.path", originalJnaPath)
            } else {
                System.clearProperty("jna.library.path")
            }

            debugLog("Failed to load native library from all locations")
            return null
        }
    }
}
