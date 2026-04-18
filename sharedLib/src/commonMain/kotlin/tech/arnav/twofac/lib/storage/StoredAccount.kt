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
    val tagIds: List<String> = emptyList(),
) {
    data class DisplayAccount(
        val accountID: String,
        val accountLabel: String,
        val nextCodeAt: Long = 0L,
        val issuer: String? = null,
        val tags: List<StoredTag> = emptyList(),
    )

    fun forDisplay(
        nextCodeAt: Long? = 0L,
        issuer: String? = null,
        accountLabel: String = this.accountLabel,
        tags: List<StoredTag> = emptyList(),
    ): DisplayAccount {
        return DisplayAccount(
            accountID = accountID.toString(),
            accountLabel = accountLabel,
            nextCodeAt = nextCodeAt ?: 0L,
            issuer = issuer,
            tags = tags,
        )
    }
}
