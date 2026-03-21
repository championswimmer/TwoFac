package tech.arnav.twofac.companion

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import platform.Foundation.NSError
import platform.Foundation.NSLog
import platform.WatchConnectivity.WCSession
import platform.WatchConnectivity.WCSessionActivationStateActivated
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

    private val _companionActiveFlow = MutableStateFlow(false)
    override val companionActiveFlow: StateFlow<Boolean> = _companionActiveFlow

    private val activationCompleted = CompletableDeferred<Boolean>()

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
                activationCompleted.complete(false)
            } else {
                val activated =
                    activationDidCompleteWithState == WCSessionActivationStateActivated
                activationCompleted.complete(activated)
                if (activated) {
                    updateCompanionActiveState(session)
                }
            }
        }

        override fun sessionDidBecomeInactive(session: WCSession) = Unit

        override fun sessionDidDeactivate(session: WCSession) {
            session.activateSession()
        }

        override fun sessionWatchStateDidChange(session: WCSession) {
            updateCompanionActiveState(session)
        }
    }

    init {
        session?.delegate = sessionDelegate
        session?.activateSession()
    }

    private fun updateCompanionActiveState(session: WCSession) {
        val active = session.isPaired() && session.isWatchAppInstalled()
        _companionActiveFlow.value = active
    }

    private suspend fun awaitActivation(): Boolean {
        return withTimeoutOrNull(ACTIVATION_TIMEOUT_MS) {
            activationCompleted.await()
        } ?: false
    }

    override suspend fun isCompanionActive(): Boolean {
        val activeSession = session ?: return false
        if (!awaitActivation()) return false
        val active = activeSession.isPaired() && activeSession.isWatchAppInstalled()
        _companionActiveFlow.value = active
        return active
    }

    override suspend fun forceDiscoverCompanion(): Boolean {
        val activeSession = session ?: return false
        if (!awaitActivation()) return false
        val active = activeSession.isPaired() && activeSession.isWatchAppInstalled()
        _companionActiveFlow.value = active
        return active
    }

    override suspend fun syncNow(manual: Boolean): Boolean {
        val activeSession = session ?: return false
        if (!awaitActivation()) return false
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
private const val ACTIVATION_TIMEOUT_MS = 3000L
