package tech.arnav.twofac.lib.presentation.issuer

import tech.arnav.twofac.lib.PublicApi

@PublicApi
object IssuerIconCatalog {
    const val PLACEHOLDER_ICON_KEY = "placeholder"

    private const val AMAZON_ICON_KEY = "amazon"
    private const val ATLASSIAN_ICON_KEY = "atlassian"
    private const val CLOUDFLARE_ICON_KEY = "cloudflare"
    private const val DISCORD_ICON_KEY = "discord"
    private const val DROPBOX_ICON_KEY = "dropbox"
    private const val FACEBOOK_ICON_KEY = "facebook"
    private const val GITHUB_ICON_KEY = "github"
    private const val GITLAB_ICON_KEY = "gitlab"
    private const val GOOGLE_ICON_KEY = "google"
    private const val INSTAGRAM_ICON_KEY = "instagram"
    private const val LINKEDIN_ICON_KEY = "linkedin"
    private const val META_ICON_KEY = "meta"
    private const val MICROSOFT_ICON_KEY = "microsoft"
    private const val PAYPAL_ICON_KEY = "paypal"
    private const val REDDIT_ICON_KEY = "reddit"
    private const val SHOPIFY_ICON_KEY = "shopify"
    private const val SLACK_ICON_KEY = "slack"
    private const val STEAM_ICON_KEY = "steam"
    private const val STRIPE_ICON_KEY = "stripe"
    private const val TWITCH_ICON_KEY = "twitch"
    private const val X_TWITTER_ICON_KEY = "x_twitter"
    private const val YAHOO_ICON_KEY = "yahoo"

    private val aliasToIconKey = mapOf(
        "amazon" to AMAZON_ICON_KEY,
        "amazoncom" to AMAZON_ICON_KEY,
        "aws" to AMAZON_ICON_KEY,
        "amazonwebservices" to AMAZON_ICON_KEY,
        "signinwithamazon" to AMAZON_ICON_KEY,
        "awsamazoncom" to AMAZON_ICON_KEY,
        "atlassian" to ATLASSIAN_ICON_KEY,
        "atlassiancloud" to ATLASSIAN_ICON_KEY,
        "atlassiancom" to ATLASSIAN_ICON_KEY,
        "jira" to ATLASSIAN_ICON_KEY,
        "jiracloud" to ATLASSIAN_ICON_KEY,
        "jirasoftware" to ATLASSIAN_ICON_KEY,
        "confluence" to ATLASSIAN_ICON_KEY,
        "bitbucket" to ATLASSIAN_ICON_KEY,
        "trello" to ATLASSIAN_ICON_KEY,
        "cloudflare" to CLOUDFLARE_ICON_KEY,
        "cloudflarecom" to CLOUDFLARE_ICON_KEY,
        "cloudflaredashboard" to CLOUDFLARE_ICON_KEY,
        "cloudflareaccess" to CLOUDFLARE_ICON_KEY,
        "cloudflarezerotrust" to CLOUDFLARE_ICON_KEY,
        "discord" to DISCORD_ICON_KEY,
        "discordapp" to DISCORD_ICON_KEY,
        "discordcom" to DISCORD_ICON_KEY,
        "discordappcom" to DISCORD_ICON_KEY,
        "dropbox" to DROPBOX_ICON_KEY,
        "dropboxcom" to DROPBOX_ICON_KEY,
        "facebook" to FACEBOOK_ICON_KEY,
        "facebookcom" to FACEBOOK_ICON_KEY,
        "github" to GITHUB_ICON_KEY,
        "githubcom" to GITHUB_ICON_KEY,
        "githubenterprise" to GITHUB_ICON_KEY,
        "gitlab" to GITLAB_ICON_KEY,
        "gitlabcom" to GITLAB_ICON_KEY,
        "google" to GOOGLE_ICON_KEY,
        "googleaccount" to GOOGLE_ICON_KEY,
        "googleworkspace" to GOOGLE_ICON_KEY,
        "gmail" to GOOGLE_ICON_KEY,
        "gmailcom" to GOOGLE_ICON_KEY,
        "googlemail" to GOOGLE_ICON_KEY,
        "googlemailcom" to GOOGLE_ICON_KEY,
        "accountsgooglecom" to GOOGLE_ICON_KEY,
        "googlecloud" to GOOGLE_ICON_KEY,
        "googlecloudplatform" to GOOGLE_ICON_KEY,
        "gsuite" to GOOGLE_ICON_KEY,
        "googleapps" to GOOGLE_ICON_KEY,
        "youtube" to GOOGLE_ICON_KEY,
        "youtubecom" to GOOGLE_ICON_KEY,
        "instagram" to INSTAGRAM_ICON_KEY,
        "instagramcom" to INSTAGRAM_ICON_KEY,
        "linkedin" to LINKEDIN_ICON_KEY,
        "linkedincom" to LINKEDIN_ICON_KEY,
        "meta" to META_ICON_KEY,
        "metacom" to META_ICON_KEY,
        "metaplatforms" to META_ICON_KEY,
        "metaquest" to META_ICON_KEY,
        "workplacebymeta" to META_ICON_KEY,
        "microsoft" to MICROSOFT_ICON_KEY,
        "microsoftaccount" to MICROSOFT_ICON_KEY,
        "livecom" to MICROSOFT_ICON_KEY,
        "outlook" to MICROSOFT_ICON_KEY,
        "outlookcom" to MICROSOFT_ICON_KEY,
        "office365" to MICROSOFT_ICON_KEY,
        "microsoft365" to MICROSOFT_ICON_KEY,
        "hotmail" to MICROSOFT_ICON_KEY,
        "hotmailcom" to MICROSOFT_ICON_KEY,
        "azure" to MICROSOFT_ICON_KEY,
        "azuread" to MICROSOFT_ICON_KEY,
        "azureactivedirectory" to MICROSOFT_ICON_KEY,
        "microsoftentra" to MICROSOFT_ICON_KEY,
        "entra" to MICROSOFT_ICON_KEY,
        "paypal" to PAYPAL_ICON_KEY,
        "paypalcom" to PAYPAL_ICON_KEY,
        "reddit" to REDDIT_ICON_KEY,
        "redditcom" to REDDIT_ICON_KEY,
        "oldredditcom" to REDDIT_ICON_KEY,
        "shopify" to SHOPIFY_ICON_KEY,
        "shopifycom" to SHOPIFY_ICON_KEY,
        "slack" to SLACK_ICON_KEY,
        "slackcom" to SLACK_ICON_KEY,
        "slackworkspace" to SLACK_ICON_KEY,
        "slacktechnologies" to SLACK_ICON_KEY,
        "steam" to STEAM_ICON_KEY,
        "steamcom" to STEAM_ICON_KEY,
        "steampowered" to STEAM_ICON_KEY,
        "steampoweredcom" to STEAM_ICON_KEY,
        "valve" to STEAM_ICON_KEY,
        "stripe" to STRIPE_ICON_KEY,
        "stripecom" to STRIPE_ICON_KEY,
        "stripedashboard" to STRIPE_ICON_KEY,
        "twitch" to TWITCH_ICON_KEY,
        "twitchtv" to TWITCH_ICON_KEY,
        "twitter" to X_TWITTER_ICON_KEY,
        "twittercom" to X_TWITTER_ICON_KEY,
        "x" to X_TWITTER_ICON_KEY,
        "xcom" to X_TWITTER_ICON_KEY,
        "xtwitter" to X_TWITTER_ICON_KEY,
        "xcorp" to X_TWITTER_ICON_KEY,
        "yahoo" to YAHOO_ICON_KEY,
        "yahoocom" to YAHOO_ICON_KEY,
        "yahoomail" to YAHOO_ICON_KEY,
    )

    private val supportedIconKeys = linkedSetOf(
        AMAZON_ICON_KEY,
        ATLASSIAN_ICON_KEY,
        CLOUDFLARE_ICON_KEY,
        DISCORD_ICON_KEY,
        DROPBOX_ICON_KEY,
        FACEBOOK_ICON_KEY,
        GITHUB_ICON_KEY,
        GITLAB_ICON_KEY,
        GOOGLE_ICON_KEY,
        INSTAGRAM_ICON_KEY,
        LINKEDIN_ICON_KEY,
        META_ICON_KEY,
        MICROSOFT_ICON_KEY,
        PAYPAL_ICON_KEY,
        REDDIT_ICON_KEY,
        SHOPIFY_ICON_KEY,
        SLACK_ICON_KEY,
        STEAM_ICON_KEY,
        STRIPE_ICON_KEY,
        TWITCH_ICON_KEY,
        X_TWITTER_ICON_KEY,
        YAHOO_ICON_KEY,
        PLACEHOLDER_ICON_KEY,
    )

    private val iconGlyphs = mapOf(
        AMAZON_ICON_KEY to "\uF270",
        ATLASSIAN_ICON_KEY to "\uF77B",
        CLOUDFLARE_ICON_KEY to "\uE07D",
        DISCORD_ICON_KEY to "\uF392",
        DROPBOX_ICON_KEY to "\uF16B",
        FACEBOOK_ICON_KEY to "\uF09A",
        GITHUB_ICON_KEY to "\uF09B",
        GITLAB_ICON_KEY to "\uF296",
        GOOGLE_ICON_KEY to "\uF1A0",
        INSTAGRAM_ICON_KEY to "\uF16D",
        LINKEDIN_ICON_KEY to "\uF08C",
        META_ICON_KEY to "\uE49B",
        MICROSOFT_ICON_KEY to "\uF3CA",
        PAYPAL_ICON_KEY to "\uF1ED",
        REDDIT_ICON_KEY to "\uF1A1",
        SHOPIFY_ICON_KEY to "\uE057",
        SLACK_ICON_KEY to "\uF198",
        STEAM_ICON_KEY to "\uF1B6",
        STRIPE_ICON_KEY to "\uF429",
        TWITCH_ICON_KEY to "\uF1E8",
        X_TWITTER_ICON_KEY to "\uE61B",
        YAHOO_ICON_KEY to "\uF19E",
    )

    @PublicApi
    fun supportedIconKeys(): Set<String> = supportedIconKeys

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
            ?.let(aliasToIconKey::get)
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
