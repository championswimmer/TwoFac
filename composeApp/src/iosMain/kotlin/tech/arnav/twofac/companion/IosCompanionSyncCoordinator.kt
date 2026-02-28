package tech.arnav.twofac.companion

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.serialization.json.Json
import platform.Foundation.NSError
import platform.Foundation.NSLog
import platform.WatchConnectivity.WCSession
import platform.WatchConnectivity.WCSessionDelegateProtocol
import platform.darwin.NSObject
import tech.arnav.twofac.lib.TwoFacLib
import tech.arnav.twofac.lib.watchsync.WatchSyncSnapshot
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class, ExperimentalForeignApi::class)
class IosCompanionSyncCoordinator(
    private val twoFacLib: TwoFacLib,
) : CompanionSyncCoordinator {

    override val companionDisplayName: String = "Apple Watch"

    private val session: WCSession? =
        if (WCSession.isSupported()) WCSession.defaultSession() else null
    private val sessionDelegate = object : NSObject(), WCSessionDelegateProtocol {
        override fun session(
            session: WCSession,
            activationDidCompleteWithState: Long,
            error: NSError?,
        ) {
            if (error != null) {
                NSLog("WCSession activation failed: %@", error.localizedDescription)
            }
        }

        override fun sessionDidBecomeInactive(session: WCSession) = Unit

        override fun sessionDidDeactivate(session: WCSession) {
            session.activateSession()
        }
    }

    init {
        session?.delegate = sessionDelegate
        session?.activateSession()
    }

    override suspend fun isCompanionActive(): Boolean {
        val activeSession = session ?: return false
        activeSession.activateSession()
        return activeSession.isPaired() && activeSession.isWatchAppInstalled()
    }

    override suspend fun forceDiscoverCompanion(): Boolean {
        val activeSession = session ?: return false
        activeSession.activateSession()
        return activeSession.isPaired() && activeSession.isWatchAppInstalled()
    }

    override suspend fun syncNow(manual: Boolean): Boolean {
        val activeSession = session ?: return false
        activeSession.activateSession()
        if (!twoFacLib.isUnlocked()) {
            NSLog("Companion sync aborted: vault is locked.")
            return false
        }

        val sourceAccounts = loadCompanionSyncSourceAccounts(twoFacLib) {
            NSLog("Companion sync aborted: no accounts available.")
        } ?: return false

        if (!isCompanionActive()) {
            NSLog("Companion sync aborted: Apple Watch companion is unavailable.")
            return false
        }

        val snapshot = buildCompanionSyncSnapshot(
            sourceAccounts = sourceAccounts,
            generatedAtEpochSec = Clock.System.now().epochSeconds,
        )
        val payloadString = Json.encodeToString(WatchSyncSnapshot.serializer(), snapshot)
        // Keep values as strings so payload is always valid property-list data for WCSession.
        val payload: Map<Any?, Any?> = mapOf(
            IOS_PAYLOAD_STRING_KEY to payloadString,
            IOS_GENERATED_AT_EPOCH_SEC_KEY to snapshot.generatedAtEpochSec.toString(),
            IOS_MANUAL_SYNC_KEY to manual.toString(),
        )

        NSLog(
            "Companion sync: sending payload. activationState=%ld accounts=%ld payloadLength=%ld",
            activeSession.activationState,
            sourceAccounts.size.toLong(),
            payloadString.length.toLong(),
        )
        activeSession.transferUserInfo(payload)
        updateApplicationContext(activeSession, payload)
        sendImmediateMessageIfReachable(activeSession, payload)
        return true
    }

    override suspend fun onAccountsUnlocked() {
        syncNow(manual = false)
    }

    override suspend fun onAccountsChanged() {
        syncNow(manual = false)
    }

    private fun updateApplicationContext(
        activeSession: WCSession,
        payload: Map<Any?, *>,
    ) {
        val success = activeSession.updateApplicationContext(payload, null)
        if (!success) {
            NSLog("Companion sync application context update failed.")
        } else {
            NSLog("Companion sync application context updated successfully.")
        }
    }

    private fun sendImmediateMessageIfReachable(
        activeSession: WCSession,
        payload: Map<Any?, Any?>,
    ) {
        if (!activeSession.isReachable()) {
            NSLog("Companion sync immediate message skipped: watch is not reachable.")
            return
        }

        activeSession.sendMessage(
            message = payload,
            replyHandler = null,
            errorHandler = { error ->
                NSLog(
                    "Companion sync immediate message failed: %@",
                    error?.localizedDescription ?: "unknown error",
                )
            },
        )
        NSLog("Companion sync immediate message sent.")
    }
}

private const val IOS_PAYLOAD_STRING_KEY = "payloadString"
private const val IOS_GENERATED_AT_EPOCH_SEC_KEY = "generatedAtEpochSec"
private const val IOS_MANUAL_SYNC_KEY = "manualSync"
