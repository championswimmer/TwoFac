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
)