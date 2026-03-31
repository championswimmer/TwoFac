---
name: Code Simplification Plan - androidApp
status: Completed
progress:
  - "[x] Phase 0: Thin-wrapper cleanup"
  - "[x] Phase 1: Backup and release hardening"
  - "[x] Phase 2: Validate wrapper remains minimal"
---

# Code Simplification Plan - androidApp

## Scope
- Module: `androidApp` (thin Android host wrapper for `composeApp`)
- Goal: keep wrapper minimal, remove redundancy, and close obvious security/config gaps.

## Steelmanned Findings (research-backed)

### ✅ KEPT: Duplicate resource file
- `androidApp/src/main/res/drawable/ic_launcher_foreground.xml`
- `androidApp/src/main/res/drawable-anydpi-v26/ic_launcher_foreground.xml`
- Files are byte-identical (confirmed with `diff`).
- **Decision: delete the `drawable-anydpi-v26/` copy, keep `drawable/`.**
- `mipmap-anydpi-v26/ic_launcher.xml` references `@drawable/ic_launcher_foreground` without a qualifier; the resource system resolves correctly from `drawable/`.
- All other adaptive-icon layers (`ic_launcher_background.xml`, `ic_launcher_monochrome.xml`) already live only in `drawable/`; keeping the foreground there as well makes the asset structure consistent.

### ❌ DROPPED: Remove `composeMultiplatform` plugin from androidApp
- Original claim: the wrapper module doesn't need it.
- **Research verdict: it IS needed.**
  - `org.jetbrains.compose` ensures version parity between the shared library's Compose runtime and the host app; mismatches cause binary incompatibility.
  - It drives proper packaging of `composeResources` from the shared library into the final APK.
  - `org.jetbrains.kotlin.plugin.compose` is separately required (Kotlin 2.0+) because `MainActivity.kt` contains `@Preview` and calls `@Composable` functions.
  - JetBrains docs and community guidance confirm: apply both plugins to every module that contains Composable code or links Compose resources.
- **No change needed.**

### ❌ DROPPED: Remove "transitive" dependencies (koin, filekit, appdirs)
- Original claim: those deps are already provided transitively by `:composeApp`.
- **Research verdict: they are NOT transitively exposed to `androidApp`.**
  - In `:composeApp/build.gradle.kts` all three are declared as `implementation(...)`, not `api(...)`.  `implementation` does not add a library to the consumer's *compile* classpath.
  - `androidApp` code uses these symbols **directly**:
    - `TwoFacApplication.kt` calls `attachAppDirs()` (appdirs) and passes a lambda using `modules()` (koin-core).
    - `MainActivity.kt` calls `FileKit.manualFileKitCoreInitialization()` and `FileKit.init(this)` (filekit-core/dialogs).
  - Removing these from `androidApp/build.gradle.kts` would immediately break compilation.
  - Relying on accidental compile-classpath leakage from library `implementation` deps is a Gradle anti-pattern that breaks with every AGP/Gradle upgrade.
- **No change needed.**

### ✅ KEPT: Backup posture not explicit — add `dataExtractionRules`
- `android:allowBackup="true"` without `dataExtractionRules` is a critical-severity finding for any 2FA app:
  - ADB extraction, Google Drive auto-backup, and device-to-device transfers can all exfiltrate TOTP shared secrets.
  - Android 12 (API 31) added `dataExtractionRules`; on API ≤ 30 the older `fullBackupContent` attribute covers the same ground.
- **Important caveat:** the app implements intentional backup (`androidBackupModule` in `composeApp/androidMain`), so `allowBackup=false` is too blunt — it would silently break the feature. The correct fix is:
  - Add `res/xml/data_extraction_rules.xml` with `disableIfNoEncryptionCapabilities="true"` in `<cloud-backup>` and strict excludes in `<device-transfer>`.
  - Add `res/xml/backup_rules.xml` with matching excludes for Android 11 and below (`fullBackupContent`).
  - Reference both attributes in the manifest.
- minSdk = 30 means both files are needed to cover the full supported range.

### ✅ KEPT: Release minification disabled
- `isMinifyEnabled = false` in the release build type is a genuine gap: no code shrinking, no obfuscation, larger APK, harder to audit.
- **Caution:** KMP + Compose Multiplatform require a well-tuned ProGuard/R8 config. Common breakage points: kotlinx.serialization serializers, Koin reflection-based injection, Compose internal classes.
- The recommended workflow:
  1. Enable `isMinifyEnabled = true` + `isShrinkResources = true` in release.
  2. Start from `proguard-android-optimize.txt`.
  3. Add library-specific rules; prefer `consumer-rules.pro` in `:composeApp` so rules ship with the library automatically.
  4. Check `build/outputs/mapping/release/missing_rules.txt` after the first release build and copy any suggested rules.
- **Note:** `:composeApp` currently has no `consumer-rules.pro`; this should be created alongside enabling minification.

### ❌ DROPPED: Theme light/dark bar consolidation
- Original claim: XML (`themes.xml` / `themes-night.xml`) and the runtime `SideEffect` in `MainActivity.kt` are duplicate sources of truth; one can be removed.
- **Research verdict: both layers are intentional and serve distinct roles.**
  - `themes.xml` / `themes-night.xml` — applied by the OS to the window *before* `Activity.onCreate` runs. They set the initial status/navigation bar icon colours for the splash/loading instant. Without them, devices get a jarring flash of wrong-colour icons on startup.
  - `SideEffect { WindowInsetsControllerCompat(...) }` — runs after the first Compose composition; it is the canonical way to drive dynamic light/dark changes at runtime (e.g. the user toggling system dark mode while the app is open).
  - Android guidance (2024+): keep a safe XML default, override programmatically in Compose via `SideEffect`. Neither replaces the other.
- **Minor cosmetic issue noted:** `themes-night.xml` inherits from `Theme.Material.Light.NoActionBar` even for dark mode (should be `Theme.Material.NoActionBar`). This is harmless at runtime because Compose renders the actual UI colours, and `windowLightStatusBar=false` is the only flag that matters from that file. Not worth changing.
- **No change needed.**

---

## Simplification Roadmap

### Phase 0: Thin-wrapper cleanup
- [ ] Delete `androidApp/src/main/res/drawable-anydpi-v26/ic_launcher_foreground.xml`.
- [ ] Confirm launcher icon unchanged: run `./gradlew :androidApp:assembleDebug` and inspect the icon.

### Phase 1: Backup and release hardening
- [ ] Create `androidApp/src/main/res/xml/data_extraction_rules.xml` with `disableIfNoEncryptionCapabilities="true"` in `<cloud-backup>` and `<exclude domain="root" path="."/>` in `<device-transfer>`.
- [ ] Create `androidApp/src/main/res/xml/backup_rules.xml` with equivalent excludes for Android ≤ 11 (`fullBackupContent`).
- [ ] Add `android:dataExtractionRules="@xml/data_extraction_rules"` and `android:fullBackupContent="@xml/backup_rules"` to `AndroidManifest.xml`.
- [ ] Set `isMinifyEnabled = true` and `isShrinkResources = true` in the release build type.
- [ ] Add a `proguard-rules.pro` in `androidApp/` starting from `proguard-android-optimize.txt`.
- [ ] Create `composeApp/consumer-rules.pro` with KMP/Compose/Koin/Serialization keep rules and reference it via `consumerProguardFiles`.
- [ ] Verify release build: `./gradlew :androidApp:assembleRelease`; check `missing_rules.txt` and iterate.

### Phase 2: Validate wrapper remains minimal
- [ ] Build checks:
  - `./gradlew :androidApp:assembleDebug`
  - `./gradlew :androidApp:assembleRelease`
- [ ] Smoke test startup, biometric prompt path, and import/export screens on a release build.

## Success Criteria
- No duplicate icon assets.
- Explicit and secure backup policy in manifest and XML rule files, preserving intentional backup functionality.
- Release build uses minification/optimization safely with documented keep rules.
- Wrapper plugin and dependency declarations confirmed necessary and correct.
