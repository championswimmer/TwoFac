package tech.arnav.twofac.lib.backup

import dev.whyoleg.cryptography.CryptographyProvider
import tech.arnav.twofac.lib.crypto.DefaultCryptoTools
import tech.arnav.twofac.lib.crypto.Encoding.toByteString
import tech.arnav.twofac.lib.storage.Storage
import tech.arnav.twofac.lib.storage.StorageUtils.toOTP
import tech.arnav.twofac.lib.uri.OtpAuthURI

class BackupService(
    private val storage: Storage,
    private val payloadCodec: BackupPayloadCodec = BackupPayloadCodec(),
) {

    private val cryptoTools = DefaultCryptoTools(CryptographyProvider.Default)

    suspend fun createPlaintextBackup(
        passkey: String,
        transport: BackupTransport,
        fileName: String? = null,
        appVersion: String? = null,
    ): BackupResult<BackupDescriptor> {
        if (passkey.isBlank()) {
            return BackupResult.Failure(
                BackupError(
                    code = BackupErrorCode.ValidationError,
                    message = "Passkey is required to export backups",
                )
            )
        }
        if (!transport.isAvailable()) {
            return BackupResult.Failure(
                BackupError(
                    code = BackupErrorCode.TransportUnavailable,
                    message = "Backup transport ${transport.id} is not available",
                )
            )
        }

        val accounts = storage.getAccountList()
        val snapshots = accounts.mapNotNull { stored ->
            try {
                val signingKey = cryptoTools.createSigningKey(passkey, stored.salt.toByteString())
                val otp = stored.toOTP(signingKey)
                BackupAccountSnapshot(
                    id = stored.accountID.toString(),
                    label = stored.accountLabel,
                    otpAuthUri = OtpAuthURI.create(otp),
                )
            } catch (_: Exception) {
                null
            }
        }

        val encodedResult = payloadCodec.encode(
            accounts = snapshots,
            appVersion = appVersion,
        )
        if (encodedResult is BackupResult.Failure) return encodedResult
        encodedResult as BackupResult.Success

        val payload = encodedResult.value
        val uploadRequest = UploadBackupRequest(
            blob = payload.blob,
            backupName = fileName ?: payload.backupName,
            schemaVersion = payload.schemaVersion,
            encryption = BackupEncryption.Plaintext,
            createdAtMillis = payload.createdAtMillis,
        )

        return transport.upload(uploadRequest)
    }

    suspend fun restorePlaintextBackup(
        passkey: String,
        transport: BackupTransport,
        backupId: String,
    ): BackupResult<BackupImportSummary> {
        if (passkey.isBlank()) {
            return BackupResult.Failure(
                BackupError(
                    code = BackupErrorCode.ValidationError,
                    message = "Passkey is required to import backups",
                )
            )
        }

        val downloadResult = transport.download(backupId)
        if (downloadResult is BackupResult.Failure) return downloadResult
        downloadResult as BackupResult.Success

        val payloadResult = payloadCodec.decode(downloadResult.value)
        if (payloadResult is BackupResult.Failure) return payloadResult
        payloadResult as BackupResult.Success

        var imported = 0
        var failed = 0
        payloadResult.value.accounts.forEach { snapshot ->
            try {
                val otp = OtpAuthURI.parse(snapshot.otpAuthUri)
                val signingKey = cryptoTools.createSigningKey(passkey)
                val storedAccount = otp.toStoredAccount(signingKey)
                val saved = storage.saveAccount(storedAccount)
                if (saved) imported++ else failed++
            } catch (_: Exception) {
                failed++
            }
        }
        return BackupResult.Success(
            BackupImportSummary(
                imported = imported,
                failed = failed,
            )
        )
    }

    suspend fun listBackups(transport: BackupTransport): BackupResult<List<BackupDescriptor>> {
        if (!transport.isAvailable()) {
            return BackupResult.Failure(
                BackupError(
                    code = BackupErrorCode.TransportUnavailable,
                    message = "Backup transport ${transport.id} is not available",
                )
            )
        }
        return transport.listBackups()
    }
}
