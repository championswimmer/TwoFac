package tech.arnav.twofac.navigation

import kotlinx.serialization.Serializable

@Serializable
object Home

@Serializable
object Accounts

@Serializable
data class AccountDetail(val accountId: String)

@Serializable
object Settings

@Serializable
object AddAccount