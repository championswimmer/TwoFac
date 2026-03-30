package tech.arnav.twofac.lib.presentation.issuer

import tech.arnav.twofac.lib.PublicApi

@PublicApi
object IssuerIconCatalog {
    const val PLACEHOLDER_ICON_KEY = "placeholder"

    private const val AMAZON_ICON_KEY = "amazon"
    private const val DISCORD_ICON_KEY = "discord"
    private const val DROPBOX_ICON_KEY = "dropbox"
    private const val FACEBOOK_ICON_KEY = "facebook"
    private const val GITHUB_ICON_KEY = "github"
    private const val GITLAB_ICON_KEY = "gitlab"
    private const val GOOGLE_ICON_KEY = "google"
    private const val LINKEDIN_ICON_KEY = "linkedin"
    private const val MICROSOFT_ICON_KEY = "microsoft"
    private const val SLACK_ICON_KEY = "slack"
    private const val TWITCH_ICON_KEY = "twitch"
    private const val X_TWITTER_ICON_KEY = "x_twitter"

    private val aliasToIconKey = mapOf(
        "amazon" to AMAZON_ICON_KEY,
        "amazoncom" to AMAZON_ICON_KEY,
        "aws" to AMAZON_ICON_KEY,
        "amazonwebservices" to AMAZON_ICON_KEY,
        "discord" to DISCORD_ICON_KEY,
        "discordapp" to DISCORD_ICON_KEY,
        "dropbox" to DROPBOX_ICON_KEY,
        "dropboxcom" to DROPBOX_ICON_KEY,
        "facebook" to FACEBOOK_ICON_KEY,
        "facebookcom" to FACEBOOK_ICON_KEY,
        "github" to GITHUB_ICON_KEY,
        "githubcom" to GITHUB_ICON_KEY,
        "gitlab" to GITLAB_ICON_KEY,
        "gitlabcom" to GITLAB_ICON_KEY,
        "google" to GOOGLE_ICON_KEY,
        "googleaccount" to GOOGLE_ICON_KEY,
        "googleworkspace" to GOOGLE_ICON_KEY,
        "gmail" to GOOGLE_ICON_KEY,
        "linkedin" to LINKEDIN_ICON_KEY,
        "linkedincom" to LINKEDIN_ICON_KEY,
        "microsoft" to MICROSOFT_ICON_KEY,
        "microsoftaccount" to MICROSOFT_ICON_KEY,
        "livecom" to MICROSOFT_ICON_KEY,
        "outlook" to MICROSOFT_ICON_KEY,
        "outlookcom" to MICROSOFT_ICON_KEY,
        "office365" to MICROSOFT_ICON_KEY,
        "slack" to SLACK_ICON_KEY,
        "slackcom" to SLACK_ICON_KEY,
        "twitch" to TWITCH_ICON_KEY,
        "twitchtv" to TWITCH_ICON_KEY,
        "twitter" to X_TWITTER_ICON_KEY,
        "twittercom" to X_TWITTER_ICON_KEY,
        "x" to X_TWITTER_ICON_KEY,
        "xcom" to X_TWITTER_ICON_KEY,
        "xtwitter" to X_TWITTER_ICON_KEY,
    )

    private val supportedIconKeys = linkedSetOf(
        AMAZON_ICON_KEY,
        DISCORD_ICON_KEY,
        DROPBOX_ICON_KEY,
        FACEBOOK_ICON_KEY,
        GITHUB_ICON_KEY,
        GITLAB_ICON_KEY,
        GOOGLE_ICON_KEY,
        LINKEDIN_ICON_KEY,
        MICROSOFT_ICON_KEY,
        SLACK_ICON_KEY,
        TWITCH_ICON_KEY,
        X_TWITTER_ICON_KEY,
        PLACEHOLDER_ICON_KEY,
    )

    private val iconGlyphs = mapOf(
        AMAZON_ICON_KEY to "\uF270",
        DISCORD_ICON_KEY to "\uF392",
        DROPBOX_ICON_KEY to "\uF16B",
        FACEBOOK_ICON_KEY to "\uF09A",
        GITHUB_ICON_KEY to "\uF09B",
        GITLAB_ICON_KEY to "\uF296",
        GOOGLE_ICON_KEY to "\uF1A0",
        LINKEDIN_ICON_KEY to "\uF08C",
        MICROSOFT_ICON_KEY to "\uF3CA",
        SLACK_ICON_KEY to "\uF198",
        TWITCH_ICON_KEY to "\uF1E8",
        X_TWITTER_ICON_KEY to "\uE61B",
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
