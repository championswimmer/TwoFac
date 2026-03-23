package tech.arnav.twofac.session

/**
 * Secure session contract for web targets that gate unlock with WebAuthn.
 */
interface WebAuthnSessionManager : SecureSessionManager {
    /**
     * Returns true if a WebAuthn credential and encrypted passkey are stored,
     * meaning the vault can be unlocked via passkey/biometric.
     *
     * This is a fast synchronous check — it does NOT trigger a WebAuthn prompt.
     */
    fun isPasskeyEnrolled(): Boolean
}
