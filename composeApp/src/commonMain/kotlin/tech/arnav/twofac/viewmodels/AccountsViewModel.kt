package tech.arnav.twofac.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import tech.arnav.twofac.companion.CompanionSyncCoordinator
import tech.arnav.twofac.lib.TwoFacLib
import tech.arnav.twofac.lib.storage.StoredAccount
import tech.arnav.twofac.qr.CameraQRCodeReader
import tech.arnav.twofac.qr.ClipboardQRCodeReader
import tech.arnav.twofac.session.SecureSessionManager
import tech.arnav.twofac.session.SessionManager
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class AccountsViewModel(
    private val twoFacLib: TwoFacLib,
    private val companionSyncCoordinator: CompanionSyncCoordinator? = null,
    private val sessionManager: SessionManager? = null,
    val cameraQRCodeReader: CameraQRCodeReader? = null,
    val clipboardQRCodeReader: ClipboardQRCodeReader? = null,
) : ViewModel() {


    private val _accounts = MutableStateFlow<List<StoredAccount.DisplayAccount>>(emptyList())
    val accounts: StateFlow<List<StoredAccount.DisplayAccount>> = _accounts.asStateFlow()

    private val _accountOtps = MutableStateFlow<List<Pair<StoredAccount.DisplayAccount, String>>>(emptyList())
    val accountOtps: StateFlow<List<Pair<StoredAccount.DisplayAccount, String>>> = _accountOtps.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val twoFacLibUnlocked: Boolean get() = twoFacLib.isUnlocked()

    /** Return a passkey previously persisted by the session manager, or null. */
    suspend fun getSavedPasskey(): String? = sessionManager?.getSavedPasskey()

    /** Clear any saved passkey from the session manager. */
    fun clearSavedPasskey() { sessionManager?.clearPasskey() }

    /** Whether secure unlock can be attempted immediately without manual passkey entry. */
    fun isSecureUnlockReady(): Boolean =
        (sessionManager as? SecureSessionManager)?.isSecureUnlockReady() ?: false

    private var otpAutoRefreshJob: Job? = null

    init {
        loadAccounts()
        startOtpAutoRefreshLoop()
    }

    fun loadAccounts() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val accountList = twoFacLib.getAllAccounts()
                _accounts.value = accountList
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load accounts"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadAccountsWithOtps(
        passkey: String?,
        fromAutoUnlock: Boolean = false,
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            if (!twoFacLibUnlocked) {
                if (passkey.isNullOrBlank()) {
                    _error.value = "Passkey is required to load accounts with OTPs"
                    _isLoading.value = false
                    return@launch
                } else {
                    try {
                        twoFacLib.unlock(passkey)
                    } catch (e: Exception) {
                        _error.value = e.message ?: "Failed to unlock with passkey"
                        _isLoading.value = false
                        return@launch
                    }
                }
            }

            try {
                val accountOtpList = twoFacLib.getAllAccountOTPs()
                _accountOtps.value = accountOtpList
                _accounts.value = accountOtpList.map { it.first }
                companionSyncCoordinator?.onAccountsUnlocked()

                // Persist the passkey for session auto-unlock (if the user opted in)
                if (passkey != null) {
                    sessionManager?.savePasskey(passkey)
                    sessionManagerForPostUnlockEnrollment(sessionManager, fromAutoUnlock)
                        ?.enrollPasskey(passkey)
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load accounts with OTPs"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addAccount(uri: String, passkey: String?, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                if (!twoFacLibUnlocked) {
                    if (passkey.isNullOrBlank()) {
                        _error.value = "Passkey is required to add an account while vault is locked"
                        onComplete(false)
                        return@launch
                    }
                    twoFacLib.unlock(passkey)
                }
                val success = twoFacLib.addAccount(uri)
                if (success) {
                    companionSyncCoordinator?.onAccountsChanged()
                    val accountOtpList = twoFacLib.getAllAccountOTPs()
                    _accountOtps.value = accountOtpList
                    _accounts.value = accountOtpList.map { it.first }
                    onComplete(true)
                } else {
                    _error.value = "Failed to add account"
                    onComplete(false)
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to add account"
                onComplete(false)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteAccount(accountId: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                if (!twoFacLibUnlocked) {
                    _error.value = "Passkey is required to delete an account while vault is locked"
                    onComplete(false)
                    return@launch
                }

                val success = twoFacLib.deleteAccount(accountId)
                if (success) {
                    val accountOtpList = twoFacLib.getAllAccountOTPs()
                    _accountOtps.value = accountOtpList
                    _accounts.value = accountOtpList.map { it.first }
                    companionSyncCoordinator?.onAccountsChanged()
                    onComplete(true)
                } else {
                    _error.value = "Failed to delete account"
                    onComplete(false)
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to delete account"
                onComplete(false)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getOtpForAccount(accountId: String): String? {
        if (!twoFacLibUnlocked) {
            _error.value = "Accounts are not loaded. Please unlock accounts first"
            return null
        }
        return _accountOtps.value.find { it.first.accountID == accountId }?.second
    }

    suspend fun getFreshOtpForAccount(accountId: String): String? {
        if (!twoFacLibUnlocked) {
            _error.value = "Accounts are not loaded. Please unlock accounts first"
            return null
        }

        return try {
            val accountOtpList = twoFacLib.getAllAccountOTPs()
            _accountOtps.value = accountOtpList
            _accounts.value = accountOtpList.map { it.first }
            accountOtpList.find { it.first.accountID == accountId }?.second
        } catch (e: Exception) {
            _error.value = e.message ?: "Failed to generate OTP"
            null
        }
    }

    /** Re-read accounts (and OTPs if unlocked) from TwoFacLib after external mutations. */
    fun reloadAccounts() {
        viewModelScope.launch {
            try {
                val accountList = twoFacLib.getAllAccounts()
                _accounts.value = accountList
                if (twoFacLibUnlocked) {
                    val accountOtpList = twoFacLib.getAllAccountOTPs()
                    _accountOtps.value = accountOtpList
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to reload accounts"
            }
        }
    }

    fun refreshOtps() {
        viewModelScope.launch {
            refreshOtpsInternal()
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun startOtpAutoRefreshLoop() {
        if (otpAutoRefreshJob?.isActive == true) return

        otpAutoRefreshJob = viewModelScope.launch {
            while (isActive) {
                if (!twoFacLibUnlocked || _accounts.value.isEmpty()) {
                    delay(1000)
                    continue
                }

                if (_accountOtps.value.isEmpty()) {
                    refreshOtpsInternal()
                    delay(1000)
                    continue
                }

                val nowEpochSeconds = Clock.System.now().epochSeconds
                val nextTotpCodeAt = _accountOtps.value
                    .asSequence()
                    .map { it.first.nextCodeAt }
                    .filter { it > 0L }
                    .minOrNull()

                if (nextTotpCodeAt != null && nowEpochSeconds >= nextTotpCodeAt) {
                    refreshOtpsInternal()
                }

                delay(1000)
            }
        }
    }

    private suspend fun refreshOtpsInternal() {
        try {
            val accountOtpList = twoFacLib.getAllAccountOTPs()
            _accountOtps.value = accountOtpList
        } catch (e: Exception) {
            _error.value = e.message ?: "Failed to refresh OTPs"
        }
    }

    fun clearError() {
        _error.value = null
    }
}

internal fun sessionManagerForPostUnlockEnrollment(
    sessionManager: SessionManager?,
    fromAutoUnlock: Boolean,
): SecureSessionManager? {
    if (fromAutoUnlock) return null
    val secureSessionManager = sessionManager as? SecureSessionManager ?: return null
    return secureSessionManager.takeIf {
        it.isSecureUnlockEnabled() && it.isSecureUnlockAvailable() && !it.isSecureUnlockReady()
    }
}
