@file:OptIn(ExperimentalUuidApi::class)

package tech.arnav.twofac.lib.storage

import kotlinx.serialization.Serializable
import tech.arnav.twofac.lib.PublicApi
import tech.arnav.twofac.lib.presentation.issuer.IssuerIconCatalog
import tech.arnav.twofac.lib.presentation.issuer.IssuerIconMatch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@PublicApi
@Serializable
data class StoredAccount constructor(
    val accountID: Uuid,
    val accountLabel: String,
    val salt: String,
    val encryptedURI: String,
) {
    data class DisplayAccount(
        val accountID: String,
        val accountLabel: String,
        val nextCodeAt: Long = 0L,
        val issuer: String? = null,
        val issuerIconMatch: IssuerIconMatch = IssuerIconCatalog.resolveIssuerIcon(issuer),
    )

    fun forDisplay(
        nextCodeAt: Long? = 0L,
        issuer: String? = null,
        accountLabel: String = this.accountLabel,
    ): DisplayAccount {
        return DisplayAccount(
            accountID = accountID.toString(),
            accountLabel = accountLabel,
            nextCodeAt = nextCodeAt ?: 0L,
            issuer = issuer,
        )
    }
}
