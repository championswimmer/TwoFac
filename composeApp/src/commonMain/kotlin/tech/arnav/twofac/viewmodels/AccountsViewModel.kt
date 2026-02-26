package tech.arnav.twofac.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import tech.arnav.twofac.lib.TwoFacLib
import tech.arnav.twofac.lib.storage.StoredAccount
import tech.arnav.twofac.session.SessionManager
import tech.arnav.twofac.companion.CompanionSyncCoordinator
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class AccountsViewModel(
    private val twoFacLib: TwoFacLib,
    private val companionSyncCoordinator: CompanionSyncCoordinator? = null,
    private val sessionManager: SessionManager? = null,
) : ViewModel() {

    companion object {
        const val REFRESH_DEBOUNCE = 100L // milliseconds
    }

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
    fun getSavedPasskey(): String? = sessionManager?.getSavedPasskey()

    /** Clear any saved passkey from the session manager. */
    fun clearSavedPasskey() { sessionManager?.clearPasskey() }

    private val _refreshTrigger = MutableStateFlow(0L)

    @OptIn(FlowPreview::class)
    private val triggerRefreshFlow = _refreshTrigger
        .filter { it > 0 }
        .debounce(REFRESH_DEBOUNCE)

    init {
        loadAccounts()

        triggerRefreshFlow
            .onEach { refreshOtpsInternal() }
            .launchIn(viewModelScope)
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

    fun loadAccountsWithOtps(passkey: String?) {
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
                    loadAccounts()
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

    fun getOtpForAccount(accountId: String): String? {
        if (!twoFacLibUnlocked) {
            _error.value = "Accounts are not loaded. Please unlock accounts first"
            return null
        }
        return _accountOtps.value.find { it.first.accountID == accountId }?.second
    }

    @OptIn(ExperimentalTime::class)
    fun refreshOtps() {
        _refreshTrigger.value = Clock.System.now().toEpochMilliseconds()
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
