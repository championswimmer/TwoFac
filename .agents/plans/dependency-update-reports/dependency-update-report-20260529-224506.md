---
name: Dependency Update Report 2026-05-29 22:45:02
status: Completed
generated_by: dependency-updates skill
---

# Dependency Update Report

Generated on `2026-05-29T22:45:02`.

## Repository dependency surfaces

- **Gradle / Kotlin Multiplatform:** `sharedLib`, `composeApp`, `cliApp`, `androidApp`, `watchApp` share `gradle/libs.versions.toml`
- **npm:** `website`, `composeApp/src/wasmJsMain/typescript`
- **Apple native wrappers:** `iosApp` contains Xcode project/workspace files but currently no `Podfile`, `Podfile.lock`, `Package.swift`, or `Package.resolved`
- **Ignore for dependency audits:** `kotlin-js-store/*.yarn.lock` exist without nearby `package.json`, so they are not first-class source package manifests
- **CocoaPods detected now:** no

## Gradle / Kotlin dependency surface

- Modules sharing the central version catalog: `sharedLib`, `composeApp`, `cliApp`, `androidApp`, `watchApp`
- Primary audit source: `gradle/libs.versions.toml`
- Audit command: `./gradlew refreshVersions`
- This repo currently keeps dependency versions in the version catalog; `versions.properties` is effectively only refreshVersions metadata.

### Stable catalog update candidates

| Key | Current | Recommended stable | Latest seen | Notes |
| --- | --- | --- | --- | --- |
| agp | 9.2.1 | — | 9.3.0-alpha09 | pre-release only |
| androidx-lifecycle | 2.10.0 | — | 2.11.0-beta01 | pre-release only |
| androidx-navigation | 2.9.2 | — | 2.10.0-alpha01 | pre-release only |
| biometric | 1.1.0 | — | 1.4.0-alpha07 | pre-release only |
| composeMultiplatform | 1.11.0 | — | 1.12.0-alpha01 | pre-release only |
| material3 | 1.10.0-alpha05 | — | 1.12.0-alpha01 | pre-release only |
| kotlin | 2.3.21 | — | 2.4.0-RC2 | pre-release only |
| kotlinStdlib | 2.3.21 | — | 2.4.0-RC2 | pre-release only |
| composeMaterial | 1.6.2 | — | 1.7.0-alpha03 | pre-release only |
| composeFoundation | 1.6.2 | — | 1.7.0-alpha03 | pre-release only |

### Hardcoded settings plugins to review manually

| Plugin | Pinned version |
| --- | --- |
| org.gradle.toolchains.foojay-resolver-convention | 1.0.0 |
| de.fayard.refreshVersions | 0.60.6 |

### Gradle wrapper suggestions from refreshVersions

- `./gradlew wrapper --gradle-version 9.6.0-rc-1`

## npm dependency surface

- npm projects in this repo: `website`, `composeApp/src/wasmJsMain/typescript`
- Audit command per project: `npm outdated --json`

### Direct dependency update candidates

No outdated npm dependencies were reported.

- `website`: no outdated direct dependencies
- `composeApp/src/wasmJsMain/typescript`: no outdated direct dependencies

## CocoaPods dependency surface

- No `Podfile` was found in this repository.
- `iosApp` currently uses Gradle-built `TwoFacUIKit` / `TwoFacKit` frameworks instead of CocoaPods-managed dependencies.
- Keep this script so future native iOS pods can be audited with `pod outdated --no-repo-update`.

## Validation commands after any future upgrade

- Root baseline: `./gradlew check`
- Shared/core regression: `./gradlew :sharedLib:allTests :cliApp:allTests`
- Compose/Desktop/Web regression: `./gradlew :composeApp:desktopTest :composeApp:wasmJsBrowserTest :composeApp:checkXcodeProjectConfiguration`
- Android wrappers: `./gradlew :androidApp:testDebugUnitTest :watchApp:testDebugUnitTest`
- Apple framework smoke build: `./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64 :sharedLib:linkDebugFrameworkIosSimulatorArm64`
- Website build: `cd website && npm run build`
- Wasm TypeScript interop build: `cd composeApp/src/wasmJsMain/typescript && npm run build`
- Optional UI/E2E follow-up: see `.agents/skills/ui-testing/SKILL.md`
