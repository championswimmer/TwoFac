package tech.arnav.twofac.backup

import tech.arnav.twofac.lib.backup.BackupResult
import tech.arnav.twofac.lib.backup.BackupTransport

enum class BackupAuthorizationState {
    NOT_CONFIGURED,
    DISCONNECTED,
    CONNECTED,
}

data class BackupAuthorizationStatus(
    val state: BackupAuthorizationState,
    val detail: String? = null,
)

data class BackupAuthorizationChallenge(
    val verificationUri: String,
    val userCode: String,
    val verificationUriComplete: String? = null,
    val deviceCode: String,
    val expiresInSeconds: Long,
    val pollIntervalSeconds: Long,
)

interface AuthorizableBackupTransport : BackupTransport {
    suspend fun authorizationStatus(): BackupAuthorizationStatus
    suspend fun configureAuthorization(clientId: String): BackupResult<Unit>
    suspend fun beginAuthorization(): BackupResult<BackupAuthorizationChallenge>
    suspend fun completeAuthorization(challenge: BackupAuthorizationChallenge): BackupResult<Unit>
    suspend fun disconnectAuthorization(): BackupResult<Unit>
}
