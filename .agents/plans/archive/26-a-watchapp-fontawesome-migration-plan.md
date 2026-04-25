---
name: watchOS Font Awesome Migration Plan
status: Completed
progress:
  - "[x] Phase 0 - Confirm the active watchOS target, bundle, and plist wiring for custom fonts"
  - "[x] Phase 1 - Expose a Swift-friendly shared glyph API for watchOS"
  - "[x] Phase 2 - Add the Font Awesome Brands font to the watch target and register it properly"
  - "[x] Phase 3 - Replace SVG image rendering with a SwiftUI font-based issuer icon view"
  - "[x] Phase 4 - Remove obsolete watchOS SVG assets and align maintenance docs"
  - "[x] Phase 5 - Validate rendering, fallback behavior, and watchOS packaging"
---

# 26-a: watchOS Font Awesome Migration Plan

## Goal

Migrate the native Apple Watch UI from **SVG asset-catalog brand icons** to a **properly bundled Font Awesome Free Brands font** so that:

1. `iosApp/watchAppExtension` renders brand icons from the same canonical font family already used on Compose targets,
2. watchOS no longer requires one SVG asset per brand,
3. the shared `iconKey` contract remains the single source of truth,
4. placeholder behavior stays consistent for unknown issuers,
5. the watch target is configured correctly for custom font loading and future issuer additions.

This is a follow-up to `.agents/plans/26-issuer-brand-icons-rollout-plan.md`, specifically replacing the watchOS-specific SVG strategy with a font-backed SwiftUI renderer.

## Current repository state

### Current watchOS rendering

- `iosApp/watchAppExtension/IssuerBrandIconView.swift`
  - currently renders:
    - a placeholder circle with `?` in SwiftUI, or
    - `Image(iconKey)` from the asset catalog for known brands
- `iosApp/watchAppExtension/WatchExtensionContentView.swift`
  - already passes `account.issuerIconKey` into `IssuerBrandIconView`

### Current watchOS assets

- `iosApp/watchApp/Assets.xcassets/*.imageset/*.svg`
  - currently contains one SVG image set per known issuer:
    - `amazon`
    - `discord`
    - `dropbox`
    - `facebook`
    - `github`
    - `gitlab`
    - `google`
    - `linkedin`
    - `microsoft`
    - `slack`
    - `twitch`
    - `x_twitter`

### Current shared issuer contract

- `sharedLib/src/commonMain/kotlin/tech/arnav/twofac/lib/presentation/issuer/IssuerIconCatalog.kt`
  - already exposes:
    - stable `iconKey`
    - normalized issuer lookup
    - glyph lookup via `glyphForIconKey(iconKey: String): String?`
- `sharedLib/src/commonMain/kotlin/tech/arnav/twofac/lib/watchsync/WatchSyncPayload.kt`
  - `WatchSyncAccount` already carries `issuerIconKey`

### Current Xcode/project wiring

- `iosApp/iosApp.xcodeproj/project.pbxproj`
  - uses file-system-synchronized groups for:
    - `watchApp/`
    - `watchAppExtension/`
- `iosApp/watchApp/Info.plist`
- `iosApp/watchAppExtension/Info.plist`

Because the project is using file-system-synchronized groups, **the first step must confirm which watch target plist is actually active for custom font registration** before changing `UIAppFonts`.

## Why migrate away from SVGs

### Benefits

1. The watchOS implementation will use the same Font Awesome source family as Compose and Wear OS instead of a separate exported-asset pipeline.
2. Adding a new issuer will stop requiring a new watchOS `.imageset` for each icon.
3. It removes drift risk between:
   - Compose font glyphs,
   - shared `iconKey` values,
   - watchOS asset names.

### Trade-offs

1. watchOS now needs correct custom-font packaging and plist registration.
2. SwiftUI must render glyphs using the font’s **PostScript name**, not just the file name.
3. We need explicit fallback behavior for:
   - placeholder icon,
   - missing glyphs,
   - failed font registration.

## Recommended architecture decision

Keep **issuer matching and icon-key resolution** in `sharedLib`, but change watchOS rendering from:

- `iconKey -> asset catalog image`

to:

- `iconKey -> shared glyph string -> SwiftUI Text(Font.custom(...))`

### Important rule

Do **not** duplicate issuer normalization or alias logic in Swift.

SwiftUI should only be responsible for:

1. receiving a stable `iconKey`,
2. obtaining the corresponding glyph string,
3. rendering it with the bundled Font Awesome font,
4. falling back to the existing placeholder if the key is unknown or the glyph is unavailable.

## Migration phases

## Phase 0 - Confirm the active watchOS target, bundle, and plist wiring for custom fonts

### Goals

- identify the exact watch target that owns the runtime bundle,
- verify which `Info.plist` must receive `UIAppFonts`,
- avoid landing a font file in the wrong bundle.

### Steps

1. Open the watch target in Xcode and confirm:
   - the active watch app target name,
   - the bundle that contains `IssuerBrandIconView.swift`,
   - the bundle that owns the currently loaded `Assets.xcassets`.
2. Verify which plist is bound in the watch target build settings:
   - `iosApp/watchApp/Info.plist`
   - or `iosApp/watchAppExtension/Info.plist`
3. Document whether custom fonts need to be registered in:
   - the app target plist,
   - the extension plist,
   - or both.
4. Confirm whether the font file must have target membership on:
   - the watch app bundle only,
   - or the SwiftUI/extension source bundle as well.

### Expected output

- one verified target/plist decision for font registration,
- no ambiguity about where the `.ttf` belongs.

## Phase 1 - Expose a Swift-friendly shared glyph API for watchOS

### Goals

- make glyph lookup reusable from Swift,
- avoid hardcoding Font Awesome codepoints in SwiftUI files,
- keep watchOS aligned with `sharedLib`.

### Steps

1. Review the current `IssuerIconCatalog.glyphForIconKey(iconKey: String): String?` API from Swift via `TwoFacKit`.
2. If Swift interop is awkward, add a tiny shared wrapper with a Swift-friendly shape, for example:
   - `IssuerIconCatalog.sharedGlyphForIconKey(iconKey: String): String`
   - or a dedicated `@PublicApi` helper object returning non-null fallback values.
3. Ensure the shared API exposes:
   - glyph string,
   - placeholder fallback behavior,
   - optionally the watch font family name constant if helpful.
4. Avoid exposing raw integer codepoints to Swift if a ready-to-render string is already available.

### Files likely touched

- `sharedLib/src/commonMain/kotlin/tech/arnav/twofac/lib/presentation/issuer/IssuerIconCatalog.kt`
- possibly a new `sharedLib/.../IssuerIconFontBridge.kt`
- `sharedLib/api/sharedLib.api`
- `sharedLib/api/sharedLib.klib.api`

### Expected output

- Swift can resolve `iconKey -> glyph string` using the shared library, not a duplicated local map.

## Phase 2 - Add the Font Awesome Brands font to the watch target and register it properly

### Goals

- bundle the font correctly in the watch target,
- guarantee runtime registration through the active plist,
- avoid “works in Preview but not on device” failures.

### Steps

1. Decide whether to ship:
   - the same full `fa_brands_400_regular.ttf`, or
   - a subsetted watch-specific font containing only the curated v1 glyphs.
2. Add the selected font file into the watch project tree under a stable location, for example:
   - `iosApp/watchApp/Fonts/fa_brands_400_regular.ttf`
3. Ensure the font file has correct target membership for the verified watch target from Phase 0.
4. Add `UIAppFonts` to the active watch target `Info.plist` and register the exact font filename.
5. Confirm the font’s **PostScript name** (not just the filename) for SwiftUI usage, for example:
   - `Font Awesome 6 Brands-Regular`
6. Add a small debug-only validation helper if needed to print available font families on watchOS during development.

### Files likely touched

- `iosApp/watchApp/Info.plist` and/or `iosApp/watchAppExtension/Info.plist`
- new font file under `iosApp/watchApp/` or another watch-target resource directory
- `iosApp/iosApp.xcodeproj/project.pbxproj` only if file-system sync is insufficient

### Expected output

- the Font Awesome Brands font is packaged in the correct watch bundle and registered at runtime.

## Phase 3 - Replace SVG image rendering with a SwiftUI font-based issuer icon view

### Goals

- render issuer icons using `Text` + custom font,
- preserve current `iconKey` input contract,
- keep placeholder behavior and tinting clean.

### Steps

1. Rewrite `iosApp/watchAppExtension/IssuerBrandIconView.swift` so known brands render using:
   - `Text(glyph)`
   - `.font(.custom(postScriptName, size: size))`
   - `.foregroundStyle(tint)`
2. Keep the existing placeholder circle fallback for:
   - `placeholder` iconKey,
   - missing shared glyph result,
   - failed font availability if a defensive check is added.
3. Ensure sizing remains visually consistent with the current `18`-point icon slot in `WatchExtensionContentView.swift`.
4. Verify baseline alignment and scaling for:
   - small watch screens,
   - Dynamic Type / accessibility size changes if applicable.
5. Ensure the icon remains decorative by default unless a specific accessibility label is provided.

### Files likely touched

- `iosApp/watchAppExtension/IssuerBrandIconView.swift`
- possibly a new `iosApp/watchAppExtension/WatchFontRegistry.swift` helper if runtime checks are useful

### Expected output

- watchOS renders Font Awesome brand glyphs directly from a bundled font, no asset lookup required.

## Phase 4 - Remove obsolete watchOS SVG assets and align maintenance docs

### Goals

- clean up the old asset pipeline,
- prevent future contributors from updating both fonts and SVGs,
- keep maintenance instructions accurate.

### Steps

1. Delete the now-obsolete SVG image sets from:
   - `iosApp/watchApp/Assets.xcassets/*.imageset`
2. Leave unrelated app icons untouched.
3. Update:
   - `docs/ISSUER_BRAND_ICONS.md`
   - any relevant `AGENTS.md` or plan docs
4. Document the new watchOS workflow:
   - add issuer alias in sharedLib,
   - add glyph in sharedLib,
   - ensure glyph exists in watch font,
   - no asset catalog entry required for each brand.

### Expected output

- one watchOS icon pipeline instead of font + per-brand SVG duplication.

## Phase 5 - Validate rendering, fallback behavior, and watchOS packaging

### Goals

- confirm the custom font works on real watch bundles,
- make sure placeholder and unknown issuers still behave correctly,
- catch packaging regressions early.

### Steps

1. Build the Apple targets that produce `TwoFacKit` and the watch app.
2. Run the watch app in a simulator and verify:
   - GitHub renders correctly,
   - Google renders correctly,
   - X/Twitter renders correctly,
   - unknown issuer still shows the placeholder.
3. Confirm that no runtime fallback to system font occurs for known icons.
4. Confirm that `issuerIconKey` values from synced payloads still drive the correct watch rendering.
5. If feasible, add a lightweight regression test or debug assertion around glyph lookup and placeholder fallback.

### Expected output

- watchOS Font Awesome rendering works reliably in the packaged watch target.

## Exact files likely touched

- `sharedLib/src/commonMain/kotlin/tech/arnav/twofac/lib/presentation/issuer/IssuerIconCatalog.kt`
- `sharedLib/api/sharedLib.api`
- `sharedLib/api/sharedLib.klib.api`
- `iosApp/watchApp/Info.plist`
- `iosApp/watchAppExtension/Info.plist` (only if confirmed active for font registration)
- `iosApp/watchAppExtension/IssuerBrandIconView.swift`
- possibly new font resource under `iosApp/watchApp/`
- possibly `iosApp/iosApp.xcodeproj/project.pbxproj`
- `docs/ISSUER_BRAND_ICONS.md`
- deletion of `iosApp/watchApp/Assets.xcassets/{brand}.imageset/*`

## Risks to manage

1. **Wrong plist registration**
   - The biggest risk is registering `UIAppFonts` in the wrong plist for the actual watch runtime target.
2. **Wrong font name**
   - `Font.custom(...)` requires the PostScript font name, not necessarily the filename.
3. **Missing target membership**
   - The font file may exist in the repo but not be packaged into the actual watch bundle.
4. **Glyph mismatch**
   - If Swift uses a local codepoint table instead of the shared helper, drift returns immediately.
5. **Premature SVG cleanup**
   - Do not delete the existing SVG image sets until the font-based renderer is verified on-device or simulator.

## Suggested execution slices

1. **PR 1**
   - confirm target/plist wiring
   - add Swift-friendly shared glyph bridge if needed
2. **PR 2**
   - add watch font asset
   - register `UIAppFonts`
   - implement font-based `IssuerBrandIconView`
3. **PR 3**
   - remove SVG asset sets
   - update docs
   - finish validation

## Success criteria

This migration is done when:

1. native watchOS issuer icons render using a bundled Font Awesome font,
2. watchOS no longer depends on per-brand SVG image sets,
3. `issuerIconKey` remains the only watch-side input for icon selection,
4. unknown issuers still render the placeholder cleanly,
5. future issuer additions do not require new watchOS asset-catalog brand files.
