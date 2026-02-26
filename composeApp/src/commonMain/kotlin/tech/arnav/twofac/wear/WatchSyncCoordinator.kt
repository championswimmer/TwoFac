package tech.arnav.twofac.wear

import tech.arnav.twofac.companion.CompanionSyncCoordinator
import tech.arnav.twofac.companion.CompanionSyncSourceAccount
import tech.arnav.twofac.companion.buildCompanionSyncSnapshot
import tech.arnav.twofac.companion.isSyncToCompanionEnabled

@Deprecated(
    message = "Use CompanionSyncCoordinator from tech.arnav.twofac.companion",
    replaceWith = ReplaceWith("CompanionSyncCoordinator"),
)
typealias WatchSyncCoordinator = CompanionSyncCoordinator

@Deprecated(
    message = "Use CompanionSyncSourceAccount from tech.arnav.twofac.companion",
    replaceWith = ReplaceWith("CompanionSyncSourceAccount"),
)
typealias WatchSyncSourceAccount = CompanionSyncSourceAccount

@Deprecated(
    message = "Use buildCompanionSyncSnapshot from tech.arnav.twofac.companion",
    replaceWith = ReplaceWith("buildCompanionSyncSnapshot(sourceAccounts, generatedAtEpochSec)"),
)
fun buildWatchSyncSnapshot(
    sourceAccounts: List<CompanionSyncSourceAccount>,
    generatedAtEpochSec: Long,
) = buildCompanionSyncSnapshot(
    sourceAccounts = sourceAccounts,
    generatedAtEpochSec = generatedAtEpochSec,
)

@Deprecated(
    message = "Use isSyncToCompanionEnabled from tech.arnav.twofac.companion",
    replaceWith = ReplaceWith("isSyncToCompanionEnabled(isCompanionActive, isSyncInProgress)"),
)
fun isSyncToWatchEnabled(
    isCompanionActive: Boolean,
    isSyncInProgress: Boolean,
): Boolean = isSyncToCompanionEnabled(
    isCompanionActive = isCompanionActive,
    isSyncInProgress = isSyncInProgress,
)
