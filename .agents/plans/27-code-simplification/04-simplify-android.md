---
name: Code Simplification Plan - androidApp
status: Planned
progress:
  - "[ ] Phase 0: Thin-wrapper cleanup"
  - "[ ] Phase 1: Build script dependency/plugin declutter"
  - "[ ] Phase 2: Backup and release hardening"
  - "[ ] Phase 3: Theme/source-of-truth cleanup"
  - "[ ] Phase 4: Validate wrapper remains minimal"
---

# Code Simplification Plan - androidApp

## Scope
- Module: `androidApp` (thin Android host wrapper for `composeApp`)
- Goal: keep wrapper minimal, remove redundancy, and close obvious security/config gaps.

## High-Signal Findings (evidence)
1. **Duplicate resource file**
   - `androidApp/src/main/res/drawable/ic_launcher_foreground.xml`
   - `androidApp/src/main/res/drawable-anydpi-v26/ic_launcher_foreground.xml`
   - byte-identical duplicate.
2. **Potentially redundant plugin in pure wrapper module**
   - `androidApp/build.gradle.kts` applies `composeMultiplatform` in app wrapper.
3. **Dependencies likely duplicated transitively through `:composeApp`**
   - `koin` core/bom
   - `filekit` core/dialogs
   - `kotlin.multiplatform.appdirs`
4. **Sensitive-data backup posture not explicit**
   - `android:allowBackup="true"` in `AndroidManifest.xml` without `dataExtractionRules`.
5. **Release minification disabled**
   - `isMinifyEnabled = false` in release build.
6. **Theme light/dark bar controls defined in XML and runtime SideEffect**
   - XML in `res/values/themes.xml` and `res/values-night/themes.xml`
   - runtime override in `MainActivity.kt` using `WindowInsetsControllerCompat`.

## Simplification Roadmap

### Phase 0: Thin-wrapper cleanup
- [ ] Delete duplicate drawable in `drawable-anydpi-v26`.
- [ ] Confirm launcher icon behavior unchanged via local run.

### Phase 1: Build script dependency/plugin declutter
- [ ] Validate whether `composeMultiplatform` plugin is required in wrapper module; remove if unnecessary.
- [ ] Remove dependencies already provided transitively by `:composeApp` (after compile verification).
- [ ] Keep only true wrapper-level dependencies (`activity-compose`, `fragment-ktx`, tooling).

### Phase 2: Backup and release hardening
- [ ] Introduce `android:dataExtractionRules` and explicit backup include/exclude policy.
- [ ] Decide and document secure choice:
  - disable backup (`allowBackup=false`) for maximum secrecy, or
  - allow controlled backup with sensitive files excluded.
- [ ] Enable release minification + baseline keep rules and verify app startup flows.

### Phase 3: Theme/source-of-truth cleanup
- [ ] Consolidate status/navigation bar appearance control to one source of truth.
- [ ] Remove redundant night XML if runtime logic is canonical.

### Phase 4: Validate wrapper remains minimal
- [ ] Build checks:
  - `./gradlew :androidApp:assembleDebug`
  - `./gradlew :androidApp:assembleRelease`
- [ ] Smoke test startup, biometric prompt path, and import/export screens.

## Success Criteria
- Wrapper contains only entrypoint, manifest, and essential resources.
- No duplicate icon assets or redundant dependency declarations.
- Explicit and secure backup policy documented in manifest/resources.
- Release build uses minification/optimization safely.
