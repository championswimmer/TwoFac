package tech.arnav.twofac.lib.importer

import tech.arnav.twofac.lib.PublicApi

/** Indices refer to the submitted list. No secret-bearing URI is included in this result. */
@PublicApi
data class BulkImportResult(
    val importedIndices: List<Int>,
    val failedIndices: List<Int>,
)
