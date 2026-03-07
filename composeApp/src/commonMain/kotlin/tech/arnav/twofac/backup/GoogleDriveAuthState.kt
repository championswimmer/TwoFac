package tech.arnav.twofac.backup

import kotlinx.serialization.Serializable

@Serializable
data class GoogleDriveAuthState(
    val clientId: String? = null,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val accessTokenExpiresAtEpochSeconds: Long? = null,
)
