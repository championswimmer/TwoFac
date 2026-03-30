package tech.arnav.twofac.lib.presentation.issuer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IssuerIconCatalogTest {
    @Test
    fun normalizeIssuerCanonicalizesCaseAndPunctuation() {
        assertEquals("github", IssuerIconCatalog.normalizeIssuer("  GitHub  "))
        assertEquals("accounts google com", IssuerIconCatalog.normalizeIssuer("accounts.google.com"))
        assertEquals("x com", IssuerIconCatalog.normalizeIssuer("x.com"))
    }

    @Test
    fun normalizeIssuerReturnsNullForBlankValues() {
        assertNull(IssuerIconCatalog.normalizeIssuer(null))
        assertNull(IssuerIconCatalog.normalizeIssuer("   "))
    }

    @Test
    fun resolveIssuerIconMatchesRepresentativeRealWorldAliases() {
        val representativeAliases = mapOf(
            " Github " to "github",
            "accounts.google.com" to "google",
            "Slack Workspace" to "slack",
            "Cloudflare Zero Trust" to "cloudflare",
            "Microsoft Entra" to "microsoft",
            "Stripe Dashboard" to "stripe",
            "Valve" to "steam",
            "x.com" to "x_twitter",
        )

        representativeAliases.forEach { (issuer, expectedIconKey) ->
            assertEquals(expectedIconKey, IssuerIconCatalog.resolveIssuerIcon(issuer).iconKey, issuer)
        }
    }

    @Test
    fun resolveIssuerIconFallsBackToPlaceholderForBlankAndUnknownIssuers() {
        val blankMatch = IssuerIconCatalog.resolveIssuerIcon("   ")
        assertNull(blankMatch.normalizedIssuer)
        assertEquals("placeholder", blankMatch.iconKey)
        assertTrue(blankMatch.isPlaceholder)

        val unknownMatch = IssuerIconCatalog.resolveIssuerIcon("Unknown Issuer")
        assertEquals("unknown issuer", unknownMatch.normalizedIssuer)
        assertEquals("placeholder", unknownMatch.iconKey)
        assertTrue(unknownMatch.isPlaceholder)
    }

    @Test
    fun supportedIconKeysAllHaveFontAwesomeGlyphs() {
        IssuerIconCatalog.supportedIconKeys()
            .filterNot { it == IssuerIconCatalog.PLACEHOLDER_ICON_KEY }
            .forEach { iconKey ->
                assertNotNull(IssuerIconCatalog.glyphForIconKey(iconKey), iconKey)
            }

        assertNull(IssuerIconCatalog.glyphForIconKey("not-a-real-icon"))
    }
}
