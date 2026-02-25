package tech.arnav.twofac.watch.otp

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.isActive
import tech.arnav.twofac.lib.otp.HOTP
import tech.arnav.twofac.lib.otp.TOTP
import tech.arnav.twofac.lib.uri.OtpAuthURI
import tech.arnav.twofac.lib.watchsync.WatchSyncSnapshot
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class WatchOtpProvider(
    private val clock: Clock = Clock.System,
) {

    @OptIn(ExperimentalTime::class)
    suspend fun buildCodes(
        snapshot: WatchSyncSnapshot,
        nowEpochSec: Long = clock.now().epochSeconds
    ): List<WatchOtpEntry> {
        return snapshot.accounts.map { account ->
            try {
                val otp = OtpAuthURI.parse(account.otpAuthUri)
                when (otp) {
                    is TOTP -> WatchOtpEntry.Valid(
                        accountId = account.accountId,
                        issuer = account.issuer,
                        accountLabel = account.accountLabel,
                        otpCode = otp.generateOTP(nowEpochSec),
                        nextRefreshAtEpochSec = otp.nextCodeAt(nowEpochSec),
                        periodSec = otp.timeInterval,
                    )

                    is HOTP -> WatchOtpEntry.Valid(
                        accountId = account.accountId,
                        issuer = account.issuer,
                        accountLabel = account.accountLabel,
                        otpCode = otp.generateOTP(0),
                        nextRefreshAtEpochSec = null,
                        periodSec = null,
                    )

                    else -> WatchOtpEntry.Invalid(
                        accountId = account.accountId,
                        issuer = account.issuer,
                        accountLabel = account.accountLabel,
                        reason = "Unsupported OTP type",
                    )
                }
            } catch (error: IllegalArgumentException) {
                WatchOtpEntry.Invalid(
                    accountId = account.accountId,
                    issuer = account.issuer,
                    accountLabel = account.accountLabel,
                    reason = error.message ?: "Invalid otpauth URI",
                )
            }
        }
    }

    fun ticker(snapshotFlow: Flow<WatchSyncSnapshot?>): Flow<List<WatchOtpEntry>> {
        return snapshotFlow.flatMapLatest { snapshot ->
            if (snapshot == null) {
                flowOf(emptyList())
            } else {
                flow {
                    while (currentCoroutineContext().isActive) {
                        emit(buildCodes(snapshot))
                        delay(1000)
                    }
                }
            }
        }
    }
}

sealed interface WatchOtpEntry {
    val accountId: String
    val issuer: String?
    val accountLabel: String

    data class Valid(
        override val accountId: String,
        override val issuer: String?,
        override val accountLabel: String,
        val otpCode: String,
        val nextRefreshAtEpochSec: Long?,
        val periodSec: Long?,
    ) : WatchOtpEntry

    data class Invalid(
        override val accountId: String,
        override val issuer: String?,
        override val accountLabel: String,
        val reason: String,
    ) : WatchOtpEntry
}
