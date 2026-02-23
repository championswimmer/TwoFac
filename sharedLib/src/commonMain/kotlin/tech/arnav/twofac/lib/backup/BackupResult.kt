package tech.arnav.twofac.lib.backup

sealed class BackupResult<out T> {
    data class Success<T>(val value: T) : BackupResult<T>()
    data class Failure(val error: BackupError) : BackupResult<Nothing>()

    inline fun <R> map(transform: (T) -> R): BackupResult<R> = when (this) {
        is Success -> Success(transform(value))
        is Failure -> this
    }
}

data class BackupError(
    val code: BackupErrorCode,
    val message: String,
    val cause: Throwable? = null,
)

enum class BackupErrorCode {
    TransportUnavailable,
    SerializationError,
    StorageError,
    NotFound,
    ValidationError,
    Unknown,
}
