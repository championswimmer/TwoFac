@file:OptIn(ExperimentalUuidApi::class)

package tech.arnav.twofac.lib.storage

import kotlinx.io.bytestring.ByteString
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class StoredAccount constructor(
    val accountID: Uuid,
    val accountLabel: String,
    val salt: ByteString,
    val encryptedURI: ByteString,
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