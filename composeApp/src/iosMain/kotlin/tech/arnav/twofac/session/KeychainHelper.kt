package tech.arnav.twofac.session

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecAccessControlCreateWithFlags
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAccessControlBiometryCurrentSet
import platform.Security.kSecAttrAccessControl
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrAccessibleWhenUnlockedThisDeviceOnly
import platform.Security.kSecAttrService
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecClass
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecMatchLimit
import platform.Security.kSecReturnData
import platform.Security.kSecUseAuthenticationContext
import platform.Security.kSecValueData
import platform.LocalAuthentication.LAContext

/**
 * Low-level Keychain CRUD helper for generic password items.
 *
 * Wraps the platform.Security cinterop calls so that IosBiometricSessionManager
 * does not need to deal with CFDictionary construction or OSStatus codes directly.
 *
 * NOTE on interop: CF constants (kSecClass, kSecClassGenericPassword, etc.) must be
 * passed directly to CFDictionaryAddValue — NOT through CFBridgingRetain. Only Kotlin/ObjC
 * objects (String, NSData, LAContext) need CFBridgingRetain to bridge to CFTypeRef.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
object KeychainHelper {

    /**
     * Save a string value into the Keychain as a generic password item.
     * Deletes any existing item with the same service+account first.
     *
     * @param service  the kSecAttrService value (e.g. "tech.arnav.twofac")
     * @param account  the kSecAttrAccount value (e.g. "vault_passkey")
     * @param value    the string to store
     * @param requireBiometric  if true, protects the item with biometric access control
     * @return true if the item was saved successfully
     */
    fun save(
        service: String,
        account: String,
        value: String,
        requireBiometric: Boolean,
    ): Boolean = memScoped {
        // Always delete existing item first (ignore not-found)
        delete(service, account)

        val nsString = NSString.create(string = value)
        val data = nsString.dataUsingEncoding(NSUTF8StringEncoding) ?: run {
            println("KeychainHelper: failed to encode value as UTF-8")
            return false
        }

        val query = CFDictionaryCreateMutable(null, 6, null, null)
        CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(query, kSecAttrService, CFBridgingRetain(service))
        CFDictionaryAddValue(query, kSecAttrAccount, CFBridgingRetain(account))
        CFDictionaryAddValue(query, kSecValueData, CFBridgingRetain(data))

        if (requireBiometric) {
            val accessControl = SecAccessControlCreateWithFlags(
                null,
                kSecAttrAccessibleWhenUnlockedThisDeviceOnly,
                kSecAccessControlBiometryCurrentSet,
                null,
            )
            if (accessControl != null) {
                CFDictionaryAddValue(query, kSecAttrAccessControl, accessControl)
            } else {
                println("KeychainHelper: SecAccessControlCreateWithFlags returned null")
            }
        } else {
            CFDictionaryAddValue(query, kSecAttrAccessible, kSecAttrAccessibleWhenUnlockedThisDeviceOnly)
        }

        val status = SecItemAdd(query, null)
        if (status != errSecSuccess) {
            println("KeychainHelper: SecItemAdd failed with OSStatus $status")
        }
        status == errSecSuccess
    }

    /**
     * Read a generic password string from the Keychain.
     *
     * If the item is biometric-protected, the system will automatically present
     * the Face ID / Touch ID prompt.
     *
     * @param service  the kSecAttrService value
     * @param account  the kSecAttrAccount value
     * @param context  optional LAContext for pre-authenticated reads
     * @return the stored string, or null if not found / auth cancelled / failed
     */
    fun read(
        service: String,
        account: String,
        context: LAContext? = null,
    ): String? = memScoped {
        val query = CFDictionaryCreateMutable(null, 6, null, null)
        CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(query, kSecAttrService, CFBridgingRetain(service))
        CFDictionaryAddValue(query, kSecAttrAccount, CFBridgingRetain(account))
        CFDictionaryAddValue(query, kSecReturnData, kCFBooleanTrue)
        CFDictionaryAddValue(query, kSecMatchLimit, kSecMatchLimitOne)

        if (context != null) {
            CFDictionaryAddValue(query, kSecUseAuthenticationContext, CFBridgingRetain(context))
        }

        val result = alloc<CFTypeRefVar>()
        val status = SecItemCopyMatching(query, result.ptr)

        if (status != errSecSuccess) {
            println("KeychainHelper: SecItemCopyMatching failed with OSStatus $status")
            return null
        }

        val data = CFBridgingRelease(result.value) as? NSData ?: return null
        NSString.create(data = data, encoding = NSUTF8StringEncoding)?.toString()
    }

    /**
     * Check whether a Keychain item exists (without retrieving its data).
     * Does NOT trigger biometric prompts.
     *
     * @return true if an item with matching service+account exists
     */
    fun exists(service: String, account: String): Boolean = memScoped {
        val query = CFDictionaryCreateMutable(null, 4, null, null)
        CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(query, kSecAttrService, CFBridgingRetain(service))
        CFDictionaryAddValue(query, kSecAttrAccount, CFBridgingRetain(account))
        CFDictionaryAddValue(query, kSecMatchLimit, kSecMatchLimitOne)

        val status = SecItemCopyMatching(query, null)
        status == errSecSuccess
    }

    /**
     * Delete a generic password from the Keychain.
     * Does not require biometric authentication.
     *
     * @return true if an item was found and deleted, false if not found
     */
    fun delete(service: String, account: String): Boolean {
        val query = CFDictionaryCreateMutable(null, 3, null, null)
        CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(query, kSecAttrService, CFBridgingRetain(service))
        CFDictionaryAddValue(query, kSecAttrAccount, CFBridgingRetain(account))

        val status = SecItemDelete(query)
        return status == errSecSuccess || status == errSecItemNotFound
    }
}
