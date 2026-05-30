package tech.arnav.twofac.cli.theme

import tech.arnav.twofac.lib.presentation.issuer.IssuerIconCatalog

internal object CliIssuerIcons {
    private const val FALLBACK_ICON = "\uF084"

    /**
     * Nerd Fonts cheat-sheet confirmed mappings. Where a brand glyph is missing in Nerd Fonts,
     * we fall back to the closest widely-available icon instead of emitting a tofu box.
     */
    private val nerdFontGlyphs = mapOf(
        "amazon" to "\uF270",
        "atlassian" to "\uEF32",
        "cloudflare" to "\uE792",
        "discord" to "\uF1FF",
        "dropbox" to "\uF16B",
        "facebook" to "\uF09A",
        "github" to "\uF09B",
        "gitlab" to "\uF296",
        "google" to "\uF1A0",
        "instagram" to "\uF16D",
        "linkedin" to "\uF08C",
        "meta" to "\uF230",
        "microsoft" to "\uED04",
        "paypal" to "\uF1ED",
        "reddit" to "\uF1A1",
        "shopify" to "\uF290",
        "slack" to "\uF198",
        "steam" to "\uF1B6",
        "stripe" to "\uED53",
        "twitch" to "\uF1E8",
        "x_twitter" to "\uF099",
        "yahoo" to "\uF19E",
    )

    fun glyphForIssuer(rawIssuer: String?): String {
        val match = IssuerIconCatalog.resolveIssuerIcon(rawIssuer)
        return nerdFontGlyphs[match.iconKey]
            ?: IssuerIconCatalog.glyphForIconKey(match.iconKey)
            ?: FALLBACK_ICON
    }

    fun formatAccountLabel(
        accountLabel: String,
        issuer: String?,
        iconsEnabled: Boolean,
    ): String {
        if (!iconsEnabled) return accountLabel
        return "${glyphForIssuer(issuer)}  $accountLabel"
    }
}
