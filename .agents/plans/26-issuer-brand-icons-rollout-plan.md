---
name: Issuer Brand Icons Rollout Plan
status: Completed
progress:
  - "[x] Phase 0 - Audit issuer-bearing UI surfaces and lock the cross-platform icon strategy"
  - "[x] Phase 1 - Add shared issuer normalization and icon-key resolution in sharedLib"
  - "[x] Phase 2 - Prepare Font Awesome Free Brands assets plus a default placeholder icon"
  - "[x] Phase 3 - Add shared Compose issuer icon rendering for Android, iOS, Desktop, Web, and Wear OS"
  - "[x] Phase 4 - Roll issuer icons through account-list and OTP surfaces"
  - "[x] Phase 5 - Add native watchOS issuer icon rendering using Apple asset catalogs"
  - "[x] Phase 6 - Add tests, QA coverage, and icon-catalog maintenance docs"
---

# Issuer Brand Icons Rollout Plan

## Goal

Add issuer icons for OTP accounts so that:

1. known issuers show a recognizable brand icon,
2. issuer matching is case-insensitive by lowercasing before comparison,
3. unknown issuers show one consistent placeholder icon,
4. the feature works across every supported UI surface:
   - `composeApp` on Android, iOS, Desktop, and Web/Wasm,
   - `watchApp` on Wear OS,
   - `iosApp/watchAppExtension` on native watchOS.

The canonical icon source should be **Font Awesome Free Brands**. The implementation should share one issuer-matching catalog across platforms, while allowing different rendering strategies where the platform stack differs.

## Current repository state

### Compose app surfaces

- `composeApp/src/commonMain/kotlin/tech/arnav/twofac/components/accounts/AccountListItem.kt`
  - currently renders only `accountLabel`
- `composeApp/src/commonMain/kotlin/tech/arnav/twofac/components/accounts/AccountsListContent.kt`
  - passes only `accountLabel` into the row
- `composeApp/src/commonMain/kotlin/tech/arnav/twofac/components/otp/OTPCard.kt`
  - renders the OTP card header from `account.accountLabel`
- `sharedLib/src/commonMain/kotlin/tech/arnav/twofac/lib/storage/StoredAccount.kt`
  - `StoredAccount.DisplayAccount` currently contains only:
    - `accountID`
    - `accountLabel`
    - `nextCodeAt`
  - there is **no issuer field** in the common display model yet

### Wear OS surface

- `watchApp/src/main/java/tech/arnav/twofac/watch/ui/OtpAccountScreen.kt`
  - already has access to `entry.issuer`
  - currently renders issuer/account text only

### Native watchOS surface

- `iosApp/watchAppExtension/WatchExtensionContentView.swift`
  - already has access to `account.issuer`
  - currently renders issuer/account text only

### Shared/watch sync data flow

- `composeApp/src/commonMain/kotlin/tech/arnav/twofac/companion/CompanionSyncCoordinator.kt`
  - already parses issuer from `otpAuthUri` when building `WatchSyncAccount`
- `sharedLib/src/commonMain/kotlin/tech/arnav/twofac/lib/watchsync/WatchSyncPayload.kt`
  - `WatchSyncAccount` already contains `issuer`

## Research summary

### Compose Multiplatform findings

1. Compose Multiplatform supports bundled custom fonts through shared resources in `composeResources`, and generated `Res` accessors can be used on Android, iOS, Desktop, and Web/Wasm.
2. Web/Wasm supports bundled fonts too, but web targets may need resource preloading to avoid first-frame missing glyphs.
3. Using a Font Awesome Brands font as the glyph source is practical for Compose, but we should keep the shipped asset set small to avoid unnecessary bundle size on web.
4. A small in-repo wrapper is preferable to scattering raw Font Awesome codepoints throughout UI code.

### watchOS findings

1. Native watchOS UI is SwiftUI, not Compose, so we should not force the Compose font pipeline onto `watchAppExtension`.
2. Although a bundled custom font is possible on watchOS, **vector assets in the Xcode asset catalog are the safer and simpler default** for the watch target.
3. The best cross-platform compromise is:
   - use **Font Awesome Free Brands** as the canonical icon source,
   - use a **shared logical icon key** across platforms,
   - render that key via a **Compose font-based renderer** on Compose targets,
   - render that same key via **native image assets** on watchOS.

## Recommended architecture decision

Use **one shared issuer normalization + icon-key registry in `sharedLib`**, and let each UI technology render that resolved icon key using its native mechanism.

### Why this is the right shape

1. Matching logic should not be duplicated between Compose, Wear OS, and watchOS.
2. Case-insensitive comparisons such as `github`, `Github`, and `GitHub` belong in shared business/presentation code, not in each UI.
3. watchOS should stay native and lightweight instead of depending on Compose-specific rendering assumptions.
4. A shared icon key keeps the catalog portable even if we later switch the rendering implementation on one platform.

## Recommended shared model

Introduce a small issuer presentation package in `sharedLib`, for example:

- `sharedLib/src/commonMain/kotlin/tech/arnav/twofac/lib/presentation/issuer/IssuerIconCatalog.kt`
- `sharedLib/src/commonMain/kotlin/tech/arnav/twofac/lib/presentation/issuer/IssuerIconMatch.kt`

Suggested responsibilities:

1. `normalizeIssuer(rawIssuer: String?): String?`
   - trim,
   - lowercase,
   - optionally collapse punctuation/spacing for known aliases,
   - return `null` if blank
2. `resolveIssuerIcon(rawIssuer: String?): IssuerIconMatch`
   - returns:
     - normalized issuer key,
     - stable icon key,
     - whether result is a placeholder fallback
3. a curated alias table
   - examples:
     - `github`, `github.com` -> `github`
     - `google`, `google account` -> `google`
     - `microsoft`, `microsoft account` -> `microsoft`
4. a curated known-issuer list
   - limited to popular issuers for the first rollout

### Important implementation rule

**Do not compare on display casing. Always normalize before lookup.**

That shared helper should be the only place that decides:

- whether an issuer is known,
- which icon key to use,
- when to fall back to the placeholder icon.

## Asset strategy

### Compose targets and Wear OS

Use a **shared Font Awesome Free Brands renderer** for:

- `composeApp` common Compose UI,
- `watchApp` Wear OS Compose UI.

Recommended path:

1. bundle the chosen Font Awesome asset in project resources,
2. keep a typed mapping from `iconKey -> codepoint`,
3. expose a reusable composable such as `IssuerBrandIcon(...)`,
4. add a single non-brand placeholder icon for unknown issuers.

### watchOS

Use **exported vector assets derived from Font Awesome Free Brands** in the Xcode asset catalog:

1. export only the curated issuer set plus the placeholder,
2. keep asset names aligned with the shared `iconKey`,
3. render them with a native SwiftUI `Image(...)` wrapper such as `IssuerBrandIconView`.

This keeps watchOS native while still using the same brand pack as the source of truth.

## Phase-by-phase implementation plan

## Phase 0 - Audit issuer-bearing UI surfaces and lock the cross-platform icon strategy

### Goals

- confirm every issuer-bearing UI surface,
- define the first issuer catalog,
- lock the rendering approach before code changes start.

### Steps

1. Inventory all UI surfaces that display issuer/account identity:
   - `composeApp` account list
   - `composeApp` home OTP cards
   - any detail/edit surfaces that should also show the icon
   - `watchApp` OTP pages
   - `iosApp/watchAppExtension` OTP pages
2. Decide the initial issuer list for v1:
   - start with a small set of high-value issuers such as GitHub, Google, Microsoft, Amazon, Discord, Dropbox, GitLab, Facebook, LinkedIn, Slack, Twitch, X/Twitter, etc.
3. Verify each chosen issuer exists in **Font Awesome Free Brands**.
4. Decide one placeholder icon that is not a brand icon and is safe to use everywhere.
5. Lock the cross-platform policy:
   - shared matching in `sharedLib`
   - Compose/Wear use Font Awesome glyph rendering
   - watchOS uses asset catalog vectors sourced from the same Font Awesome icons

### Expected output

- final v1 issuer list,
- final placeholder choice,
- confirmed rendering policy for Compose vs watchOS.

## Phase 1 - Add shared issuer normalization and icon-key resolution in sharedLib

### Goals

- centralize issuer matching,
- make casing irrelevant,
- avoid platform-specific lookup tables.

### Steps

1. Add a new shared helper package in `sharedLib/commonMain`.
2. Implement issuer normalization:
   - `trim()`
   - lowercase for comparison
   - handle blank/null defensively
3. Add alias normalization for known issuer variations:
   - domain forms,
   - display-name variants,
   - spacing/punctuation variants where needed
4. Introduce a stable icon-key model:
   - examples: `github`, `google`, `microsoft`, `placeholder`
5. Return a structured result instead of raw strings wherever possible so UI code does not re-derive fallback logic.
6. Add Swift-friendly API shape if needed so watchOS can call the same shared helper cleanly from `TwoFacKit`.
7. Extend the common display/presentation path for Compose:
   - either add `issuer` to `StoredAccount.DisplayAccount`,
   - or introduce a new UI-facing display model that includes issuer metadata
8. Ensure issuer parsing happens once when building display models, not repeatedly inside composables.

### Files likely touched

- `sharedLib/src/commonMain/kotlin/tech/arnav/twofac/lib/storage/StoredAccount.kt`
- new `sharedLib/src/commonMain/kotlin/tech/arnav/twofac/lib/presentation/issuer/*`
- any shared mapper that creates UI display accounts

### Expected output

- one shared source of truth for issuer matching,
- a UI-safe display model that can carry issuer data into Compose surfaces.

## Phase 2 - Prepare Font Awesome Free Brands assets plus a default placeholder icon

### Goals

- establish the icon source-of-truth files,
- avoid ad hoc asset naming,
- keep web/watch payload size under control.

### Steps

1. Add a curated icon asset manifest for the v1 issuer set.
2. Decide whether the Compose/Wear renderer ships:
   - a curated subset font,
   - or the upstream brands font if subsetting is not worth the initial complexity.
3. Store the Compose/Wear asset in the project’s resource pipeline with stable naming.
4. Create a typed Kotlin mapping:
   - `iconKey -> glyph/codepoint`
5. Create the watchOS asset equivalents:
   - one vector asset per icon in the Xcode asset catalog,
   - same stable `iconKey` naming
6. Add the placeholder asset to both pipelines.
7. Document the asset-ingest workflow so future icon additions stay consistent.

### Risk to manage

For Web/Wasm, large font assets can hurt startup. If the unsubsetted brand font is noticeably heavy, subsetting should become part of this phase before rollout.

### Expected output

- stable assets for Compose/Wear and watchOS,
- a maintainable mapping between shared icon keys and concrete platform assets.

## Phase 3 - Add shared Compose issuer icon rendering for Android, iOS, Desktop, Web, and Wear OS

### Goals

- provide one reusable renderer for Compose surfaces,
- keep raw Font Awesome glyph details out of feature components.

### Steps

1. Add a small shared UI helper in `composeApp/commonMain`, for example:
   - `IssuerBrandIcon(...)`
2. Make the composable accept:
   - raw issuer string or resolved icon match,
   - size,
   - tint,
   - content description behavior
3. Hook the composable up to the shared `IssuerIconCatalog` from `sharedLib`.
4. Implement consistent placeholder behavior so unknown issuers never produce empty leading space.
5. Validate rendering on:
   - Android
   - iOS
   - Desktop
   - Web/Wasm
6. Add web-specific preload or startup handling if glyphs do not appear on first composition.
7. Decide whether `watchApp` can reuse the same renderer directly or needs a tiny Android/Wear wrapper with the same shared catalog underneath.

### Files likely touched

- new `composeApp/src/commonMain/kotlin/tech/arnav/twofac/components/icons/*`
- `composeApp/build.gradle.kts` if the resource pipeline needs adjustment
- `watchApp` UI helper files if Wear requires a module-local wrapper

### Expected output

- a reusable issuer icon composable usable from any Compose-based screen.

## Phase 4 - Roll issuer icons through account-list and OTP surfaces

### Goals

- apply the icon renderer consistently wherever issuer identity is shown,
- keep layout stable even when issuer is unknown.

### Steps

1. Update `AccountListItem` to render a leading issuer icon.
2. Update `AccountsListContent` and upstream callers to pass issuer-aware display models.
3. Update `OTPCard` to display the issuer icon in the card header near the account identity.
4. Update any other shared account identity surfaces discovered in Phase 0.
5. Update `watchApp/src/main/java/tech/arnav/twofac/watch/ui/OtpAccountScreen.kt`
   - render the same icon key on Wear OS
   - keep the compact layout readable on small round screens
6. Update previews/sample data so known-issuer and unknown-issuer states are both covered.

### Files likely touched

- `composeApp/src/commonMain/kotlin/tech/arnav/twofac/components/accounts/AccountListItem.kt`
- `composeApp/src/commonMain/kotlin/tech/arnav/twofac/components/accounts/AccountsListContent.kt`
- `composeApp/src/commonMain/kotlin/tech/arnav/twofac/components/otp/OTPCard.kt`
- `watchApp/src/main/java/tech/arnav/twofac/watch/ui/OtpAccountScreen.kt`

### Expected output

- all Compose-driven account surfaces show:
  - brand icon for known issuers,
  - placeholder for unknown issuers,
  - correct behavior regardless of input casing.

## Phase 5 - Add native watchOS issuer icon rendering using Apple asset catalogs

### Goals

- keep watchOS native,
- reuse the shared matching logic,
- match the placeholder/brand behavior of the Compose targets.

### Steps

1. Add issuer vector assets plus placeholder asset to the watch extension asset catalog.
2. Create a small SwiftUI wrapper, for example:
   - `IssuerBrandIconView`
3. Call the shared `TwoFacKit` issuer-resolution helper from Swift if possible.
4. If Swift interop is awkward for the first pass, allow a thin Swift mapping layer that still uses the same shared `iconKey` names and documents the temporary duplication.
5. Update `WatchExtensionContentView.swift` to show the issuer icon above or beside issuer text.
6. Ensure accessibility is correct:
   - decorative icon if redundant with adjacent text,
   - labeled icon if used as a control or stand-alone identity marker
7. Validate legibility and spacing on small watch sizes.

### Files likely touched

- `iosApp/watchAppExtension/WatchExtensionContentView.swift`
- new `iosApp/watchAppExtension/*IssuerBrandIcon*.swift`
- watch extension asset catalog files

### Expected output

- watchOS shows the same known/unknown issuer icon behavior as the Compose targets while staying fully native.

## Phase 6 - Add tests, QA coverage, and icon-catalog maintenance docs

### Goals

- make matching deterministic,
- prevent regressions when issuers or assets are added later,
- document the process for extending the catalog.

### Steps

1. Add shared unit tests in `sharedLib` for normalization and lookup:
   - `github` -> GitHub icon
   - `Github` -> GitHub icon
   - `GitHub` -> GitHub icon
   - blank/null -> placeholder
   - unknown issuer -> placeholder
2. Add tests for alias handling:
   - domain forms like `github.com`
   - marketing-name variants where applicable
3. Add UI/previews/tests for at least:
   - known issuer on Compose
   - unknown issuer on Compose
   - Wear OS known/unknown layouts
4. Run a manual QA matrix across:
   - Android
   - iOS
   - Desktop
   - Web/Wasm
   - Wear OS
   - watchOS
5. Add a short maintenance note describing:
   - how to add a new issuer,
   - how to add aliases,
   - how to add the corresponding Compose glyph and watchOS asset,
   - how to verify the placeholder fallback still works

### Expected output

- stable cross-platform behavior,
- a repeatable workflow for growing the issuer catalog later.

## Suggested execution slices

1. **PR 1**
   - shared issuer normalization
   - icon-key registry
   - tests
2. **PR 2**
   - Compose/Wear asset pipeline
   - shared Compose icon renderer
3. **PR 3**
   - Compose account list and OTP surface rollout
   - Wear OS surface rollout
4. **PR 4**
   - native watchOS assets and SwiftUI renderer
5. **PR 5**
   - QA hardening
   - maintenance docs

## Success criteria

The feature is done when:

1. a known issuer consistently resolves to the same brand icon across all supported UI targets,
2. issuer matching is case-insensitive,
3. unknown issuers always show one placeholder icon instead of leaving the UI blank,
4. Compose, Wear OS, and watchOS all render the feature using stable, maintainable asset pipelines,
5. the catalog can be extended without duplicating lookup logic across platforms.
