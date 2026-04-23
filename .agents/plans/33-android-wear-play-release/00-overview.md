---
name: Android + Wear OS Play Release Automation — Overview
status: Draft
---

> **Legend**
> - 🧑 **HUMAN** — requires a person (Play Console, credential decisions, device testing)
> - 🤖 **AGENT** — pure code/config change; can be done autonomously

# Android + Wear OS Play Release Automation — Overview

## Goal

Distribute `androidApp` (phone) and `watchApp` (Wear OS) **together** on the
Google Play Store from a single release pipeline, with deterministic versioning
and a single `git tag v*` trigger.

The two sub‑plans in this folder explore two different strategies. This
overview compares them and states the recommendation.

---

## Current State (starting point)

| Aspect                  | `androidApp`                         | `watchApp`                           |
|-------------------------|--------------------------------------|--------------------------------------|
| Gradle module           | `:androidApp`                        | `:watchApp`                          |
| `applicationId`         | `tech.arnav.twofac.app`              | `tech.arnav.twofac.app` (same)       |
| `versionCode`           | `rootProject.extra["appVersionCode"]` (e.g. `260401062`) | same value (identical) |
| `versionName`           | `rootProject.extra["appVersionName"]` (e.g. `1.6.2`) | same value (identical) |
| `minSdk`                | `libs.versions.android.minSdk`       | `30`                                 |
| `targetSdk` / `compileSdk` | `libs.versions.android.targetSdk` / `compileSdk` | same |
| Signing                 | Release keystore wired via env vars, enforced for `*Release` tasks | **No signing config** — release builds would be unsigned |
| `<uses-feature watch>`  | absent                               | present                              |
| Standalone flag         | n/a                                  | `com.google.android.wearable.standalone = false` |
| CI release workflow     | `./gradlew :androidApp:assembleRelease` → APK artifact uploaded to GitHub Release; **no Play upload**, **no AAB**, **no watch build** | — |

References:
- `androidApp/build.gradle.kts`
- `watchApp/build.gradle.kts`
- `build.gradle.kts` (root — `appVersionCode`, `appVersionName`)
- `.github/workflows/release.yml`
- `.agents/plans/publishing-plans/01-android-publish.md`

---

## Key constraint (from Play Store policy research)

Since **August 2023** Google Play has **removed support for embedding a Wear OS
APK inside a phone app bundle** (the legacy `wearApp` / "embedded wearable
APK" mechanism). New apps and updates must ship the Wear OS app as its own
AAB.

- 📖 [Wear OS packaging guidelines](https://developer.android.com/wear/publish/packaging-guidelines)
- 📖 [Multi-device experience](https://developer.android.com/wear/publish/multi-device-experience)

However, Google Play **does** support uploading **two separate AABs to the same
app listing** when they share an `applicationId`. Play filters delivery by
form factor using the `<uses-feature android:name="android.hardware.type.watch">`
manifest entry — watches receive the watch AAB, phones receive the phone AAB.

This constraint is the lens through which both options below are framed.

---

## The two options at a glance

|                                 | **Option 1 — Staggered versionCodes, two modules** | **Option 2 — "Unified" bundle / single-module build** |
|---------------------------------|----------------------------------------------------|-------------------------------------------------------|
| Number of AABs uploaded         | 2 (phone + wear) to same listing                   | 2 (phone + wear) to same listing — *see note below*   |
| Gradle modules                  | Keep `:androidApp` + `:watchApp` separate          | Merge into one module with `phone` / `wear` flavors   |
| `versionCode` strategy          | Phone ends in `0`, wear ends in `1` (`…062`/`…063` with staggering, or `…620`/`…621`) | Same staggering pattern, derived in one place |
| Play listing                    | Single listing                                     | Single listing                                        |
| Can ship as **one** AAB file?   | No (not allowed by Play)                           | **No, not today.** Legacy "embedded wearApp" is dead. The closest we can get is unifying the *build* (one Gradle module, one signing config, one CI step) while still producing two AAB artifacts. |
| Migration risk to existing code | Minimal — add versionCode offset + wear signing + CI steps | High — refactor module layout, manifest merging, resources, dependencies, tests, Koin DI, Wear sync code paths |
| Time to first Play release      | Low (days)                                         | High (weeks) — and end-user outcome is *identical* to Option 1 |
| Best suited for                 | Shipping ASAP with clean release automation        | Long-term consolidation if the watch app grows to share more UI/logic with phone |

> **Note on "single bundle":** the problem statement asks whether the two
> modules can be *merged* so that a **single bundle** is produced. As of 2025
> this is not permitted by Google Play for Wear OS — see Option 2 for the
> detailed explanation of what is and isn't possible, and what the realistic
> "merged" outcome looks like.

---

## Recommendation

**Adopt Option 1** (staggered versionCodes across two modules) for the initial
Play Store release. It:

- Matches Google's current recommended packaging model.
- Requires the least code churn.
- Gives a clean, deterministic version-code scheme that avoids collisions
  forever.
- Is fully automatable in the existing `release.yml`.
- Leaves the door open to migrate to Option 2 later if the watch app ever
  starts sharing UI / DI with the phone app (today it does not — watch uses
  `:sharedLib` directly and has its own Compose UI stack).

Option 2 is documented as an alternative, but should only be pursued if a
separate driver (code sharing, not release packaging) justifies the refactor.
The **user-visible release outcome is the same either way** — Play still
requires two AABs on the listing.

---

## File index

| File | Contents |
|------|----------|
| `00-overview.md` | This file — comparison, constraints, recommendation |
| `01-option-staggered-versioning.md` | **Option 1** — keep two modules, stagger `versionCode`, automate two‑AAB release |
| `02-option-unified-bundle.md` | **Option 2** — research into a single bundle, feasibility verdict, and what a *build-level* unification would look like |

---

## Shared decisions (apply to both options)

These decisions are independent of which option is picked and should be
settled before implementation:

1. 🧑 **Play Console account**: confirm developer account is active and the
   app listing `tech.arnav.twofac.app` has been created per
   `.agents/plans/publishing-plans/01-android-publish.md`.
2. 🧑 **Service account for automation**: create a GCP service account with
   "Service Account User" + grant it access in Play Console under
   *Users & permissions → Invite new users → Service Account* with the roles
   *Release manager* (or *Release to production* + *Create and edit draft
   releases*). Download JSON key, store as GitHub secret
   `PLAY_SERVICE_ACCOUNT_JSON`.
3. 🤖 **Play Publisher plugin**: adopt
   [`com.github.triplet.play`](https://github.com/Triple-T/gradle-play-publisher)
   v3.12+ (Gradle Play Publisher). Alternative: `fastlane supply`. Plugin is
   chosen because the repo is already Gradle-first and has no Ruby tooling.
4. 🤖 **Signing for `watchApp`**: mirror the `androidApp` signing setup so
   the watch AAB can be signed with the same upload keystore (both AABs must
   be signed with the same key since they share the `applicationId` and Play
   App Signing is per-listing).
5. 🤖 **AAB (bundle) instead of APK**: switch the release workflow from
   `assembleRelease` to `bundleRelease` for both modules. Play Console
   requires AABs for new apps since August 2021.
6. 🤖 **Mapping file upload**: both modules have R8 enabled (`androidApp`) or
   minification (`watchApp` — `isMinifyEnabled = true`). The Play Publisher
   plugin uploads `mapping.txt` automatically when
   `mappingFile` is present.
7. 🧑 **Release track policy**: default publish track is `internal` for the
   first tag; promote to `production` manually via Play Console until
   confidence is high, then flip the workflow default.
8. 🤖 **Version-code derivation**: replace the bare integer in root
   `build.gradle.kts` with a small helper that returns the *phone* and *wear*
   versionCodes from one base number (see Option 1 for the exact scheme).
   Keep `versionName` identical across both form factors.

---

## Open questions for the human

- Do you want CI to publish **automatically** to the Play `internal` track on
  every `v*` tag, or only produce the two AABs and upload them as GitHub
  Release artifacts (manual Play upload)? The sub-plans assume "yes, automate
  internal track" but both work without it.
- Do you want to enable **staged rollout** (e.g., 10 % → 50 % → 100 %) for
  production releases? Play Publisher supports this via `userFraction`.
- Should Wear OS have its own release-notes block in the Play listing, or
  reuse the phone release notes verbatim? (Play allows per-AAB release notes
  but not per-form-factor release notes within one release.)
