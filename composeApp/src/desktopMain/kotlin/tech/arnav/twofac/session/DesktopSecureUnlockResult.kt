package tech.arnav.twofac.session

/**
 * Result of a secure unlock operation.
 */
sealed class DesktopSecureUnlockResult {
    data class Success(val passkey: String) : DesktopSecureUnlockResult()
    data object Cancelled : DesktopSecureUnlockResult()
    data class Error(val message: String, val cause: Throwable? = null) : DesktopSecureUnlockResult()
}
