package tech.arnav.twofac.lib.presentation.issuer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IssuerIconCatalogTest {
    @Test
    fun normalizeIssuerLowercasesAndTrims() {
        assertEquals("github", IssuerIconCatalog.normalizeIssuer("  GitHub  "))
        assertEquals("google account", IssuerIconCatalog.normalizeIssuer("Google Account"))
    }

    @Test
    fun normalizeIssuerReturnsNullForBlankValues() {
        assertNull(IssuerIconCatalog.normalizeIssuer(null))
        assertNull(IssuerIconCatalog.normalizeIssuer("   "))
    }

    @Test
    fun resolveIssuerIconHandlesKnownAliasesCaseInsensitively() {
        assertEquals("github", IssuerIconCatalog.resolveIssuerIcon("github").iconKey)
        assertEquals("github", IssuerIconCatalog.resolveIssuerIcon("Github").iconKey)
        assertEquals("github", IssuerIconCatalog.resolveIssuerIcon("github.com").iconKey)
        assertEquals("google", IssuerIconCatalog.resolveIssuerIcon("Google Account").iconKey)
        assertEquals("microsoft", IssuerIconCatalog.resolveIssuerIcon("outlook.com").iconKey)
        assertEquals("x_twitter", IssuerIconCatalog.resolveIssuerIcon("Twitter").iconKey)
    }

    @Test
    fun resolveIssuerIconFallsBackToPlaceholder() {
        assertEquals("placeholder", IssuerIconCatalog.resolveIssuerIcon(null).iconKey)
        assertTrue(IssuerIconCatalog.resolveIssuerIcon(null).isPlaceholder)
        assertEquals("placeholder", IssuerIconCatalog.resolveIssuerIcon("Unknown Issuer").iconKey)
        assertTrue(IssuerIconCatalog.resolveIssuerIcon("Unknown Issuer").isPlaceholder)
    }

    @Test
    fun supportedIconKeysIncludesCuratedV1Set() {
        val keys = IssuerIconCatalog.supportedIconKeys()
        assertTrue("github" in keys)
        assertTrue("google" in keys)
        assertTrue("microsoft" in keys)
        assertTrue("amazon" in keys)
        assertTrue("discord" in keys)
        assertTrue("dropbox" in keys)
        assertTrue("facebook" in keys)
        assertTrue("gitlab" in keys)
        assertTrue("linkedin" in keys)
        assertTrue("slack" in keys)
        assertTrue("twitch" in keys)
        assertTrue("x_twitter" in keys)
        assertTrue("placeholder" in keys)
    }
}
