package tech.arnav.twofac.lib.backup

import tech.arnav.twofac.lib.PublicApi

@PublicApi
sealed class BackupResult<out T> {
    data class Success<T>(val value: T) : BackupResult<T>()
    data class Failure(val message: String, val cause: Throwable? = null) : BackupResult<Nothing>()
}
