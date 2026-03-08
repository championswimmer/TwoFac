package tech.arnav.twofac.backup

import io.github.xxfast.kstore.file.storeOf
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.io.files.Path
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class GoogleDriveBackupTransportTest {
    @Test
    fun availabilityDetailReflectsAuthorizationState() = runTest {
        val transport = buildTransport { request ->
            error("Unexpected request: ${request.url}")
        }

        assertEquals(
            "Google OAuth client ID is required before Drive backup can connect.",
            transport.availabilityDetail(),
        )

        assertIs<tech.arnav.twofac.lib.backup.BackupResult.Success<Unit>>(
            transport.configureAuthorization("test-client-id.apps.googleusercontent.com"),
        )

        assertEquals(
            "Google Drive backup is configured but not connected.",
            transport.availabilityDetail(),
        )
    }

    @Test
    fun deviceAuthorizationRoundTripsIntoConnectedState() = runTest {
        val transport = buildTransport { request ->
            when {
                request.url.encodedPath.endsWith("/device/code") -> respond(
                    content = """
                        {
                          "device_code": "device-code-1",
                          "user_code": "ABCD-EFGH",
                          "verification_url": "https://www.google.com/device",
                          "verification_url_complete": "https://www.google.com/device?user_code=ABCD-EFGH",
                          "expires_in": 1800,
                          "interval": 1
                        }
                    """.trimIndent(),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )

                request.url.encodedPath.endsWith("/token") -> respond(
                    content = """
                        {
                          "access_token": "access-1",
                          "refresh_token": "refresh-1",
                          "expires_in": 3600
                        }
                    """.trimIndent(),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )

                else -> error("Unexpected request: ${request.url}")
            }
        }

        assertIs<tech.arnav.twofac.lib.backup.BackupResult.Success<Unit>>(
            transport.configureAuthorization("test-client-id.apps.googleusercontent.com"),
        )

        val challenge = transport.beginAuthorization()
        assertIs<tech.arnav.twofac.lib.backup.BackupResult.Success<BackupAuthorizationChallenge>>(challenge)

        val complete = transport.completeAuthorization(challenge.value)
        assertIs<tech.arnav.twofac.lib.backup.BackupResult.Success<Unit>>(complete)
        assertEquals(BackupAuthorizationState.CONNECTED, transport.authorizationStatus().state)
    }

    @Test
    fun listBackupsUsesLogicalIdsAndRemoteIds() = runTest {
        val transport = buildConnectedTransport { request ->
            when {
                request.url.encodedPath.endsWith("/files") -> respond(
                    content = """
                        {
                          "files": [
                            {
                              "id": "drive-file-1",
                              "name": "twofac-backup-1.json",
                              "size": "128",
                              "appProperties": {
                                "logicalBackupId": "twofac-backup-123456-0.json",
                                "schemaVersion": "1"
                              }
                            }
                          ]
                        }
                    """.trimIndent(),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )

                else -> error("Unexpected request: ${request.url}")
            }
        }

        val result = transport.listBackups()
        val backups = assertIs<tech.arnav.twofac.lib.backup.BackupResult.Success<List<tech.arnav.twofac.lib.backup.BackupDescriptor>>>(result).value

        assertEquals(1, backups.size)
        assertEquals("twofac-backup-123456-0.json", backups.single().id)
        assertEquals("drive-file-1", backups.single().remoteId)
        assertEquals(123456, backups.single().createdAt)
    }

    @Test
    fun downloadResolvesByLogicalIdAndReturnsRemoteDescriptor() = runTest {
        val transport = buildConnectedTransport { request ->
            when {
                request.url.encodedPath.endsWith("/files") -> respond(
                    content = """
                        {
                          "files": [
                            {
                              "id": "drive-file-9",
                              "name": "twofac-backup-42-0.json",
                              "size": "12",
                              "appProperties": {
                                "logicalBackupId": "twofac-backup-42-0.json",
                                "schemaVersion": "1"
                              }
                            }
                          ]
                        }
                    """.trimIndent(),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )

                request.url.encodedPath.endsWith("/files/drive-file-9") -> respond(
                    content = """{"createdAt":42,"accounts":["otpauth://totp/Test:demo?secret=GEZDGNBVGY3TQOJQ&issuer=Test"]}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )

                else -> error("Unexpected request: ${request.url}")
            }
        }

        val result = transport.download("twofac-backup-42-0.json")
        val blob = assertIs<tech.arnav.twofac.lib.backup.BackupResult.Success<tech.arnav.twofac.lib.backup.BackupBlob>>(result).value
        assertEquals("drive-file-9", blob.descriptor.remoteId)
        assertEquals("twofac-backup-42-0.json", blob.descriptor.id)
    }

    private fun buildConnectedTransport(
        handler: suspend io.ktor.client.engine.mock.MockRequestHandleScope.(io.ktor.client.request.HttpRequestData) -> io.ktor.client.request.HttpResponseData,
    ): GoogleDriveBackupTransport {
        val store = authStore()
        val httpClient = HttpClient(MockEngine(handler))
        return GoogleDriveBackupTransport(
            authStore = store.apply {
                kotlinx.coroutines.runBlocking {
                    set(
                        GoogleDriveAuthState(
                            clientId = "test-client-id.apps.googleusercontent.com",
                            accessToken = "access-1",
                            refreshToken = "refresh-1",
                            accessTokenExpiresAtEpochSeconds = Long.MAX_VALUE,
                        )
                    )
                }
            },
            httpClient = httpClient,
        )
    }

    private fun buildTransport(
        handler: suspend io.ktor.client.engine.mock.MockRequestHandleScope.(io.ktor.client.request.HttpRequestData) -> io.ktor.client.request.HttpResponseData,
    ): GoogleDriveBackupTransport {
        return GoogleDriveBackupTransport(
            authStore = authStore(),
            httpClient = HttpClient(MockEngine(handler)),
        )
    }

    private fun authStore() = storeOf(
        file = run {
            val directory = Files.createTempDirectory("google-drive-auth")
            Path(directory.resolve("auth.json").toString())
        },
        default = GoogleDriveAuthState(),
    )
}
