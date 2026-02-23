package tech.arnav.twofac.lib.backup

import tech.arnav.twofac.lib.PublicApi

@PublicApi
data class BackupBlob(
    val content: ByteArray,
    val descriptor: BackupDescriptor,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BackupBlob) return false
        return content.contentEquals(other.content) && descriptor == other.descriptor
    }

    override fun hashCode(): Int {
        var result = content.contentHashCode()
        result = 31 * result + descriptor.hashCode()
        return result
    }
}
