---
name: Library Upgrade Plan
status: Completed
progress:
  - "[x] Generate baseline dependency audit report with dependency-updates skill"
  - "[x] Review and stage low-risk npm wanted updates"
  - "[x] Review and stage stable Gradle/KMP dependency updates in small batches"
  - "[x] Re-test Android, Desktop, Web, CLI, and Apple framework smoke builds after each batch"
  - "[x] Evaluate major-version and pre-release candidates separately"
  - "[x] Land upgrades with rollback notes and follow-up issues for deferred items"
---

# Library Upgrade Plan

## Goal

Upgrade outdated libraries in this repository in a controlled way, keeping the default policy of **audit first, upgrade only intentionally**, validating each batch before moving to the next, and preferring **latest stable versions only** unless the user explicitly approves pre-release adoption.

## Audit baseline

A fresh audit was generated with:

```bash
node .agents/skills/dependency-updates/scripts/dependency-updates.mjs
```

Current report:

- `.agents/plans/dependency-update-reports/dependency-update-report-20260529-221818.md`

## Dependency surfaces

- **Gradle / Kotlin Multiplatform:** `sharedLib`, `composeApp`, `cliApp`, `androidApp`, `watchApp`
- **npm:** `website`, `composeApp/src/wasmJsMain/typescript`
- **CocoaPods:** none currently detected in this repo

## Current outdated dependency summary

### Gradle / Kotlin stable candidates

Highest-value stable candidates from the report:

- `agp`: `9.0.1` -> `9.2.1`
- `kotlin`: `2.3.10` -> `2.3.21`
- `composeMultiplatform`: `1.10.1` -> `1.11.0`
- `ktor`: `3.4.1` -> `3.5.0`
- `kotlinx-coroutines`: `1.10.2` -> `1.11.0`
- `kotlinx-serialization`: `1.9.0` -> `1.11.0`
- `koin-bom`: `4.1.1` -> `4.2.1`
- `filekit`: `0.10.0` -> `0.14.1`
- `kscan`: `0.7.0` -> `0.9.2`
- `playServicesWearable`: `19.0.0` -> `20.0.1`
- `playServicesAuth`: `21.4.0` -> `21.6.0`
- `composeBom`: `2026.02.01` -> `2026.05.01`
- `androidx-activity`: `1.12.4` -> `1.13.0`
- `androidx-lifecycle`: `2.9.6` -> `2.10.0`
- `androidxWork`: `2.11.1` -> `2.11.2`
- `zxing`: `3.5.3` -> `3.5.4`
- `jna`: `5.15.0` -> `5.18.1`
- `dokka`: `2.1.0` -> `2.2.0`
- `kover`: `0.9.7` -> `0.9.8`

### npm outdated direct dependencies

`website` wanted updates:

- `@tailwindcss/vite`: `4.2.2` -> `4.3.0`
- `@types/node`: `24.12.0` -> `24.12.4`
- `@unhead/vue`: `2.1.12` -> `2.1.15`
- `@vitejs/plugin-vue`: `6.0.5` -> `6.0.7`
- `marked`: `17.0.5` -> `17.0.6`
- `mermaid`: `11.13.0` -> `11.15.0`
- `tailwindcss`: `4.2.2` -> `4.3.0`
- `vite`: `8.0.3` -> `8.0.14`
- `vue`: `3.5.31` -> `3.5.35`
- `vue-tsc`: `3.2.6` -> `3.3.2`

Additional latest/major candidates to treat separately:

- `website/@unhead/vue`: latest `3.1.1`
- `website/vue-router`: latest `5.1.0`
- `website/typescript`: latest `6.0.3`
- `composeApp/src/wasmJsMain/typescript/typescript`: latest `6.0.3`

### Pre-release / manual-review Gradle items

Do **not** include these in the first upgrade pass:

- `androidx-navigation` -> `2.10.0-alpha01`
- `biometric` -> `1.4.0-alpha07`
- `material3` -> `1.12.0-alpha01`
- `kotlin` -> `2.4.0-RC2`
- `composeMultiplatform` -> `1.12.0-alpha01`
- `composeFoundation` / `composeMaterial` newer alpha lines
- Gradle wrapper `9.6.0-rc-1`

## Upgrade strategy

### Phase 1 — npm wanted updates only

Apply only safe in-range npm updates first:

```bash
node .agents/skills/dependency-updates/scripts/dependency-updates.mjs upgrade \
  --skip-gradle --skip-cocoapods \
  --npm-strategy wanted
```

Validation:

```bash
cd website && npm run build
cd composeApp/src/wasmJsMain/typescript && npm run build
```

### Phase 2 — low-risk Gradle library batch

Start with small stable libraries with narrower blast radius:

- `zxing`
- `jna`
- `dokka`
- `kover`
- `crypto-kt`
- `androidxWork`

Suggested command:

```bash
node .agents/skills/dependency-updates/scripts/dependency-updates.mjs upgrade \
  --skip-npm --skip-cocoapods \
  --gradle-keys zxing,jna,dokka,kover,crypto-kt,androidxWork
```

Validation:

```bash
./gradlew check
./gradlew :sharedLib:allTests :cliApp:allTests
```

### Phase 3 — app/runtime library batch

Next upgrade stable runtime libraries that are widely used but still below build-tool risk:

- `ktor`
- `kotlinx-coroutines`
- `kotlinx-serialization`
- `koin-bom`
- `filekit`
- `kscan`
- `playServicesAuth`
- `playServicesWearable`
- `composeBom`
- `androidx-activity`
- `androidx-lifecycle`

Suggested command:

```bash
node .agents/skills/dependency-updates/scripts/dependency-updates.mjs upgrade \
  --skip-npm --skip-cocoapods \
  --gradle-keys ktor,kotlinx-coroutines,kotlinx-serialization,koin-bom,filekit,kscan,playServicesAuth,playServicesWearable,composeBom,androidx-activity,androidx-lifecycle
```

Validation:

```bash
./gradlew check
./gradlew :androidApp:testDebugUnitTest :watchApp:testDebugUnitTest
./gradlew :composeApp:desktopTest :composeApp:wasmJsBrowserTest
```

### Phase 4 — build/toolchain batch

Upgrade the tightly coupled build stack together:

- Gradle wrapper -> `9.5.1`
- `agp` -> `9.2.1`
- `kotlin` / `kotlinStdlib` -> `2.3.21`
- `composeMultiplatform` -> `1.11.0`
- `composePwa` -> `0.6.1`

Suggested steps:

```bash
./gradlew wrapper --gradle-version 9.5.1
node .agents/skills/dependency-updates/scripts/dependency-updates.mjs upgrade \
  --skip-npm --skip-cocoapods \
  --gradle-keys agp,kotlin,kotlinStdlib,composeMultiplatform,composePwa
```

Validation:

```bash
./gradlew check
./gradlew :composeApp:desktopTest :composeApp:wasmJsBrowserTest :composeApp:checkXcodeProjectConfiguration
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64 :sharedLib:linkDebugFrameworkIosSimulatorArm64
./gradlew :androidApp:testDebugUnitTest :watchApp:testDebugUnitTest
```

### Phase 5 — deferred majors / pre-releases

These are out of scope for routine maintenance and require explicit user approval.

Status after this upgrade pass:

- `vue-router` 5.x: upgraded successfully
- TypeScript 6.x in website and wasm interop: upgraded successfully
- `@unhead/vue` 3.x: **deferred**; latest `vite-ssg` (`28.3.0`) still uses and documents `@unhead/vue` v2, and the website lost SSR/SSG head output when tested against v3
- alpha/beta/RC AndroidX, Compose, Kotlin, and Gradle wrapper candidates: still intentionally deferred

These should be done only after reviewing release notes and checking for API migrations.

## Validation checklist

Run after each upgrade batch as appropriate:

```bash
./gradlew check
./gradlew :sharedLib:allTests :cliApp:allTests
./gradlew :composeApp:desktopTest :composeApp:wasmJsBrowserTest :composeApp:checkXcodeProjectConfiguration
./gradlew :androidApp:testDebugUnitTest :watchApp:testDebugUnitTest
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64 :sharedLib:linkDebugFrameworkIosSimulatorArm64
cd website && npm run build
cd composeApp/src/wasmJsMain/typescript && npm run build
```

Optional follow-up:

- run UI/E2E checks from `.agents/skills/ui-testing/SKILL.md`
- smoke-test Android and Wear on emulator/device
- smoke-test iOS app and watch targets in Xcode

## Risk notes

- `agp`, `kotlin`, and `composeMultiplatform` are coupled and should be upgraded together
- `filekit`, `kscan`, and Google Play Services changes may affect platform integrations and permissions flows
- `vue-router` 5 and TypeScript 6 required code/config changes and should not be batched with routine wanted updates
- `@unhead/vue` 3 is currently blocked by `vite-ssg` v2-based head integration in this repo and should be revisited only with an upstream-compatible path
- pre-release Gradle/AndroidX/Compose updates should remain out of the default maintenance pass

## Done criteria

This plan is complete when:

- stable npm wanted updates are merged
- stable Gradle updates are merged in validated batches
- deferred major/pre-release candidates are either upgraded in follow-up work or explicitly documented as postponed
- a fresh dependency report shows no remaining intended stable/wanted updates for the completed scope, except `@unhead/vue` 3.x which is intentionally deferred for upstream compatibility
