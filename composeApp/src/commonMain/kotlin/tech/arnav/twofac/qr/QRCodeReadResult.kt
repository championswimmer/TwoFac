package tech.arnav.twofac.qr

sealed interface QRCodeReadResult {
    data class Success(val otpAuthUri: String) : QRCodeReadResult
    data object Canceled : QRCodeReadResult
    data object PermissionDenied : QRCodeReadResult
    data object Unsupported : QRCodeReadResult
    data class DecodeFailure(val reason: String) : QRCodeReadResult
}
