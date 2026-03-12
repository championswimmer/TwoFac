package tech.arnav.twofac.backup

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Tasks
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.Url
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.appendPathSegments
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import tech.arnav.twofac.lib.backup.BackupBlob
import tech.arnav.twofac.lib.backup.BackupDescriptor
import tech.arnav.twofac.lib.backup.BackupProviderIds
import tech.arnav.twofac.lib.backup.BackupResult
import tech.arnav.twofac.lib.backup.BackupTransport

/**
 * Android Google Drive backup transport using appDataFolder + drive.appdata scope.
 *
 * Auth is handled through Google Identity AuthorizationClient and may prompt the user
 * on first use.
 */
class GoogleDriveAppDataBackupTransport(
    appContext: Context,
    private val activityProvider: () -> FragmentActivity,
) : BackupTransport {
    override val id: String = BackupProviderIds.GOOGLE_DRIVE_APPDATA
    override val displayName: String = "Google Drive"
    override val supportsAutomaticRestore: Boolean = true
    override val requiresAuthentication: Boolean = true

    private val context = appContext.applicationContext
    private val credentials: GoogleCloudCredentials? by lazy {
        loadGoogleCloudCredentials(context)
    }
    private val httpClient = HttpClient(Android) {
        expectSuccess = false
        install(HttpTimeout) {
            requestTimeoutMillis = NETWORK_TIMEOUT_MS.toLong()
            connectTimeoutMillis = NETWORK_TIMEOUT_MS.toLong()
            socketTimeoutMillis = NETWORK_TIMEOUT_MS.toLong()
        }
    }

    private val knownDescriptorsById = linkedMapOf<String, BackupDescriptor>()

    override suspend fun isAvailable(): Boolean {
        val playServicesStatus = GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(context, 0)
        if (playServicesStatus != ConnectionResult.SUCCESS) return false
        return credentials != null
    }

    override suspend fun listBackups(): BackupResult<List<BackupDescriptor>> {
        val tokenResult = authorizeForDriveAccess()
        if (tokenResult is BackupResult.Failure) return tokenResult
        val accessToken = (tokenResult as BackupResult.Success).value

        val response = executeRequest(
            method = HttpMethod.Get,
            url = DRIVE_LIST_BACKUPS_URL,
            accessToken = accessToken,
        )
        if (response is BackupResult.Failure) return response

        val body = (response as BackupResult.Success).value
        val descriptors = parseListResponse(body)
            .filter { metadata ->
                metadata.name.startsWith(BACKUP_FILE_PREFIX) && metadata.name.endsWith(".json")
            }
            .map { metadata ->
                val createdAt =
                    metadata.createdAt
                        ?: metadata.modifiedAt
                        ?: parseTimestampFromFilename(metadata.name)
                BackupDescriptor(
                    id = metadata.id,
                    transportId = id,
                    createdAt = createdAt,
                    byteSize = metadata.size ?: 0L,
                )
            }
            .sortedByDescending { it.createdAt }

        synchronized(knownDescriptorsById) {
            knownDescriptorsById.clear()
            descriptors.forEach { descriptor ->
                knownDescriptorsById[descriptor.id] = descriptor
            }
        }

        return BackupResult.Success(descriptors)
    }

    override suspend fun upload(
        content: ByteArray,
        descriptor: BackupDescriptor,
    ): BackupResult<BackupDescriptor> {
        val tokenResult = authorizeForDriveAccess()
        if (tokenResult is BackupResult.Failure) return tokenResult
        val accessToken = (tokenResult as BackupResult.Success).value

        val boundary = "twofac-${UUID.randomUUID()}"
        val metadata = JSONObject().apply {
            put("name", descriptor.id)
            put("parents", JSONArray().put("appDataFolder"))
            put(
                "appProperties",
                JSONObject().apply {
                    put("transport", id)
                    put("logicalBackupId", descriptor.id)
                    put("schemaVersion", descriptor.schemaVersion.toString())
                    credentials?.projectId?.let { put("projectId", it) }
                }
            )
        }

        val multipartBody = buildMultipartBody(
            boundary = boundary,
            metadataJson = metadata.toString(),
            content = content,
        )

        val response = executeRequest(
            method = HttpMethod.Post,
            url = DRIVE_MULTIPART_UPLOAD_URL,
            accessToken = accessToken,
            contentType = "multipart/related; boundary=$boundary",
            requestBody = multipartBody,
        )
        if (response is BackupResult.Failure) return response

        val uploaded = parseFileMetadata((response as BackupResult.Success).value)
            ?: return BackupResult.Failure(
                "Google Drive upload succeeded but response metadata was missing"
            )

        val uploadedDescriptor = BackupDescriptor(
            id = uploaded.id,
            transportId = id,
            createdAt = uploaded.createdAt ?: descriptor.createdAt,
            byteSize = uploaded.size ?: descriptor.byteSize,
            schemaVersion = descriptor.schemaVersion,
        )

        synchronized(knownDescriptorsById) {
            knownDescriptorsById[uploadedDescriptor.id] = uploadedDescriptor
        }

        return BackupResult.Success(uploadedDescriptor)
    }

    override suspend fun download(backupId: String): BackupResult<BackupBlob> {
        val tokenResult = authorizeForDriveAccess()
        if (tokenResult is BackupResult.Failure) return tokenResult
        val accessToken = (tokenResult as BackupResult.Success).value

        val downloadResponse = executeRequest(
            method = HttpMethod.Get,
            url = driveFileContentUrl(backupId),
            accessToken = accessToken,
        )
        if (downloadResponse is BackupResult.Failure) return downloadResponse
        val content = (downloadResponse as BackupResult.Success).value

        val knownDescriptor = synchronized(knownDescriptorsById) { knownDescriptorsById[backupId] }
        val descriptor = knownDescriptor ?: BackupDescriptor(
            id = backupId,
            transportId = id,
            createdAt = 0L,
            byteSize = content.size.toLong(),
        )

        return BackupResult.Success(
            BackupBlob(
                content = content,
                descriptor = descriptor,
            )
        )
    }

    override suspend fun delete(backupId: String): BackupResult<Unit> {
        val tokenResult = authorizeForDriveAccess()
        if (tokenResult is BackupResult.Failure) return tokenResult
        val accessToken = (tokenResult as BackupResult.Success).value

        val response = executeRequest(
            method = HttpMethod.Delete,
            url = driveFileUrl(backupId),
            accessToken = accessToken,
        )
        if (response is BackupResult.Failure) return response

        synchronized(knownDescriptorsById) {
            knownDescriptorsById.remove(backupId)
        }
        return BackupResult.Success(Unit)
    }

    private suspend fun authorizeForDriveAccess(): BackupResult<String> {
        val oauthCredentials = credentials
            ?: return BackupResult.Failure(
                "Missing Google OAuth config. Place google-cloud-credentials.json in androidApp/src/main/assets/"
            )
        if (oauthCredentials.clientId.isBlank()) {
            return BackupResult.Failure("Google OAuth config is invalid: installed.client_id is missing")
        }

        val activity = runCatching { activityProvider() }
            .getOrElse { error ->
                return BackupResult.Failure(
                    "No active Android activity available for Google Drive authorization",
                    error,
                )
            }

        val authorizationClient = Identity.getAuthorizationClient(activity)
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(GOOGLE_DRIVE_APPDATA_SCOPE)))
            .build()

        val initialResult = try {
            withContext(Dispatchers.IO) {
                Tasks.await(authorizationClient.authorize(request))
            }
        } catch (e: Exception) {
            val message = (e as? ApiException)?.status?.statusMessage ?: e.message
            return BackupResult.Failure("Google Drive authorization failed: ${message ?: "unknown error"}", e)
        }

        val finalResult = if (initialResult.hasResolution()) {
            val pendingIntent = initialResult.pendingIntent
                ?: return BackupResult.Failure("Google Drive authorization requires user consent")

            val resolutionData = try {
                withContext(Dispatchers.Main.immediate) {
                    val result = activity.awaitIntentSenderResult(pendingIntent)
                    if (result.resultCode != Activity.RESULT_OK) return@withContext null
                    result.data
                }
            } catch (e: Exception) {
                return BackupResult.Failure(
                    "Unable to launch Google authorization UI: ${e.message}",
                    e,
                )
            }

            if (resolutionData == null) {
                return BackupResult.Failure("Google Drive authorization was cancelled")
            }

            try {
                authorizationClient.getAuthorizationResultFromIntent(resolutionData)
            } catch (e: Exception) {
                return BackupResult.Failure(
                    "Google authorization did not return a valid result: ${e.message}",
                    e,
                )
            }
        } else {
            initialResult
        }

        val token = finalResult.accessToken
        if (token.isNullOrBlank()) {
            return BackupResult.Failure(
                "Google authorization succeeded but no access token was returned"
            )
        }
        return BackupResult.Success(token)
    }

    private suspend fun FragmentActivity.awaitIntentSenderResult(
        pendingIntent: PendingIntent,
    ): androidx.activity.result.ActivityResult {
        return suspendCancellableCoroutine { continuation ->
            val launcherKey = "twofac-gdrive-auth-${UUID.randomUUID()}"
            lateinit var launcher: ActivityResultLauncher<IntentSenderRequest>

            launcher = activityResultRegistry.register(
                launcherKey,
                ActivityResultContracts.StartIntentSenderForResult(),
            ) { result ->
                launcher.unregister()
                if (continuation.isActive) continuation.resume(result)
            }

            continuation.invokeOnCancellation {
                launcher.unregister()
            }

            try {
                launcher.launch(IntentSenderRequest.Builder(pendingIntent).build())
            } catch (e: Exception) {
                launcher.unregister()
                continuation.resumeWithException(e)
            }
        }
    }

    private fun parseListResponse(body: ByteArray): List<DriveFileMetadata> {
        val root = runCatching { JSONObject(body.toString(StandardCharsets.UTF_8)) }
            .getOrNull() ?: return emptyList()

        val files = root.optJSONArray("files") ?: return emptyList()
        val items = mutableListOf<DriveFileMetadata>()
        for (i in 0 until files.length()) {
            val file = files.optJSONObject(i) ?: continue
            val metadata = parseFileMetadata(file)
            if (metadata != null) items += metadata
        }
        return items
    }

    private fun parseFileMetadata(body: ByteArray): DriveFileMetadata? {
        val root = runCatching { JSONObject(body.toString(StandardCharsets.UTF_8)) }
            .getOrNull() ?: return null
        return parseFileMetadata(root)
    }

    private fun parseFileMetadata(json: JSONObject): DriveFileMetadata? {
        val id = json.optString("id").takeIf { it.isNotBlank() } ?: return null
        val name = json.optString("name", id)
        val createdTime = json.opt("createdTime")?.toString()
            ?.takeIf { it.isNotBlank() && it != "null" }
        val modifiedTime = json.opt("modifiedTime")?.toString()
            ?.takeIf { it.isNotBlank() && it != "null" }
        val sizeRaw = json.opt("size")?.toString()
            ?.takeIf { it.isNotBlank() && it != "null" }
        val createdAt = parseDriveTimestamp(createdTime)
        val modifiedAt = parseDriveTimestamp(modifiedTime)
        val size = sizeRaw?.toLongOrNull()

        return DriveFileMetadata(
            id = id,
            name = name,
            createdAt = createdAt,
            modifiedAt = modifiedAt,
            size = size,
        )
    }

    private fun parseDriveTimestamp(value: String?): Long? {
        val input = value?.takeIf { it.isNotBlank() } ?: return null
        return runCatching { Instant.parse(input).epochSecond }.getOrNull()
    }

    private fun parseTimestampFromFilename(filename: String): Long {
        val raw = filename
            .removePrefix(BACKUP_FILE_PREFIX)
            .removeSuffix(".json")
        return raw.substringBefore('-').toLongOrNull() ?: 0L
    }

    private fun buildMultipartBody(
        boundary: String,
        metadataJson: String,
        content: ByteArray,
    ): ByteArray {
        val out = ByteArrayOutputStream()

        fun writeUtf8(value: String) {
            out.write(value.toByteArray(StandardCharsets.UTF_8))
        }

        writeUtf8("--$boundary\r\n")
        writeUtf8("Content-Type: application/json; charset=UTF-8\r\n\r\n")
        writeUtf8(metadataJson)
        writeUtf8("\r\n")

        writeUtf8("--$boundary\r\n")
        writeUtf8("Content-Type: application/json\r\n\r\n")
        out.write(content)
        writeUtf8("\r\n")

        writeUtf8("--$boundary--\r\n")
        return out.toByteArray()
    }

    private suspend fun executeRequest(
        method: HttpMethod,
        url: Url,
        accessToken: String,
        contentType: String? = null,
        requestBody: ByteArray? = null,
    ): BackupResult<ByteArray> {
        return try {
            val response = httpClient.request(url) {
                this.method = method
                header("Authorization", "Bearer $accessToken")
                accept(ContentType.Application.Json)
                contentType?.let { header("Content-Type", it) }
                requestBody?.let { setBody(it) }
            }
            val body: ByteArray = response.body()

            if (response.status.value !in 200..299) {
                return BackupResult.Failure(
                    parseApiError(
                        statusCode = response.status.value,
                        response = body,
                    )
                )
            }

            BackupResult.Success(body)
        } catch (e: Exception) {
            BackupResult.Failure("Google Drive request failed: ${e.message}", e)
        }
    }

    private fun parseApiError(statusCode: Int, response: ByteArray): String {
        val rawBody = response.toString(StandardCharsets.UTF_8)
        val apiMessage = runCatching {
            JSONObject(rawBody)
                .optJSONObject("error")
                ?.optString("message")
                ?.takeIf { it.isNotBlank() }
        }.getOrNull()

        return if (!apiMessage.isNullOrBlank()) {
            "Google Drive API error (HTTP $statusCode): $apiMessage"
        } else {
            "Google Drive API error (HTTP $statusCode)"
        }
    }

    private fun driveFileUrl(backupId: String): Url = URLBuilder(DRIVE_FILES_ENDPOINT)
        .apply {
            appendPathSegments(backupId)
        }
        .build()

    private fun driveFileContentUrl(backupId: String): Url = URLBuilder(driveFileUrl(backupId).toString())
        .apply {
            parameters.append("alt", "media")
        }
        .build()
}

private data class GoogleCloudCredentials(
    val clientId: String,
    val projectId: String?,
)

private data class DriveFileMetadata(
    val id: String,
    val name: String,
    val createdAt: Long?,
    val modifiedAt: Long?,
    val size: Long?,
)

private fun loadGoogleCloudCredentials(context: Context): GoogleCloudCredentials? {
    return runCatching {
        context.assets.open(GOOGLE_CLOUD_CREDENTIALS_ASSET).bufferedReader().use { reader ->
            val root = JSONObject(reader.readText())
            val installed = root.optJSONObject("installed") ?: return@use null
            val clientId = installed.optString("client_id")
            if (clientId.isBlank()) return@use null

            val projectId = installed.optString("project_id").takeIf { it.isNotBlank() }
            GoogleCloudCredentials(clientId = clientId, projectId = projectId)
        }
    }.getOrNull()
}

private const val DRIVE_FILES_ENDPOINT = "https://www.googleapis.com/drive/v3/files"
private const val DRIVE_UPLOAD_ENDPOINT = "https://www.googleapis.com/upload/drive/v3/files"
private const val DRIVE_FILE_FIELDS = "id,name,createdTime,modifiedTime,size"
private val DRIVE_LIST_BACKUPS_URL: Url = URLBuilder(DRIVE_FILES_ENDPOINT)
    .apply {
        parameters.append("spaces", "appDataFolder")
        parameters.append("q", "trashed=false and 'appDataFolder' in parents")
        parameters.append("fields", "files($DRIVE_FILE_FIELDS)")
        parameters.append("pageSize", "100")
    }
    .build()

private val DRIVE_MULTIPART_UPLOAD_URL: Url = URLBuilder(DRIVE_UPLOAD_ENDPOINT)
    .apply {
        parameters.append("uploadType", "multipart")
        parameters.append("fields", DRIVE_FILE_FIELDS)
    }
    .build()

private const val GOOGLE_DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
private const val GOOGLE_CLOUD_CREDENTIALS_ASSET = "google-cloud-credentials.json"
private const val BACKUP_FILE_PREFIX = "twofac-backup-"
private const val NETWORK_TIMEOUT_MS = 20_000
