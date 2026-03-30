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
        assertEquals("google", IssuerIconCatalog.resolveIssuerIcon("gmail.com").iconKey)
        assertEquals("google", IssuerIconCatalog.resolveIssuerIcon("accounts.google.com").iconKey)
        assertEquals("google", IssuerIconCatalog.resolveIssuerIcon("Google Cloud").iconKey)
        assertEquals("instagram", IssuerIconCatalog.resolveIssuerIcon("Instagram").iconKey)
        assertEquals("meta", IssuerIconCatalog.resolveIssuerIcon("Meta").iconKey)
        assertEquals("meta", IssuerIconCatalog.resolveIssuerIcon("meta.com").iconKey)
        assertEquals("cloudflare", IssuerIconCatalog.resolveIssuerIcon("cloudflare.com").iconKey)
        assertEquals("cloudflare", IssuerIconCatalog.resolveIssuerIcon("Cloudflare Zero Trust").iconKey)
        assertEquals("slack", IssuerIconCatalog.resolveIssuerIcon("Slack Workspace").iconKey)
        assertEquals("atlassian", IssuerIconCatalog.resolveIssuerIcon("Jira").iconKey)
        assertEquals("atlassian", IssuerIconCatalog.resolveIssuerIcon("Bitbucket").iconKey)
        assertEquals("paypal", IssuerIconCatalog.resolveIssuerIcon("paypal.com").iconKey)
        assertEquals("stripe", IssuerIconCatalog.resolveIssuerIcon("Stripe.com").iconKey)
        assertEquals("stripe", IssuerIconCatalog.resolveIssuerIcon("Stripe Dashboard").iconKey)
        assertEquals("reddit", IssuerIconCatalog.resolveIssuerIcon("reddit.com").iconKey)
        assertEquals("shopify", IssuerIconCatalog.resolveIssuerIcon("Shopify").iconKey)
        assertEquals("steam", IssuerIconCatalog.resolveIssuerIcon("SteamPowered").iconKey)
        assertEquals("steam", IssuerIconCatalog.resolveIssuerIcon("Valve").iconKey)
        assertEquals("yahoo", IssuerIconCatalog.resolveIssuerIcon("Yahoo Mail").iconKey)
        assertEquals("microsoft", IssuerIconCatalog.resolveIssuerIcon("outlook.com").iconKey)
        assertEquals("microsoft", IssuerIconCatalog.resolveIssuerIcon("hotmail.com").iconKey)
        assertEquals("microsoft", IssuerIconCatalog.resolveIssuerIcon("Microsoft Entra").iconKey)
        assertEquals("x_twitter", IssuerIconCatalog.resolveIssuerIcon("Twitter").iconKey)
        assertEquals("x_twitter", IssuerIconCatalog.resolveIssuerIcon("x.com").iconKey)
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
        assertTrue("instagram" in keys)
        assertTrue("meta" in keys)
        assertTrue("cloudflare" in keys)
        assertTrue("atlassian" in keys)
        assertTrue("paypal" in keys)
        assertTrue("reddit" in keys)
        assertTrue("shopify" in keys)
        assertTrue("slack" in keys)
        assertTrue("steam" in keys)
        assertTrue("stripe" in keys)
        assertTrue("twitch" in keys)
        assertTrue("x_twitter" in keys)
        assertTrue("yahoo" in keys)
        assertTrue("placeholder" in keys)
    }
}
