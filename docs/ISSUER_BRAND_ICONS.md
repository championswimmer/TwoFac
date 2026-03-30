# Issuer brand icons maintenance

Issuer brand icons use one shared lookup catalog in `sharedLib`:

- `sharedLib/src/commonMain/kotlin/tech/arnav/twofac/lib/presentation/issuer/IssuerIconCatalog.kt`

The current v1 icon keys are:

- `amazon`
- `atlassian`
- `cloudflare`
- `discord`
- `dropbox`
- `facebook`
- `github`
- `gitlab`
- `google`
- `instagram`
- `linkedin`
- `meta`
- `microsoft`
- `paypal`
- `reddit`
- `shopify`
- `slack`
- `steam`
- `stripe`
- `twitch`
- `x_twitter`
- `yahoo`
- `placeholder`

## Source of truth

Brand assets come from **Font Awesome Free Brands**.

- Compose Multiplatform font: `composeApp/src/commonMain/composeResources/font/fa_brands_400_regular.ttf`
- Wear OS font copy: `watchApp/src/main/res/font/fa_brands_400_regular.ttf`
- watchOS SVG assets: `iosApp/watchApp/Assets.xcassets/*.imageset/*.svg`

The placeholder is intentionally drawn in code as a circled `?` so all platforms can share the same fallback behavior without shipping a second non-brand icon font.

Issuer matching is intentionally derived from the current `issuer` value at UI or sync time. Stored account records do not persist a matched icon key, so catalog updates can change icon selection without any secrets-store migration.

## Adding a new issuer

1. Add or extend aliases in `IssuerIconCatalog.normalizeIssuer()` / `aliasToIconKey`.
2. Add the stable `iconKey` to `supportedIconKeys`.
3. Add the matching Font Awesome glyph string in `glyphForIconKey`.
4. Ensure the brand glyph exists in `fa_brands_400_regular.ttf`.
5. Add the matching watchOS asset catalog entry at `iosApp/watchApp/Assets.xcassets/{iconKey}.imageset/`.
6. If the issuer should sync to watchOS, no extra mapping is needed because `WatchSyncAccount.issuerIconKey` is derived from the shared catalog when the sync snapshot is produced.
7. Run:
   - `./gradlew --no-daemon :sharedLib:allTests :sharedLib:updateLegacyAbi`
   - `./gradlew --no-daemon :composeApp:compileKotlinMetadata`

## Notes

- `composeApp` and `watchApp` intentionally keep separate copies of the Font Awesome Brands TTF because the modules are independent and do not share a resource pipeline.
- Unknown or blank issuers must continue to resolve to `placeholder`; do not add UI-local fallback logic.
