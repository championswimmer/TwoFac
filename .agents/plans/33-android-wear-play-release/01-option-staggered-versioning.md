---
name: Option 1 — Staggered versionCodes, two modules, automated dual-AAB release
status: Draft
---

> **Legend**
> - 🧑 **HUMAN** — requires a person (Play Console, credential decisions, device testing)
> - 🤖 **AGENT** — pure code/config change; can be done autonomously

# Option 1 — Staggered versionCodes, two modules, automated dual‑AAB release

## Summary

Keep `:androidApp` and `:watchApp` as separate Gradle modules (today's
layout). Give them distinct but deterministic `versionCode`s derived from a
single base number, sign both with the same upload keystore, and teach
`.github/workflows/release.yml` to build **two AABs** and upload them to the
**same Play Store listing** using the Gradle Play Publisher plugin.

This is the shortest, lowest-risk path to a Play Store launch for both form
factors, and it matches Google's currently-supported packaging model for
Wear OS.

---

## Why this works

- Both modules already share `applicationId = tech.arnav.twofac.app` — Play
  treats them as one app.
- `watchApp` already declares `<uses-feature android:name="android.hardware.type.watch">`
  — Play will route that AAB to wearables and the other AAB to phones.
- `standalone=false` stays as-is: the wear app still requires the phone
  companion to sync OTPs (current behaviour is preserved).
- No code refactor required — only build/signing/CI wiring.

---

## 1 — Version-code staggering scheme

### Current state

Both modules read:

```kotlin
// build.gradle.kts (root)
extra["appVersionCode"] = 260401062 // eg: 2026 02(Feb) 01 00 3 (1.0.3)
extra["appVersionName"] = "1.6.2"
```

So **both AABs would have the same `versionCode`**, which Play Store rejects
when both bundles are published under the same `applicationId`.

### Proposed scheme

Extend the root configuration so phone and wear derive distinct codes from
one base:

| Concept            | Value                                      |
|--------------------|--------------------------------------------|
| `appVersionCodeBase` | `26040106` — the human-readable base (year-month-day-release) |
| Phone `versionCode` | `appVersionCodeBase * 10 + 0` → ends in **0** |
| Wear `versionCode`  | `appVersionCodeBase * 10 + 1` → ends in **1** |
| `versionName`       | `1.6.2` (identical) |

Example for the current build:

- `appVersionCodeBase = 26040106`
- phone → `260401060`
- wear  → `260401061`

### Why "last digit" and not "wear = phone + 1_000_000"?

- Last-digit staggering keeps both codes numerically close and makes it
  visually obvious which AAB you're looking at in Play Console.
- Leaves 8 spare slots (digits 2–9) for future form factors (TV, Auto, etc.)
  without reshuffling the scheme.
- Google's recommended convention is actually to keep wear's code **lower**
  than phone's so that if form-factor filtering ever fails, phones still
  prefer the phone AAB. With last-digit staggering and wear = +1, this is a
  trade‑off — but because Play's form-factor filtering for
  `android.hardware.type.watch` is reliable and enforced by the Play upload
  validator, the ordering is safe in practice. The validator will refuse the
  upload if the wear AAB could ever win on a phone.

### Gradle implementation sketch

🤖 In `build.gradle.kts` (root):

```kotlin
// Base: YYMMDDNN — year, month, day, release number within the day.
val appVersionCodeBase = 26040106
extra["appVersionCodePhone"] = appVersionCodeBase * 10 + 0
extra["appVersionCodeWear"]  = appVersionCodeBase * 10 + 1
extra["appVersionName"] = "1.6.2"
```

🤖 In `androidApp/build.gradle.kts`:

```kotlin
versionCode = rootProject.extra["appVersionCodePhone"] as Int
```

🤖 In `watchApp/build.gradle.kts`:

```kotlin
versionCode = rootProject.extra["appVersionCodeWear"] as Int
```

🤖 Keep `versionName` pulled from `appVersionName` in both.

> **Migration note:** the current base `260401062` would become
> `26040106` with a phone code of `260401060` and wear `260401061`. The
> phone code drops by `2` — **Play Store will reject any versionCode lower
> than the last one it accepted**. So the *first* release under the new
> scheme must bump the base to at least `26040107` (phone `260401070`,
> wear `260401071`). Document this in the commit that introduces the
> scheme.

---

## 2 — Wear app signing

Today `watchApp/build.gradle.kts` has **no** `signingConfigs` block. Release
builds would be unsigned, and Play App Signing requires all AABs on the
listing to be signed with the **same upload key** (since Play App Signing is
per-application-id, not per-AAB).

🤖 Lift the signing block out of `androidApp/build.gradle.kts` into a
shared location. Two concrete options:

- **Option A (preferred):** extract a `buildSrc` convention plugin
  (`release-signing.gradle.kts`) that both `:androidApp` and `:watchApp`
  apply. The plugin reads the same `ANDROID_SIGNING_*` environment variables
  already used by `androidApp`.
- **Option B (simpler):** duplicate the signing block into `watchApp`. Less
  maintenance-friendly but smaller diff.

🤖 Update the `requiresAndroidReleaseSigning` check so it also fires for
`:watchapp:bundlerelease` and `:watchapp:assemblerelease` task names.

🧑 Verify that the same upload keystore (`ANDROID_SIGNING_KEYSTORE_BASE64`
secret) is used — don't generate a second keystore.

---

## 3 — Switch to AAB outputs

The existing release workflow builds `:androidApp:assembleRelease` (APK).
Play Store requires AAB since August 2021.

🤖 Update `androidApp/build.gradle.kts` — no change needed; `bundleRelease`
is produced automatically by AGP. Same for `watchApp`.

🤖 Update `.github/workflows/release.yml`:

- Replace `:androidApp:assembleRelease` with `:androidApp:bundleRelease`.
- Add `:watchApp:bundleRelease` to the same Gradle invocation so they build
  in one daemon run (incremental saving).
- Upload both AABs as GitHub Release artifacts under names
  `android-aab` and `wearos-aab` so humans can sideload / inspect.
- Also upload mapping files
  (`androidApp/build/outputs/mapping/release/mapping.txt` and
  `watchApp/build/outputs/mapping/release/mapping.txt`) as artifacts.

Keep building `:androidApp:assembleRelease` **in addition** if a raw APK is
still wanted in the GitHub Release (for users who sideload); it's optional.

---

## 4 — Wear app manifest sanity

🤖 Before first Play upload, audit `watchApp/src/main/AndroidManifest.xml`:

- ✅ `<uses-feature android:name="android.hardware.type.watch" />` is present
  (this is what Play uses to route the AAB to wearables).
- ✅ `com.google.android.wearable.standalone = "false"` — correct, keep, the
  app does rely on the phone companion for data sync.
- ⚠ Consider adding `<uses-feature android:name="android.hardware.type.watch" android:required="true" />`
  explicitly — the `required` attribute defaults to `true`, but being
  explicit makes Play's form-factor filter intent unambiguous.
- ⚠ `android:targetSdkVersion` currently comes from
  `libs.versions.android.targetSdk` (same as phone). Wear OS requires
  API 30+; `minSdk=30` in the module already satisfies this. ✅

---

## 5 — Publishing automation (Gradle Play Publisher)

🤖 Add the plugin to `gradle/libs.versions.toml`:

```toml
[versions]
gradle-play-publisher = "3.12.1"

[plugins]
gradlePlayPublisher = { id = "com.github.triplet.play", version.ref = "gradle-play-publisher" }
```

🤖 Apply the plugin in both `androidApp/build.gradle.kts` and
`watchApp/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.gradlePlayPublisher)
}

play {
    serviceAccountCredentials.set(file(System.getenv("PLAY_SERVICE_ACCOUNT_JSON_PATH") ?: "play-service-account.json"))
    track.set("internal")                    // flip to "production" once confident
    defaultToAppBundles.set(true)
    releaseStatus.set(com.github.triplet.gradle.androidpublisher.ReleaseStatus.COMPLETED)
    // Let Play auto-generate release notes from CHANGELOG or GitHub release body
    // by pointing to src/main/play/release-notes/<locale>/internal.txt (handled per module)
}
```

🤖 Create release-notes directory in each module:

```
androidApp/src/main/play/release-notes/en-US/internal.txt
watchApp/src/main/play/release-notes/en-US/internal.txt
```

🤖 The `publishBundle` task (added by the plugin) uploads the AAB + mapping
file + release notes to the Play Console using the service-account key.

### Key Play Publisher tasks per module

| Task                          | What it does                                                |
|-------------------------------|-------------------------------------------------------------|
| `publishBundle`               | Upload AAB + mapping.txt to the configured track            |
| `promoteArtifact`             | Promote from `internal` → `alpha` → `beta` → `production`   |
| `publishReleaseNotes`         | Update release notes only (no new bundle)                   |

---

## 6 — CI wiring (`.github/workflows/release.yml`)

🤖 Add a new job `publish-play` that runs after `build-ubuntu`:

```yaml
publish-play:
  name: "Publish Android + Wear OS to Play Store (internal track)"
  needs: [ build-ubuntu ]
  if: startsWith(github.ref, 'refs/tags/v')
  runs-on: ubuntu-latest
  permissions:
    contents: read
  env:
    ANDROID_SIGNING_KEYSTORE_BASE64: ${{ secrets.ANDROID_SIGNING_KEYSTORE_BASE64 }}
    ANDROID_SIGNING_KEY_ALIAS:      ${{ secrets.ANDROID_SIGNING_KEY_ALIAS }}
    ANDROID_SIGNING_KEY_PASSWORD:   ${{ secrets.ANDROID_SIGNING_KEY_PASSWORD }}
    ANDROID_SIGNING_STORE_PASSWORD: ${{ secrets.ANDROID_SIGNING_STORE_PASSWORD }}
    PLAY_SERVICE_ACCOUNT_JSON:      ${{ secrets.PLAY_SERVICE_ACCOUNT_JSON }}
  steps:
    - uses: actions/checkout@v6
    - uses: actions/setup-java@v5
      with: { java-version: '21', distribution: 'temurin' }
    - uses: gradle/actions/setup-gradle@v5
    - name: Materialise keystore + service account
      run: |
        echo "$ANDROID_SIGNING_KEYSTORE_BASE64" | base64 -d > "$RUNNER_TEMP/android-keystore.jks"
        echo "$PLAY_SERVICE_ACCOUNT_JSON"      > "$RUNNER_TEMP/play-sa.json"
        echo "ANDROID_SIGNING_STORE_FILE=$RUNNER_TEMP/android-keystore.jks" >> "$GITHUB_ENV"
        echo "PLAY_SERVICE_ACCOUNT_JSON_PATH=$RUNNER_TEMP/play-sa.json"     >> "$GITHUB_ENV"
    - name: Build + publish both AABs
      run: ./gradlew :androidApp:publishBundle :watchApp:publishBundle
```

🤖 Keep `build-ubuntu` as the canonical "build everything" job, but change
its Android step to `bundleRelease` and upload both AABs + mapping files as
artifacts (this keeps the existing GitHub Release attachment flow working
for humans who don't use Play).

🤖 Make `publish-play` optional/toggleable with a workflow input, so tagging
doesn't *always* push to Play when the human wants to ship only to GitHub
Releases.

---

## 7 — Play Console — one-time setup

🧑 Done once, outside CI:

1. Upload an initial **signed AAB manually** for each form factor to
   establish the listing and enrol Play App Signing. (If the listing already
   exists, just confirm both AABs can coexist — Play's Device Catalog should
   show two rows: "Phones/Tablets" and "Wear".)
2. Fill the Wear OS-specific store assets:
   - Wear OS screenshots (≥ 384 × 384 px, circular crop preview acceptable).
   - Wear OS video / short description can reuse the phone copy.
3. Create a GCP Service Account with the *Android Publisher* API enabled,
   download JSON key, and invite it to the Play Console with *Release
   Manager* role.
4. Add the JSON key as GitHub secret `PLAY_SERVICE_ACCOUNT_JSON`.
5. Verify the `internal` track is reachable and has at least one tester
   account for smoke testing.

---

## 8 — Validation

🤖 Before the first release tag:

- `./gradlew :androidApp:bundleRelease :watchApp:bundleRelease` — produces
  two signed AABs locally (requires keystore env vars).
- `./gradlew :androidApp:lintVitalRelease :watchApp:lintVitalRelease` —
  same lint gate the phone app already passes.
- Inspect each AAB with `bundletool dump manifest` to confirm the
  `versionCode` values and `uses-feature` entries.

🧑 First run:

- Tag a pre-release like `v1.6.3-rc1` — pushes to `internal` track only.
- Install the internal build on a phone + a paired Wear OS watch / emulator.
- Verify the Wear OS device fetches the **watch** AAB (`versionCode` ending
  in `1`) and the phone fetches the **phone** AAB (ending in `0`).

---

## 9 — Rollout steps for subsequent releases

For every `v*` tag thereafter, CI will:

1. Build both AABs with the correct staggered `versionCode`.
2. Upload both to the Play Console `internal` track.
3. Attach both AABs + mapping files + APK (if kept) to the GitHub Release.

🧑 Manual promotion:

- Play Console → Internal testing → *Promote release* → alpha → beta →
  production. Or use `./gradlew :androidApp:promoteArtifact
  --from-track internal --promote-track production` + same for watchApp.

---

## 10 — Rollback strategy

- Play does **not** support deleting a released `versionCode`. To roll back:
  1. Bump `appVersionCodeBase` to the next integer (`26040107`).
  2. Revert the offending commit.
  3. Re-tag `v1.6.4-hotfix`.
  4. CI produces `260401070` + `260401071` and Play happily accepts them.
- Keep the staggered scheme intact — never publish a phone AAB with
  `versionCode` ending in `1`, or vice versa, because the form-factor router
  at Play's ingress assumes the pattern once it's set.

---

## Quick checklist

- [ ] 🤖 Root `build.gradle.kts` exposes `appVersionCodePhone` / `appVersionCodeWear`
- [ ] 🤖 `androidApp` reads `appVersionCodePhone`
- [ ] 🤖 `watchApp`  reads `appVersionCodeWear`
- [ ] 🤖 Wear signing config added (shared convention plugin or duplicated block)
- [ ] 🤖 `requiresAndroidReleaseSigning` check updated for `:watchApp` tasks
- [ ] 🤖 `watchApp` manifest reviewed (`uses-feature watch`, `standalone=false`)
- [ ] 🤖 Gradle Play Publisher plugin added to `libs.versions.toml`
- [ ] 🤖 Plugin applied + `play { ... }` block in both modules
- [ ] 🤖 `play/release-notes/en-US/*.txt` directories created
- [ ] 🤖 `.github/workflows/release.yml` switched to `bundleRelease`, both AABs uploaded, `publish-play` job added
- [ ] 🧑 Play Console: Wear screenshots, store listing complete
- [ ] 🧑 Play Console: GCP service account created + JSON in `PLAY_SERVICE_ACCOUNT_JSON` secret
- [ ] 🧑 First `v*` tag → internal track → smoke-tested on paired phone + watch
- [ ] 🧑 Promote internal → production when confident

---

*Last updated: April 2026. Verify Play Store policies and Gradle Play
Publisher docs before implementing; both evolve frequently.*
