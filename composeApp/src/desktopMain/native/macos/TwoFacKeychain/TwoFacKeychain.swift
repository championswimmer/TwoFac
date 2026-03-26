import Foundation
import Security
import LocalAuthentication

private let keychainService = "tech.arnav.twofac"
private let keychainAccount = "vault_passkey"

private func logError(_ message: String) {
    fputs("[TwoFacKeychain] \(message)\n", stderr)
}

private func createAccessControl() -> SecAccessControl? {
    let flags: SecAccessControlCreateFlags
    if #available(macOS 10.12.2, *) {
        flags = [.biometryCurrentSet, .userPresence]
    } else {
        flags = [.userPresence]
    }
    
    var error: Unmanaged<CFError>?
    guard let accessControl = SecAccessControlCreateWithFlags(
        kCFAllocatorDefault,
        kSecAttrAccessibleWhenUnlockedThisDeviceOnly,
        flags,
        &error
    ) else {
        if let error = error?.takeRetainedValue() {
            let errorDescription = CFErrorCopyDescription(error) as String? ?? "unknown"
            logError("Failed to create access control: \(errorDescription)")
        }
        return nil
    }
    return accessControl
}

@_cdecl("twofac_keychain_is_available")
func twofacKeychainIsAvailable() -> Int32 {
    let context = LAContext()
    var error: NSError?
    
    let isAvailable = context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &error)
    
    if let error = error {
        logError("Biometry availability check error: \(error.localizedDescription)")
    }
    
    return isAvailable ? 1 : 0
}

@_cdecl("twofac_keychain_store")
func twofacKeychainStore(_ passkey: UnsafePointer<Int8>, passkeyLen: Int) -> Int32 {
    guard passkeyLen > 0 && passkeyLen <= 1024 else {
        logError("Invalid passkey length: \(passkeyLen)")
        return -1
    }
    
    let passkeyData = Data(bytes: passkey, count: passkeyLen)
    
    twofacKeychainDelete()
    
    guard let accessControl = createAccessControl() else {
        return -2
    }
    
    let query: [String: Any] = [
        kSecClass as String: kSecClassGenericPassword,
        kSecAttrService as String: keychainService,
        kSecAttrAccount as String: keychainAccount,
        kSecValueData as String: passkeyData,
        kSecAttrAccessControl as String: accessControl
    ]
    
    let status = SecItemAdd(query as CFDictionary, nil)
    
    if status != errSecSuccess {
        logError("Failed to store passkey: \(SecCopyErrorMessageString(status, nil) as String? ?? "unknown error")")
        return Int32(status)
    }
    
    return 0
}

@_cdecl("twofac_keychain_retrieve")
func twofacKeychainRetrieve(_ buffer: UnsafeMutablePointer<Int8>, bufferLen: Int, outLen: UnsafeMutablePointer<Int>) -> Int32 {
    guard bufferLen > 0 else {
        logError("Invalid buffer length: \(bufferLen)")
        return -1
    }
    
    let query: [String: Any] = [
        kSecClass as String: kSecClassGenericPassword,
        kSecAttrService as String: keychainService,
        kSecAttrAccount as String: keychainAccount,
        kSecReturnData as String: true,
        kSecMatchLimit as String: kSecMatchLimitOne
    ]
    
    var result: AnyObject?
    let status = SecItemCopyMatching(query as CFDictionary, &result)
    
    if status == errSecItemNotFound {
        logError("Passkey not found in keychain")
        return -2
    }
    
    if status != errSecSuccess {
        logError("Failed to retrieve passkey: \(SecCopyErrorMessageString(status, nil) as String? ?? "unknown error")")
        return Int32(status)
    }
    
    guard let data = result as? Data else {
        logError("Invalid data type returned from keychain")
        return -3
    }
    
    let dataLen = data.count
    if dataLen > bufferLen {
        logError("Buffer too small: need \(dataLen), have \(bufferLen)")
        return -4
    }
    
    data.withUnsafeBytes { ptr in
        if let baseAddress = ptr.baseAddress {
            memcpy(buffer, baseAddress, dataLen)
        }
    }
    
    outLen.pointee = dataLen
    return 0
}

@_cdecl("twofac_keychain_delete")
func twofacKeychainDelete() -> Int32 {
    let query: [String: Any] = [
        kSecClass as String: kSecClassGenericPassword,
        kSecAttrService as String: keychainService,
        kSecAttrAccount as String: keychainAccount
    ]
    
    let status = SecItemDelete(query as CFDictionary)
    
    if status != errSecSuccess && status != errSecItemNotFound {
        logError("Failed to delete passkey: \(SecCopyErrorMessageString(status, nil) as String? ?? "unknown error")")
        return Int32(status)
    }
    
    return 0
}

@_cdecl("twofac_keychain_exists")
func twofacKeychainExists() -> Int32 {
    let query: [String: Any] = [
        kSecClass as String: kSecClassGenericPassword,
        kSecAttrService as String: keychainService,
        kSecAttrAccount as String: keychainAccount,
        kSecReturnData as String: false
    ]
    
    var result: AnyObject?
    let status = SecItemCopyMatching(query as CFDictionary, &result)
    
    return status == errSecSuccess ? 1 : 0
}
