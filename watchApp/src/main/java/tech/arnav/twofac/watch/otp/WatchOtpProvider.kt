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
import tech.arnav.twofac.lib.storage.StoredAccount
import tech.arnav.twofac.lib.uri.OtpAuthURI
import tech.arnav.twofac.lib.watchsync.WatchSyncSnapshot
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class WatchOtpProvider(
    private val clock: Clock = Clock.System,
) {

    fun buildCodes(
        snapshot: WatchSyncSnapshot,
        nowEpochSec: Long = clock.now().epochSeconds
    ): List<WatchOtpEntry> {
        return snapshot.accounts.map { account ->
            try {
                val otp = OtpAuthURI.parse(account.otpAuthUri)
                when (otp) {
                    is TOTP -> {
                        val nextCodeAt = otp.nextCodeAt(nowEpochSec)
                        WatchOtpEntry.Valid(
                            account = StoredAccount.DisplayAccount(
                                accountID = account.accountId,
                                accountLabel = account.accountLabel,
                                nextCodeAt = nextCodeAt,
                            ),
                            issuer = account.issuer,
                            otpCode = otp.generateOTP(nowEpochSec),
                            nextRefreshAtEpochSec = nextCodeAt,
                            periodSec = otp.timeInterval,
                        )
                    }

                    is HOTP -> WatchOtpEntry.Valid(
                        account = StoredAccount.DisplayAccount(
                            accountID = account.accountId,
                            accountLabel = account.accountLabel,
                        ),
                        issuer = account.issuer,
                        otpCode = otp.generateOTP(0),
                        nextRefreshAtEpochSec = null,
                        periodSec = null,
                    )

                    else -> WatchOtpEntry.Invalid(
                        account = StoredAccount.DisplayAccount(
                            accountID = account.accountId,
                            accountLabel = account.accountLabel,
                        ),
                        issuer = account.issuer,
                        reason = "Unsupported OTP type",
                    )
                }
            } catch (error: IllegalArgumentException) {
                WatchOtpEntry.Invalid(
                    account = StoredAccount.DisplayAccount(
                        accountID = account.accountId,
                        accountLabel = account.accountLabel,
                    ),
                    issuer = account.issuer,
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
    val account: StoredAccount.DisplayAccount
    val issuer: String?

    data class Valid(
        override val account: StoredAccount.DisplayAccount,
        override val issuer: String?,
        val otpCode: String,
        val nextRefreshAtEpochSec: Long?,
        val periodSec: Long?,
    ) : WatchOtpEntry

    data class Invalid(
        override val account: StoredAccount.DisplayAccount,
        override val issuer: String?,
        val reason: String,
    ) : WatchOtpEntry
}
