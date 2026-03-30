package tech.arnav.twofac.lib.presentation.issuer

import tech.arnav.twofac.lib.PublicApi

@PublicApi
data class IssuerIconMatch(
    val normalizedIssuer: String?,
    val iconKey: String,
    val isPlaceholder: Boolean,
)
