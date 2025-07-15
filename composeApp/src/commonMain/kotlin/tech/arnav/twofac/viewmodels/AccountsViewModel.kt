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
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class AccountsViewModel(
    private val twoFacLib: TwoFacLib
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

    val twoFacLibUnnocked: Boolean get() = twoFacLib.isUnlocked()

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
        if (!twoFacLibUnnocked) {
            if (passkey.isNullOrBlank()) {
                _error.value = "Passkey is required to load accounts with OTPs"
                return
            } else {
                try {
                    twoFacLib.unlock(passkey)
                } catch (e: Exception) {
                    _error.value = e.message ?: "Failed to unlock with passkey"
                    return
                }
            }
        }
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val accountOtpList = twoFacLib.getAllAccountOTPs()
            _accountOtps.value = accountOtpList
            _accounts.value = accountOtpList.map { it.first }

            _isLoading.value = false
        }
    }

    fun addAccount(uri: String, passkey: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                twoFacLib.unlock(passkey)
                val success = twoFacLib.addAccount(uri)
                if (success) {
                    loadAccounts()
                } else {
                    _error.value = "Failed to add account"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to add account"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getOtpForAccount(accountId: String, passkey: String?): String? {
        if (!twoFacLibUnnocked) {
            if (passkey.isNullOrBlank()) {
                _error.value = "Passkey is required to generate OTP"
                return null
            } else {
                try {
                    twoFacLib.unlock(passkey)
                } catch (e: Exception) {
                    _error.value = e.message ?: "Failed to unlock with passkey"
                    return null
                }
            }
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