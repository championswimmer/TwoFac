package tech.arnav.twofac.qr

import tech.arnav.twofac.lib.importer.google.GoogleAuthenticatorMigrationBatch

sealed interface QRCodeReadResult {
    data class Success(
        val otpAuthUris: List<String>,
        val migrationBatch: GoogleAuthenticatorMigrationBatch? = null,
    ) : QRCodeReadResult {
        init { require(otpAuthUris.isNotEmpty()) }
    }
    data object Canceled : QRCodeReadResult
    data object PermissionDenied : QRCodeReadResult
    data object Unsupported : QRCodeReadResult
    data class DecodeFailure(val reason: String) : QRCodeReadResult
}
