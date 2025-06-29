@file:OptIn(ExperimentalUuidApi::class)

package tech.arnav.twofac.lib.storage

import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

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
    )

    fun forDisplay(): DisplayAccount {
        return DisplayAccount(
            accountID = accountID.toString(),
            accountLabel = accountLabel
        )
    }
}