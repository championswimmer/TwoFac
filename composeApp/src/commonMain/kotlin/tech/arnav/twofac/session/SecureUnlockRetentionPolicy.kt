package tech.arnav.twofac.session

import kotlinx.serialization.Serializable

@Serializable
enum class SecureUnlockRetentionPolicy {
    PROMPT_EVERY_TIME,
    RETAIN_FOR_CURRENT_SESSION,
}

@Serializable
enum class SecureUnlockRetentionScope {
    APP_SESSION,
    BROWSER_SESSION,
}

/**
 * Optional secure-session capability for platforms that can reuse a previously
 * authenticated unlock for the current runtime session.
 */
interface SessionRetentionCapableSecureSessionManager : SecureSessionManager {
    /** Whether the current platform/runtime supports current-session retention. */
    fun supportsSessionRetention(): Boolean

    /** The current retention policy selected by the user. */
    fun getSecureUnlockRetentionPolicy(): SecureUnlockRetentionPolicy

    /** Update the selected retention policy. */
    fun setSecureUnlockRetentionPolicy(policy: SecureUnlockRetentionPolicy)

    /** Copy scope presented to the user when retention is supported. */
    fun getSecureUnlockRetentionScope(): SecureUnlockRetentionScope = SecureUnlockRetentionScope.APP_SESSION
}

internal suspend fun SessionRetentionCapableSecureSessionManager.readRetainedPasskey(
    sessionPasskeyCache: SessionPasskeyCache,
): String? {
    if (!supportsSessionRetention()) return null
    if (getSecureUnlockRetentionPolicy() != SecureUnlockRetentionPolicy.RETAIN_FOR_CURRENT_SESSION) {
        return null
    }
    return sessionPasskeyCache.read()
}

internal fun SessionRetentionCapableSecureSessionManager.writeRetainedPasskey(
    sessionPasskeyCache: SessionPasskeyCache,
    passkey: String,
) {
    if (!supportsSessionRetention()) return
    if (getSecureUnlockRetentionPolicy() == SecureUnlockRetentionPolicy.RETAIN_FOR_CURRENT_SESSION) {
        sessionPasskeyCache.write(passkey)
    } else {
        sessionPasskeyCache.clear()
    }
}

internal fun SessionRetentionCapableSecureSessionManager.clearRetainedPasskey(
    sessionPasskeyCache: SessionPasskeyCache,
) {
    sessionPasskeyCache.clear()
}
