import Foundation
import Security
import LocalAuthentication

private let keychainService = "tech.arnav.twofac"
private let keychainAccount = "vault_passkey"

private func logError(_ message: String) {
    fputs("[TwoFacKeychain] \(message)\n", stderr)
}

/// Synchronously evaluates a biometric (Touch ID) policy on the calling thread.
/// Returns true if the user authenticated successfully, false otherwise.
private func evaluateBiometrics(reason: String) -> Bool {
    let context = LAContext()
    var error: NSError?

    guard context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &error) else {
        if let error = error {
            logError("Biometrics not available: \(error.localizedDescription)")
        }
        return false
    }

    let semaphore = DispatchSemaphore(value: 0)
    var success = false

    context.evaluatePolicy(
        .deviceOwnerAuthenticationWithBiometrics,
        localizedReason: reason
    ) { result, evalError in
        if let evalError = evalError {
            logError("Biometric evaluation error: \(evalError.localizedDescription)")
        }
        success = result
        semaphore.signal()
    }

    semaphore.wait()
    return success
}

@_cdecl("twofac_keychain_is_available")
public func twofacKeychainIsAvailable() -> Int32 {
    let context = LAContext()
    var error: NSError?

    let isAvailable = context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &error)

    if let error = error {
        logError("Biometry availability check error: \(error.localizedDescription)")
    }

    return isAvailable ? 1 : 0
}

@_cdecl("twofac_keychain_store")
public func twofacKeychainStore(_ passkey: UnsafePointer<Int8>, passkeyLen: Int) -> Int32 {
    guard passkeyLen > 0 && passkeyLen <= 1024 else {
        logError("Invalid passkey length: \(passkeyLen)")
        return -1
    }

    // Prompt Touch ID before storing — app-level biometric gate
    guard evaluateBiometrics(reason: "Authenticate to enable biometric unlock for TwoFac") else {
        logError("Biometric authentication failed or was cancelled during store")
        return -10
    }

    let passkeyData = Data(bytes: passkey, count: passkeyLen)

    // Delete any existing item first
    twofacKeychainDelete()

    // Store without SecAccessControl — no entitlement required
    let query: [String: Any] = [
        kSecClass as String: kSecClassGenericPassword,
        kSecAttrService as String: keychainService,
        kSecAttrAccount as String: keychainAccount,
        kSecValueData as String: passkeyData,
        kSecAttrAccessible as String: kSecAttrAccessibleWhenUnlockedThisDeviceOnly
    ]

    let status = SecItemAdd(query as CFDictionary, nil)

    if status != errSecSuccess {
        logError("Failed to store passkey: \(SecCopyErrorMessageString(status, nil) as String? ?? "unknown error")")
        return Int32(status)
    }

    return 0
}

@_cdecl("twofac_keychain_retrieve")
public func twofacKeychainRetrieve(_ buffer: UnsafeMutablePointer<Int8>, bufferLen: Int, outLen: UnsafeMutablePointer<Int>) -> Int32 {
    guard bufferLen > 0 else {
        logError("Invalid buffer length: \(bufferLen)")
        return -1
    }

    // Prompt Touch ID before retrieving — app-level biometric gate
    guard evaluateBiometrics(reason: "Authenticate to unlock TwoFac vault") else {
        logError("Biometric authentication failed or was cancelled during retrieve")
        return -10
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
public func twofacKeychainDelete() -> Int32 {
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
public func twofacKeychainExists() -> Int32 {
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
