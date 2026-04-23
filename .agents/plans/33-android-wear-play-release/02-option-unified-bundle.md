---
name: Option 2 — "Unified" bundle / single-module build for phone + Wear OS
status: Draft (research)
---

> **Legend**
> - 🧑 **HUMAN** — requires a person (Play Console, credential decisions, device testing)
> - 🤖 **AGENT** — pure code/config change; can be done autonomously

# Option 2 — "Unified" bundle / single-module build for phone + Wear OS

## TL;DR — the honest answer

**A single AAB file that contains both the phone app and the Wear OS app is
not currently permitted by Google Play.** The legacy "embedded wearable APK"
mechanism that used to make this possible was deprecated in August 2023 and
has since been removed from Play Console accepting paths for new apps and
updates.

> *Wear OS apps can no longer be embedded into a handheld app's APK. Any new
> app submission or update that uses this mechanism will be rejected at
> upload time.*
> — [Android Wear OS packaging guidelines](https://developer.android.com/wear/publish/packaging-guidelines)

So what follows is the realistic interpretation of the problem statement:

1. A **literal single bundle** is not a supported outcome. We document why.
2. A **build-level unification** *is* possible — one Gradle module with
   flavors (`phone`, `wear`) producing two AABs from shared source sets.
   The output you upload to Play is still two AABs, but your source tree
   only has one `:app` module.
3. A **code-level unification** (sharing UI / DI / screens between phone and
   watch) is possible but independent of packaging — the watch and phone
   Compose UIs in this repo are almost entirely disjoint today.

If the underlying driver for "merge the modules" is **release automation
and/or versioning convenience**, Option 1 already gives you those benefits
for less work. If the underlying driver is **code sharing between phone and
watch**, that is a separate refactor and Option 2 below is the starting
point.

---

## Why a single AAB is not possible (detailed)

Google Play's historical model let you place the Wear APK inside
`res/raw/wearable_app.apk` in the phone APK/AAB, with a
`<meta-data android:name="com.google.android.wearable.beta.app" />` entry.
This was Wear 1.x-era behaviour and was kept alive as a compatibility path
through Wear OS 2.

Status today:

- Wear OS 3+ (Pixel Watch, Galaxy Watch 4+, etc.) only installs **standalone**
  Wear apps delivered directly to the watch via the Play Store on-device.
- The on-device Wear Play Store consumes an **independent AAB** that was
  published to the same listing — it does not extract from the phone bundle.
- Play Console's upload validator **rejects** AABs that contain an embedded
  wearable APK, for apps whose first version was uploaded after Aug 2023.

Consequences for TwoFac:

- Even if we built a combined AAB locally (e.g. with a custom Gradle task
  that drops the watch APK into `res/raw`), Play would reject it.
- There is no currently-documented public API to put the watch module as a
  conditional-delivery Dynamic Feature targeting `android.hardware.type.watch`
  — Play's Dynamic Feature conditional delivery supports
  `deviceFeature`/`minSdk`/`userCountries`, but **Wear OS is explicitly not
  a documented target**, and community attempts confirm it does not work.

**Conclusion:** regardless of what we build locally, we must upload two
AABs to Play.

---

## What "unification" could still mean

Since the *wire format* (AAB pair on a single listing) can't change, the
remaining levers are all on the *build side*:

| Level of unification | What changes | What stays the same |
|---------------------|---------------|---------------------|
| A — Shared versioning only | Same as Option 1 | Two Gradle modules, two source sets |
| B — Shared signing + CI    | Signing convention plugin, single CI job for both modules | Two Gradle modules |
| C — Single Gradle module with `phone`/`wear` product flavors | One `:androidApp` module producing two AABs via flavors | Manifests, resources, DI still split per flavor |
| D — Shared Compose code between phone + watch | Shared UI components / view models usable on both form factors | Two variants, two AABs |

Levels A and B are *already part of Option 1* (the overview calls out the
signing convention plugin as a shared decision). Level C is what this
document explores as "the closest thing to a single bundle". Level D is a
code-sharing effort orthogonal to packaging.

---

## Level C — single-module build with `phone` and `wear` flavors

### Proposed module layout

```
androidApp/
├── build.gradle.kts
└── src/
    ├── main/                         # code shared between phone + wear (mostly empty today)
    ├── phone/                        # was androidApp/src/main/*
    │   ├── AndroidManifest.xml
    │   ├── kotlin/
    │   └── res/
    └── wear/                         # was watchApp/src/main/*
        ├── AndroidManifest.xml
        ├── kotlin/
        └── res/
```

`watchApp/` is deleted; `settings.gradle.kts` drops `include(":watchApp")`.

### `androidApp/build.gradle.kts` sketch

🤖

```kotlin
android {
    namespace = "tech.arnav.twofac.app"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "tech.arnav.twofac.app"
        versionName = rootProject.extra["appVersionName"] as String
    }

    flavorDimensions += "formFactor"

    productFlavors {
        create("phone") {
            dimension = "formFactor"
            minSdk = libs.versions.android.minSdk.get().toInt()
            targetSdk = libs.versions.android.targetSdk.get().toInt()
            versionCode = rootProject.extra["appVersionCodePhone"] as Int
        }
        create("wear") {
            dimension = "formFactor"
            minSdk = 30
            targetSdk = libs.versions.android.targetSdk.get().toInt()
            versionCode = rootProject.extra["appVersionCodeWear"] as Int
            useLibrary("wear-sdk")  // only for the wear flavor
        }
    }

    buildTypes { /* unchanged — release + signing */ }
    compileOptions { /* unchanged */ }
}

// Dependencies fence-off per flavor via `phoneImplementation(...)` and `wearImplementation(...)`
dependencies {
    implementation(project(":sharedLib"))

    // Phone-only
    "phoneImplementation"(project(":composeApp"))
    "phoneImplementation"(libs.androidx.activity.compose)
    // ... rest of androidApp's current deps

    // Wear-only
    "wearImplementation"(libs.play.services.wearable)
    "wearImplementation"(platform(libs.androidx.compose.bom))
    "wearImplementation"(libs.androidx.wear)
    // ... rest of watchApp's current deps
}
```

Build outputs you'd then get:

- `androidApp/build/outputs/bundle/phoneRelease/app-phone-release.aab`
- `androidApp/build/outputs/bundle/wearRelease/app-wear-release.aab`

The CI step reduces from two `bundleRelease` invocations across two modules
to one: `./gradlew :androidApp:bundlePhoneRelease :androidApp:bundleWearRelease`
(or simply `bundleRelease` to build both).

### What this actually buys you

Pros:

- Single `applicationId` declared in one place.
- Single signing config (no convention plugin needed).
- Single dependency list per flavor — duplicated deps surface clearly.
- Single manifest-merge root — shared `<application>` attributes can live in
  `src/main/AndroidManifest.xml`.
- Easier code sharing if/when you decide to extract reusable pieces.

Cons (why this is **not** what Option 1 gives you for free):

- **High refactor cost.** Every Kotlin package move invalidates import
  paths in tests, manifest references, Koin modules, and Wear sync glue.
  Watch tests run under `./gradlew :watchApp:test` today; they'd need to
  move to `./gradlew :androidApp:testWearDebugUnitTest` — a different task
  name that will break any existing CI / Maestro / contributor workflow
  that references the watch module by name.
- **Manifest-merging fragility.** The phone and watch manifests diverge
  on `<application>` attributes (`android:name`, `android:theme`,
  `android:roundIcon`), on `<uses-permission>`, and on intent filters.
  AGP does merge per-flavor manifests correctly, but debugging a bad merge
  is painful.
- **Dependency conflicts.** The watch app uses `androidx.compose.material`
  (Wear Material) and the phone uses Compose Multiplatform's material3
  transitively via `:composeApp`. Co-housing them in one module's
  dependency graph means Gradle has to resolve both variants of Compose —
  version alignment issues that don't exist today will surface.
- **Identical Play outcome.** You still upload two AABs. You still stagger
  versionCodes. You still maintain two sets of release notes. **Your users
  see no difference.**
- **Touches every contributor doc.** `AGENTS.md`, `.agents/skills/kmp-modules`,
  `.agents/skills/module-routing`, `CLAUDE.md`, README — all reference
  `:watchApp` and would need rewording.
- **Loses Kotlin/Native watchOS reuse path.** The iOS watch companion is
  built against `sharedLib` today. If the Android wear module ever wants to
  pull watch-domain code (paging, sync state machines) into shared code,
  having it behind an Android flavor makes that harder than having it in a
  proper module.

### Mitigations

- Keep the watch code a git-untouched copy during refactor — move files
  wholesale with `git mv` and adjust package names only if truly
  necessary.
- Stage the refactor across multiple commits:
  1. Introduce the single-module skeleton alongside the old modules
     (duplicated temporarily).
  2. Port watch tests, verify `:androidApp:testWearDebugUnitTest` green.
  3. Switch CI to the new module.
  4. Delete `:watchApp`.
- Before starting, audit: is there actually any code the two modules want
  to share? Today the answer appears to be **no** — both depend on
  `:sharedLib` directly and their Compose layers are disjoint.

---

## Recommendation (this file, not the overview)

**Do not pursue Option 2 purely for release unification.** You will pay the
refactor cost and receive, on the Play Store side, *exactly* the same
outcome as Option 1: two AABs on one listing with staggered versionCodes.

Revisit Level C (or D) **only** when a concrete code-sharing requirement
appears — for example:

- The wear app starts needing the phone's icon catalog / issuer-brand
  icons shared from `composeApp/androidMain`.
- A watch-specific feature needs a phone-side counterpart that should live
  in the same source set.
- You want to run UI tests that instrument both form factors from one
  Gradle project.

Until then, Option 1 is strictly better: faster to implement, lower risk,
identical user-visible result.

---

## If you still want to do it — sketch of the work breakdown

### Phase 1 — preparation (🤖 unless marked)

- [ ] Decide on flavor names (`phone` / `wear`) and commit to them in
      `AGENTS.md`.
- [ ] Decide on package structure: is `tech.arnav.twofac.watch.*` staying
      or collapsing into `tech.arnav.twofac.app.wear.*`? Package rename is
      the highest-churn change.
- [ ] Ensure all watch tests pass on main first (`./gradlew :watchApp:test`)
      so regressions are easy to spot.

### Phase 2 — scaffold new module (🤖)

- [ ] Create `androidApp/src/phone/` and `androidApp/src/wear/` skeleton
      with empty manifests.
- [ ] Move one file at a time from `:androidApp` → `androidApp/src/phone/`
      to prove the `phone` flavor builds.
- [ ] Repeat for `:watchApp` → `androidApp/src/wear/`.
- [ ] Add `productFlavors` block and per-flavor dependencies.
- [ ] Verify `./gradlew :androidApp:bundlePhoneRelease
      :androidApp:bundleWearRelease` produces two signed AABs locally.

### Phase 3 — tests and CI (🤖)

- [ ] Port watch unit tests to the new flavor source set.
- [ ] Update `.github/workflows/*.yml` every reference to `:watchApp` →
      `:androidApp:*Wear*`.
- [ ] Update Maestro tests (if any) to launch the correct variant.
- [ ] Remove `:watchApp` from `settings.gradle.kts`.

### Phase 4 — documentation (🤖)

- [ ] Update `AGENTS.md` module table — remove `watchApp/` row.
- [ ] Update `.agents/skills/module-routing/SKILL.md`.
- [ ] Update `.agents/skills/kmp-modules/SKILL.md`.
- [ ] Update any README references.

### Phase 5 — Play Store (identical to Option 1 from here on)

- [ ] Everything in sections 5–10 of `01-option-staggered-versioning.md`
      applies verbatim, with two Gradle task names substituted:
      `:androidApp:publishPhoneReleaseBundle` and
      `:androidApp:publishWearReleaseBundle`.

---

## Open questions (Option 2 specific)

- 🧑 Does the `applicationId` stay exactly `tech.arnav.twofac.app` for both
  flavors (required for single-listing), and do you want to keep a
  `.wear` suffix on internal packages for code navigation?
- 🧑 Does the wear flavor keep its own `R` class / drawables directory,
  or is there a benefit to sharing drawables (app icon, launcher) across
  flavors? Wear's launcher icon needs a circular adaptive icon variant.
- 🧑 Are you willing to break downstream tooling that references
  `:watchApp` (CI, Maestro, docs) as part of this refactor?

---

## References

- [Wear OS packaging guidelines (deprecation of embedded wearable APKs)](https://developer.android.com/wear/publish/packaging-guidelines)
- [Multi-device experience (recommended split-AAB model)](https://developer.android.com/wear/publish/multi-device-experience)
- [Product flavors and source sets](https://developer.android.com/build/build-variants)
- [Dynamic feature conditional delivery (why it does NOT target Wear)](https://developer.android.com/guide/playcore/feature-delivery/conditional)
- [Gradle Play Publisher plugin](https://github.com/Triple-T/gradle-play-publisher)

---

*Last updated: April 2026. Re-verify Google Play's Wear OS packaging policy
before basing any implementation work on this document — the rules around
embedding and form-factor delivery have changed twice in the last three
years.*
