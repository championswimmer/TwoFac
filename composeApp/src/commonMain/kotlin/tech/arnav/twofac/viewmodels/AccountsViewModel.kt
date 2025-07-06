package tech.arnav.twofac.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tech.arnav.twofac.lib.TwoFacLib
import tech.arnav.twofac.lib.storage.StoredAccount

class AccountsViewModel(
    private val twoFacLib: TwoFacLib
) : ViewModel() {

    private val _accounts = MutableStateFlow<List<StoredAccount.DisplayAccount>>(emptyList())
    val accounts: StateFlow<List<StoredAccount.DisplayAccount>> = _accounts.asStateFlow()

    private val _accountOtps = MutableStateFlow<List<Pair<StoredAccount.DisplayAccount, String>>>(emptyList())
    val accountOtps: StateFlow<List<Pair<StoredAccount.DisplayAccount, String>>> = _accountOtps.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadAccounts()
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

    fun loadAccountsWithOtps(passkey: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                twoFacLib.unlock(passkey)
                val accountOtpList = twoFacLib.getAllAccountOTPs()
                _accountOtps.value = accountOtpList
                _accounts.value = accountOtpList.map { it.first }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load accounts with OTPs"
            } finally {
                _isLoading.value = false
            }
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

    fun getOtpForAccount(accountId: String, passkey: String): String? {
        return try {
            twoFacLib.unlock(passkey)
            _accountOtps.value.find { it.first.accountID == accountId }?.second
        } catch (e: Exception) {
            _error.value = e.message ?: "Failed to generate OTP"
            null
        }
    }

    fun refreshOtps() {
        viewModelScope.launch {
            try {
                val accountOtpList = twoFacLib.getAllAccountOTPs()
                _accountOtps.value = accountOtpList
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to refresh OTPs"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}