@file:OptIn(ExperimentalUuidApi::class)

package tech.arnav.twofac.lib.storage

import kotlinx.serialization.Serializable
import tech.arnav.twofac.lib.PublicApi
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
    )

    fun forDisplay(nextCodeAt: Long? = 0L): DisplayAccount {
        return DisplayAccount(
            accountID = accountID.toString(),
            accountLabel = accountLabel,
            nextCodeAt = nextCodeAt ?: 0L,
        )
    }
}