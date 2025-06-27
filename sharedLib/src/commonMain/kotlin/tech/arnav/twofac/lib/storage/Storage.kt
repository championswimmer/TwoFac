package tech.arnav.twofac.lib.storage

import tech.arnav.twofac.lib.otp.OTP

interface Storage {

    fun getAccountList(): List<OTP>

    fun getAccount(accountLabel: String): OTP?

    fun saveAccount(account: OTP): Boolean

}