package tech.arnav.twofac.backup

import io.github.xxfast.kstore.KStore
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.contentType
import kotlinx.coroutines.delay
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import tech.arnav.twofac.lib.backup.BackupBlob
import tech.arnav.twofac.lib.backup.BackupDescriptor
import tech.arnav.twofac.lib.backup.BackupResult
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class GoogleDriveBackupTransport(
    private val authStore: KStore<GoogleDriveAuthState>,
    private val httpClient: HttpClient,
) : AuthorizableBackupTransport {
    override val id: String = "gdrive-appdata"

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    override suspend fun isAvailable(): Boolean {
        return authorizationStatus().state == BackupAuthorizationState.CONNECTED
    }

    override suspend fun authorizationStatus(): BackupAuthorizationStatus {
        val state = authStore.get() ?: GoogleDriveAuthState()
        return when {
            state.clientId.isNullOrBlank() -> {
                BackupAuthorizationStatus(
                    state = BackupAuthorizationState.NOT_CONFIGURED,
                    detail = "Google OAuth client ID is required before Drive backup can connect.",
                )
            }
            state.refreshToken.isNullOrBlank() && state.accessToken.isNullOrBlank() -> {
                BackupAuthorizationStatus(
                    state = BackupAuthorizationState.DISCONNECTED,
                    detail = "Google Drive backup is configured but not connected.",
                )
            }
            else -> {
                BackupAuthorizationStatus(
                    state = BackupAuthorizationState.CONNECTED,
                    detail = "Google Drive appDataFolder backup is connected.",
                )
            }
        }
    }

    override suspend fun configureAuthorization(clientId: String): BackupResult<Unit> {
        val trimmed = clientId.trim()
        if (trimmed.isEmpty()) {
            return BackupResult.Failure("Google OAuth client ID cannot be empty")
        }

        val current = authStore.get() ?: GoogleDriveAuthState()
        authStore.set(
            current.copy(
                clientId = trimmed,
                accessToken = null,
                refreshToken = null,
                accessTokenExpiresAtEpochSeconds = null,
            )
        )
        return BackupResult.Success(Unit)
    }

    override suspend fun beginAuthorization(): BackupResult<BackupAuthorizationChallenge> {
        val state = authStore.get() ?: GoogleDriveAuthState()
        val clientId = state.clientId?.trim().orEmpty()
        if (clientId.isEmpty()) {
            return BackupResult.Failure("Configure a Google OAuth client ID before connecting Drive backup")
        }

        return try {
            val response = httpClient.post(GOOGLE_DEVICE_CODE_URL) {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(
                    FormDataContent(
                        Parameters.build {
                            append("client_id", clientId)
                            append("scope", GOOGLE_DRIVE_APPDATA_SCOPE)
                        }
                    )
                )
            }
            val body = response.bodyAsText()
            if (response.status != HttpStatusCode.OK) {
                val error = parseError(body)
                return BackupResult.Failure(error ?: "Failed to start Google device authorization")
            }
            val payload = json.decodeFromString<GoogleDeviceAuthorizationResponse>(body)
            BackupResult.Success(
                BackupAuthorizationChallenge(
                    verificationUri = payload.verificationUri,
                    userCode = payload.userCode,
                    verificationUriComplete = payload.verificationUriComplete,
                    deviceCode = payload.deviceCode,
                    expiresInSeconds = payload.expiresInSeconds,
                    pollIntervalSeconds = payload.intervalSeconds ?: DEFAULT_DEVICE_POLL_INTERVAL_SECONDS,
                )
            )
        } catch (e: Exception) {
            BackupResult.Failure("Failed to start Google Drive authorization: ${e.message}", e)
        }
    }

    override suspend fun completeAuthorization(
        challenge: BackupAuthorizationChallenge,
    ): BackupResult<Unit> {
        val current = authStore.get() ?: GoogleDriveAuthState()
        val clientId = current.clientId?.trim().orEmpty()
        if (clientId.isEmpty()) {
            return BackupResult.Failure("Google OAuth client ID is missing")
        }

        var pollDelaySeconds = challenge.pollIntervalSeconds.coerceAtLeast(1)
        val deadline = nowEpochSeconds() + challenge.expiresInSeconds

        while (nowEpochSeconds() < deadline) {
            val response = try {
                httpClient.post(GOOGLE_TOKEN_URL) {
                    contentType(ContentType.Application.FormUrlEncoded)
                    setBody(
                        FormDataContent(
                            Parameters.build {
                                append("client_id", clientId)
                                append("device_code", challenge.deviceCode)
                                append(
                                    "grant_type",
                                    "urn:ietf:params:oauth:grant-type:device_code",
                                )
                            }
                        )
                    )
                }
            } catch (e: Exception) {
                return BackupResult.Failure("Failed to complete Google Drive authorization: ${e.message}", e)
            }

            val body = response.bodyAsText()
            if (response.status == HttpStatusCode.OK) {
                val token = json.decodeFromString<GoogleTokenResponse>(body)
                authStore.set(
                    current.copy(
                        accessToken = token.accessToken,
                        refreshToken = token.refreshToken ?: current.refreshToken,
                        accessTokenExpiresAtEpochSeconds = nowEpochSeconds() + token.expiresInSeconds,
                    )
                )
                return BackupResult.Success(Unit)
            }

            val error = json.decodeFromString<GoogleErrorResponse>(body)
            when (error.error) {
                "authorization_pending" -> delay(pollDelaySeconds * 1_000)
                "slow_down" -> {
                    pollDelaySeconds += 5
                    delay(pollDelaySeconds * 1_000)
                }
                "expired_token" -> return BackupResult.Failure("Google device authorization expired before it was approved")
                else -> return BackupResult.Failure("Google authorization failed: ${error.errorDescription ?: error.error}")
            }
        }

        return BackupResult.Failure("Google device authorization timed out")
    }

    override suspend fun disconnectAuthorization(): BackupResult<Unit> {
        val current = authStore.get() ?: GoogleDriveAuthState()
        authStore.set(
            current.copy(
                accessToken = null,
                refreshToken = null,
                accessTokenExpiresAtEpochSeconds = null,
            )
        )
        return BackupResult.Success(Unit)
    }

    override suspend fun listBackups(): BackupResult<List<BackupDescriptor>> {
        val accessTokenResult = ensureAccessToken()
        if (accessTokenResult is BackupResult.Failure) return accessTokenResult
        val accessToken = (accessTokenResult as BackupResult.Success).value

        return try {
            val response = httpClient.get("$GOOGLE_DRIVE_API_ROOT/files") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $accessToken")
                }
                parameter("spaces", "appDataFolder")
                parameter("fields", "files(id,name,size,appProperties)")
                parameter("q", "trashed = false")
            }

            val body = response.bodyAsText()
            if (response.status != HttpStatusCode.OK) {
                return BackupResult.Failure(parseError(body) ?: "Failed to list Google Drive backups")
            }

            val payload = json.decodeFromString<GoogleDriveListResponse>(body)
            BackupResult.Success(
                payload.files.map { file ->
                    BackupDescriptor(
                        id = file.appProperties?.logicalBackupId ?: file.name,
                        transportId = id,
                        createdAt = parseTimestampFromFilename(file.appProperties?.logicalBackupId ?: file.name),
                        schemaVersion = file.appProperties?.schemaVersion?.toIntOrNull() ?: 1,
                        byteSize = file.size?.toLongOrNull() ?: 0L,
                        remoteId = file.id,
                        checksum = file.appProperties?.checksum,
                    )
                }.sortedByDescending { it.createdAt }
            )
        } catch (e: Exception) {
            BackupResult.Failure("Failed to list Google Drive backups: ${e.message}", e)
        }
    }

    override suspend fun upload(
        content: ByteArray,
        descriptor: BackupDescriptor,
    ): BackupResult<BackupDescriptor> {
        val accessTokenResult = ensureAccessToken()
        if (accessTokenResult is BackupResult.Failure) return accessTokenResult
        val accessToken = (accessTokenResult as BackupResult.Success).value

        return try {
            val response = httpClient.post(GOOGLE_DRIVE_UPLOAD_URL) {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $accessToken")
                }
                parameter("uploadType", "multipart")
                parameter("fields", "id,name,size,appProperties")
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append(
                                "metadata",
                                json.encodeToString(
                                    GoogleDriveCreateRequest(
                                        name = descriptor.id,
                                        parents = listOf("appDataFolder"),
                                        appProperties = GoogleDriveAppProperties(
                                            logicalBackupId = descriptor.id,
                                            schemaVersion = descriptor.schemaVersion.toString(),
                                            checksum = descriptor.checksum,
                                        ),
                                    )
                                ),
                                headers = io.ktor.http.Headers.build {
                                    append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                                    append(
                                        HttpHeaders.ContentDisposition,
                                        ContentDisposition.Inline.toString(),
                                    )
                                },
                            )
                            append(
                                "file",
                                content,
                                headers = io.ktor.http.Headers.build {
                                    append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                                    append(
                                        HttpHeaders.ContentDisposition,
                                        "form-data; name=\"file\"; filename=\"${descriptor.id}\"",
                                    )
                                },
                            )
                        }
                    )
                )
            }

            val body = response.bodyAsText()
            if (response.status != HttpStatusCode.OK) {
                return BackupResult.Failure(parseError(body) ?: "Failed to upload Google Drive backup")
            }

            val file = json.decodeFromString<GoogleDriveFileMetadata>(body)
            BackupResult.Success(
                descriptor.copy(
                    remoteId = file.id,
                    checksum = file.appProperties?.checksum,
                )
            )
        } catch (e: Exception) {
            BackupResult.Failure("Failed to upload Google Drive backup: ${e.message}", e)
        }
    }

    override suspend fun download(backupId: String): BackupResult<BackupBlob> {
        val accessTokenResult = ensureAccessToken()
        if (accessTokenResult is BackupResult.Failure) return accessTokenResult
        val accessToken = (accessTokenResult as BackupResult.Success).value
        val file = findDriveFileByBackupId(accessToken, backupId)
        if (file is BackupResult.Failure) return file
        val metadata = (file as BackupResult.Success).value

        return try {
            val response = httpClient.get("$GOOGLE_DRIVE_API_ROOT/files/${metadata.id}") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $accessToken")
                }
                parameter("alt", "media")
            }
            if (response.status != HttpStatusCode.OK) {
                return BackupResult.Failure(parseError(response.bodyAsText()) ?: "Failed to download Google Drive backup")
            }
            val bytes = response.bodyAsText().encodeToByteArray()
            BackupResult.Success(
                BackupBlob(
                    content = bytes,
                    descriptor = BackupDescriptor(
                        id = backupId,
                        transportId = id,
                        createdAt = parseTimestampFromFilename(backupId),
                        schemaVersion = metadata.appProperties?.schemaVersion?.toIntOrNull() ?: 1,
                        byteSize = bytes.size.toLong(),
                        remoteId = metadata.id,
                        checksum = metadata.appProperties?.checksum,
                    ),
                )
            )
        } catch (e: Exception) {
            BackupResult.Failure("Failed to download Google Drive backup: ${e.message}", e)
        }
    }

    override suspend fun delete(backupId: String): BackupResult<Unit> {
        val accessTokenResult = ensureAccessToken()
        if (accessTokenResult is BackupResult.Failure) return accessTokenResult
        val accessToken = (accessTokenResult as BackupResult.Success).value
        val file = findDriveFileByBackupId(accessToken, backupId)
        if (file is BackupResult.Failure) return file
        val metadata = (file as BackupResult.Success).value

        return try {
            val response = httpClient.delete("$GOOGLE_DRIVE_API_ROOT/files/${metadata.id}") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $accessToken")
                }
            }
            if (response.status != HttpStatusCode.NoContent && response.status != HttpStatusCode.OK) {
                return BackupResult.Failure(parseError(response.bodyAsText()) ?: "Failed to delete Google Drive backup")
            }
            BackupResult.Success(Unit)
        } catch (e: Exception) {
            BackupResult.Failure("Failed to delete Google Drive backup: ${e.message}", e)
        }
    }

    private suspend fun findDriveFileByBackupId(
        accessToken: String,
        backupId: String,
    ): BackupResult<GoogleDriveFileMetadata> {
        return try {
            val response = httpClient.get("$GOOGLE_DRIVE_API_ROOT/files") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $accessToken")
                }
                parameter("spaces", "appDataFolder")
                parameter("fields", "files(id,name,size,appProperties)")
                parameter("q", "name = '$backupId' and trashed = false")
            }
            val body = response.bodyAsText()
            if (response.status != HttpStatusCode.OK) {
                return BackupResult.Failure(parseError(body) ?: "Failed to locate Google Drive backup")
            }

            val payload = json.decodeFromString<GoogleDriveListResponse>(body)
            val file = payload.files.firstOrNull()
                ?: return BackupResult.Failure("Backup not found in Google Drive: $backupId")
            BackupResult.Success(file)
        } catch (e: Exception) {
            BackupResult.Failure("Failed to locate Google Drive backup: ${e.message}", e)
        }
    }

    private suspend fun ensureAccessToken(): BackupResult<String> {
        val state = authStore.get() ?: GoogleDriveAuthState()
        val clientId = state.clientId?.trim().orEmpty()
        if (clientId.isEmpty()) {
            return BackupResult.Failure("Google OAuth client ID is not configured")
        }

        val accessToken = state.accessToken
        val expiresAt = state.accessTokenExpiresAtEpochSeconds ?: 0L
        if (!accessToken.isNullOrBlank() && expiresAt > nowEpochSeconds() + TOKEN_EXPIRY_SKEW_SECONDS) {
            return BackupResult.Success(accessToken)
        }

        val refreshToken = state.refreshToken
        if (refreshToken.isNullOrBlank()) {
            return BackupResult.Failure("Google Drive backup is not connected")
        }

        return try {
            val response = httpClient.post(GOOGLE_TOKEN_URL) {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(
                    FormDataContent(
                        Parameters.build {
                            append("client_id", clientId)
                            append("refresh_token", refreshToken)
                            append("grant_type", "refresh_token")
                        }
                    )
                )
            }
            val body = response.bodyAsText()
            if (response.status != HttpStatusCode.OK) {
                return BackupResult.Failure(parseError(body) ?: "Failed to refresh Google Drive access token")
            }
            val token = json.decodeFromString<GoogleTokenResponse>(body)
            val updatedState = state.copy(
                accessToken = token.accessToken,
                refreshToken = token.refreshToken ?: refreshToken,
                accessTokenExpiresAtEpochSeconds = nowEpochSeconds() + token.expiresInSeconds,
            )
            authStore.set(updatedState)
            BackupResult.Success(updatedState.accessToken.orEmpty())
        } catch (e: Exception) {
            BackupResult.Failure("Failed to refresh Google Drive access token: ${e.message}", e)
        }
    }

    private fun parseError(body: String): String? {
        return try {
            val parsed = json.decodeFromString<GoogleErrorResponse>(body)
            parsed.errorDescription ?: parsed.error
        } catch (_: Exception) {
            null
        }
    }

    private fun parseTimestampFromFilename(filename: String): Long {
        return filename
            .removePrefix("twofac-backup-")
            .removeSuffix(".json")
            .substringBefore('-')
            .toLongOrNull() ?: 0L
    }

    @OptIn(ExperimentalTime::class)
    private fun nowEpochSeconds(): Long = Clock.System.now().epochSeconds

    private companion object {
        const val GOOGLE_DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
        const val GOOGLE_DEVICE_CODE_URL = "https://oauth2.googleapis.com/device/code"
        const val GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token"
        const val GOOGLE_DRIVE_API_ROOT = "https://www.googleapis.com/drive/v3"
        const val GOOGLE_DRIVE_UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files"
        const val DEFAULT_DEVICE_POLL_INTERVAL_SECONDS = 5L
        const val TOKEN_EXPIRY_SKEW_SECONDS = 60L
    }
}

@Serializable
private data class GoogleDeviceAuthorizationResponse(
    @SerialName("device_code") val deviceCode: String,
    @SerialName("user_code") val userCode: String,
    @SerialName("verification_url")
    val verificationUrl: String? = null,
    @SerialName("verification_uri")
    val verificationUriAlt: String? = null,
    @SerialName("verification_url_complete")
    val verificationUrlComplete: String? = null,
    @SerialName("verification_uri_complete")
    val verificationUriCompleteAlt: String? = null,
    @SerialName("expires_in") val expiresInSeconds: Long,
    @SerialName("interval") val intervalSeconds: Long? = null,
) {
    val verificationUri: String
        get() = verificationUriAlt ?: verificationUrl.orEmpty()

    val verificationUriComplete: String?
        get() = verificationUriCompleteAlt ?: verificationUrlComplete
}

@Serializable
private data class GoogleTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_in") val expiresInSeconds: Long,
)

@Serializable
private data class GoogleErrorResponse(
    val error: String,
    @SerialName("error_description") val errorDescription: String? = null,
)

@Serializable
private data class GoogleDriveListResponse(
    val files: List<GoogleDriveFileMetadata> = emptyList(),
)

@Serializable
private data class GoogleDriveFileMetadata(
    val id: String,
    val name: String,
    val size: String? = null,
    @SerialName("appProperties") val appProperties: GoogleDriveAppProperties? = null,
)

@Serializable
private data class GoogleDriveCreateRequest(
    val name: String,
    val parents: List<String>,
    @SerialName("appProperties") val appProperties: GoogleDriveAppProperties,
)

@Serializable
private data class GoogleDriveAppProperties(
    val logicalBackupId: String,
    val schemaVersion: String,
    val checksum: String? = null,
)
