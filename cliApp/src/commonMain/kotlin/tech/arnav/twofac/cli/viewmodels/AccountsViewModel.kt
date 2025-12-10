package tech.arnav.twofac.cli.viewmodels

import tech.arnav.twofac.lib.TwoFacLib
import tech.arnav.twofac.lib.storage.StoredAccount

typealias DisplayAccountsStatic = List<Pair<StoredAccount.DisplayAccount, String>>

class AccountsViewModel(val twoFacLib: TwoFacLib) {

    suspend fun unlock(passkey: String) {
        return twoFacLib.unlock(passkey)
    }

    suspend fun showAllAccountOTPs(): DisplayAccountsStatic = twoFacLib.getAllAccountOTPs()

    suspend fun addAccount(accountURI: String) = twoFacLib.addAccount(accountURI)

}