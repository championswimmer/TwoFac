package tech.arnav.twofac.qr

import tech.arnav.twofac.lib.importer.google.GoogleAuthenticatorMigrationBatch
import tech.arnav.twofac.lib.importer.google.GoogleAuthenticatorMigrationDecoder

sealed interface MigrationBatchProgress {
    data class Awaiting(val scanned: Int, val total: Int) : MigrationBatchProgress
    data class Complete(val uris: List<String>) : MigrationBatchProgress
    data object Rejected : MigrationBatchProgress
}

/** Holds secrets only for the lifetime of an import screen. */
class GoogleMigrationBatchAccumulator {
    private val chunks = mutableMapOf<Int, GoogleAuthenticatorMigrationBatch>()

    fun reset() = chunks.clear()

    fun accept(batch: GoogleAuthenticatorMigrationBatch): MigrationBatchProgress {
        if (batch.batchSize !in 1..GoogleAuthenticatorMigrationDecoder.MAX_BATCH_SIZE ||
            batch.batchIndex !in 0 until batch.batchSize || batch.otpAuthUris.isEmpty()
        ) return MigrationBatchProgress.Rejected
        val first = chunks.values.firstOrNull()
        if (first != null && (first.batchId != batch.batchId || first.batchSize != batch.batchSize ||
                first.version != batch.version)) return MigrationBatchProgress.Rejected
        val previous = chunks[batch.batchIndex]
        if (previous != null && previous != batch) return MigrationBatchProgress.Rejected
        chunks[batch.batchIndex] = batch
        if (chunks.size < batch.batchSize) return MigrationBatchProgress.Awaiting(chunks.size, batch.batchSize)
        val uris = (0 until batch.batchSize).flatMap { chunks.getValue(it).otpAuthUris }
        reset()
        return MigrationBatchProgress.Complete(uris)
    }
}
