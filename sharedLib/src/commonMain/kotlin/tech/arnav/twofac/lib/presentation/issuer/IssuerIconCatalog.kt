package tech.arnav.twofac.lib.presentation.issuer

import tech.arnav.twofac.lib.PublicApi

@PublicApi
object IssuerIconCatalog {
    const val PLACEHOLDER_ICON_KEY = "placeholder"

    private val issuerToIcon = mapOf(
        "amazon" to "amazon",
        "amazoncom" to "amazon",
        "aws" to "amazon",
        "amazonwebservices" to "amazon",
        "signinwithamazon" to "amazon",
        "awsamazoncom" to "amazon",
        "atlassian" to "atlassian",
        "atlassiancloud" to "atlassian",
        "atlassiancom" to "atlassian",
        "jira" to "atlassian",
        "jiracloud" to "atlassian",
        "jirasoftware" to "atlassian",
        "confluence" to "atlassian",
        "bitbucket" to "atlassian",
        "trello" to "atlassian",
        "cloudflare" to "cloudflare",
        "cloudflarecom" to "cloudflare",
        "cloudflaredashboard" to "cloudflare",
        "cloudflareaccess" to "cloudflare",
        "cloudflarezerotrust" to "cloudflare",
        "discord" to "discord",
        "discordapp" to "discord",
        "discordcom" to "discord",
        "discordappcom" to "discord",
        "dropbox" to "dropbox",
        "dropboxcom" to "dropbox",
        "facebook" to "facebook",
        "facebookcom" to "facebook",
        "github" to "github",
        "githubcom" to "github",
        "githubenterprise" to "github",
        "gitlab" to "gitlab",
        "gitlabcom" to "gitlab",
        "google" to "google",
        "googleaccount" to "google",
        "googleworkspace" to "google",
        "gmail" to "google",
        "gmailcom" to "google",
        "googlemail" to "google",
        "googlemailcom" to "google",
        "accountsgooglecom" to "google",
        "googlecloud" to "google",
        "googlecloudplatform" to "google",
        "gsuite" to "google",
        "googleapps" to "google",
        "youtube" to "google",
        "youtubecom" to "google",
        "instagram" to "instagram",
        "instagramcom" to "instagram",
        "linkedin" to "linkedin",
        "linkedincom" to "linkedin",
        "meta" to "meta",
        "metacom" to "meta",
        "metaplatforms" to "meta",
        "metaquest" to "meta",
        "workplacebymeta" to "meta",
        "microsoft" to "microsoft",
        "microsoftaccount" to "microsoft",
        "livecom" to "microsoft",
        "outlook" to "microsoft",
        "outlookcom" to "microsoft",
        "office365" to "microsoft",
        "microsoft365" to "microsoft",
        "hotmail" to "microsoft",
        "hotmailcom" to "microsoft",
        "azure" to "microsoft",
        "azuread" to "microsoft",
        "azureactivedirectory" to "microsoft",
        "microsoftentra" to "microsoft",
        "entra" to "microsoft",
        "paypal" to "paypal",
        "paypalcom" to "paypal",
        "reddit" to "reddit",
        "redditcom" to "reddit",
        "oldredditcom" to "reddit",
        "shopify" to "shopify",
        "shopifycom" to "shopify",
        "slack" to "slack",
        "slackcom" to "slack",
        "slackworkspace" to "slack",
        "slacktechnologies" to "slack",
        "steam" to "steam",
        "steamcom" to "steam",
        "steampowered" to "steam",
        "steampoweredcom" to "steam",
        "valve" to "steam",
        "stripe" to "stripe",
        "stripecom" to "stripe",
        "stripedashboard" to "stripe",
        "twitch" to "twitch",
        "twitchtv" to "twitch",
        "twitter" to "x_twitter",
        "twittercom" to "x_twitter",
        "x" to "x_twitter",
        "xcom" to "x_twitter",
        "xtwitter" to "x_twitter",
        "xcorp" to "x_twitter",
        "yahoo" to "yahoo",
        "yahoocom" to "yahoo",
        "yahoomail" to "yahoo",
    )

    private val iconGlyphs = mapOf(
        "amazon" to "\uF270",
        "atlassian" to "\uF77B",
        "cloudflare" to "\uE07D",
        "discord" to "\uF392",
        "dropbox" to "\uF16B",
        "facebook" to "\uF09A",
        "github" to "\uF09B",
        "gitlab" to "\uF296",
        "google" to "\uF1A0",
        "instagram" to "\uF16D",
        "linkedin" to "\uF08C",
        "meta" to "\uE49B",
        "microsoft" to "\uF3CA",
        "paypal" to "\uF1ED",
        "reddit" to "\uF1A1",
        "shopify" to "\uE057",
        "slack" to "\uF198",
        "steam" to "\uF1B6",
        "stripe" to "\uF429",
        "twitch" to "\uF1E8",
        "x_twitter" to "\uE61B",
        "yahoo" to "\uF19E",
    )

    @PublicApi
    fun supportedIconKeys(): Set<String> = linkedSetOf<String>().apply {
        addAll(issuerToIcon.values)
        add(PLACEHOLDER_ICON_KEY)
    }

    @PublicApi
    fun normalizeIssuer(rawIssuer: String?): String? {
        return rawIssuer
            ?.trim()
            ?.takeIf(String::isNotEmpty)
            ?.lowercase()
            ?.replace("&", " and ")
            ?.replace(Regex("""[._:/()'’+-]+"""), " ")
            ?.replace(Regex("""\s+"""), " ")
            ?.trim()
            ?.takeIf(String::isNotEmpty)
    }

    @PublicApi
    fun resolveIssuerIcon(rawIssuer: String?): IssuerIconMatch {
        val normalizedIssuer = normalizeIssuer(rawIssuer)
        val iconKey = normalizedIssuer
            ?.let(::toLookupKey)
            ?.let(issuerToIcon::get)
            ?: PLACEHOLDER_ICON_KEY
        return IssuerIconMatch(
            normalizedIssuer = normalizedIssuer,
            iconKey = iconKey,
            isPlaceholder = iconKey == PLACEHOLDER_ICON_KEY,
        )
    }

    @PublicApi
    fun resolveIssuerIconKey(rawIssuer: String?): String = resolveIssuerIcon(rawIssuer).iconKey

    @PublicApi
    fun glyphForIconKey(iconKey: String): String? = iconGlyphs[iconKey]

    private fun toLookupKey(normalizedIssuer: String): String {
        return buildString(normalizedIssuer.length) {
            normalizedIssuer.forEach { ch ->
                if (ch.isLetterOrDigit()) {
                    append(ch)
                }
            }
        }
    }
}
