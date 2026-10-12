package tech.arnav.twofac.qr

import tech.arnav.twofac.lib.importer.google.GoogleAuthenticatorMigrationBatch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class GoogleMigrationBatchAccumulatorTest {
    private fun batch(index: Int, size: Int = 2, id: Int = 42, version: Int = 1) =
        GoogleAuthenticatorMigrationBatch(listOf("uri-$index"), version, size, index, id)

    @Test
    fun singleChunkCompletesAndClearsBuffer() {
        val accumulator = GoogleMigrationBatchAccumulator()
        assertEquals(listOf("uri-0"), assertIs<MigrationBatchProgress.Complete>(accumulator.accept(batch(0, 1))).uris)
        assertIs<MigrationBatchProgress.Awaiting>(accumulator.accept(batch(0, id = 99)))
    }

    @Test
    fun outOfOrderAndRepeatedScansAreIdempotent() {
        val accumulator = GoogleMigrationBatchAccumulator()
        assertEquals(MigrationBatchProgress.Awaiting(1, 2), accumulator.accept(batch(1)))
        assertEquals(MigrationBatchProgress.Awaiting(1, 2), accumulator.accept(batch(1)))
        assertEquals(listOf("uri-0", "uri-1"), assertIs<MigrationBatchProgress.Complete>(accumulator.accept(batch(0))).uris)
    }

    @Test
    fun rejectsConflictingDuplicatesAndMismatchedExportsWithoutLosingProgress() {
        val accumulator = GoogleMigrationBatchAccumulator()
        accumulator.accept(batch(0))
        listOf(batch(0).copy(otpAuthUris = listOf("different")), batch(1, id = 99),
            batch(1, size = 3), batch(1, version = 0), batch(2), batch(0, size = 0),
            batch(0).copy(otpAuthUris = emptyList())).forEach {
            assertIs<MigrationBatchProgress.Rejected>(accumulator.accept(it))
        }
        assertIs<MigrationBatchProgress.Complete>(accumulator.accept(batch(1)))
    }

    @Test
    fun resetAllowsNewExport() {
        val accumulator = GoogleMigrationBatchAccumulator()
        accumulator.accept(batch(0))
        accumulator.reset()
        assertEquals(MigrationBatchProgress.Awaiting(1, 2), accumulator.accept(batch(1, id = 99)))
    }
}
