---
name: Dependency Update Report 2026-05-29 21:35:34
status: Completed
generated_by: dependency-updates skill
---

# Dependency Update Report

Generated on `2026-05-29T21:35:34`.

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
| agp | 9.0.1 | 9.2.1 | 9.3.0-alpha09 | newer pre-release available |
| androidx-activity | 1.12.4 | 1.13.0 | 1.13.0 |  |
| androidx-lifecycle | 2.9.6 | 2.10.0 | 2.11.0-beta01 | newer pre-release available |
| androidx-navigation | 2.9.2 | — | 2.10.0-alpha01 | pre-release only |
| biometric | 1.1.0 | — | 1.4.0-alpha07 | pre-release only |
| composeMultiplatform | 1.10.1 | 1.11.0 | 1.12.0-alpha01 | newer pre-release available |
| composePwa | 0.5.0 | 0.6.1 | 0.6.1 |  |
| material3 | 1.10.0-alpha05 | — | 1.12.0-alpha01 | pre-release only |
| kotlin | 2.3.10 | 2.3.21 | 2.4.0-RC2 | newer pre-release available |
| ktor | 3.4.1 | 3.5.0 | 3.5.0 |  |
| kotlinx-coroutines | 1.10.2 | 1.11.0 | 1.11.0 |  |
| kotlinStdlib | 2.3.10 | 2.3.21 | 2.4.0-RC2 | newer pre-release available |
| kotlinx-serialization | 1.9.0 | 1.11.0 | 1.11.0 |  |
| koin-bom | 4.1.1 | 4.2.1 | 4.2.1 |  |
| crypto-kt | 0.5.0 | 0.6.0 | 0.6.0 |  |
| dokka | 2.1.0 | 2.2.0 | 2.2.0 |  |
| kover | 0.9.7 | 0.9.8 | 0.9.8 |  |
| playServicesWearable | 19.0.0 | 20.0.1 | 20.0.1 |  |
| playServicesAuth | 21.4.0 | 21.6.0 | 21.6.0 |  |
| composeBom | 2026.02.01 | 2026.05.01 | 2026.05.01 |  |
| kscan | 0.7.0 | 0.9.2 | 0.9.2 |  |
| filekit | 0.10.0 | 0.14.1 | 0.14.1 |  |
| zxing | 3.5.3 | 3.5.4 | 3.5.4 |  |
| jna | 5.15.0 | 5.18.1 | 5.18.1 |  |
| composeMaterial | 1.5.6 | 1.6.2 | 1.7.0-alpha03 | newer pre-release available |
| composeFoundation | 1.5.6 | 1.6.2 | 1.7.0-alpha03 | newer pre-release available |
| androidxWork | 2.11.1 | 2.11.2 | 2.11.2 |  |

### Hardcoded settings plugins to review manually

| Plugin | Pinned version |
| --- | --- |
| org.gradle.toolchains.foojay-resolver-convention | 1.0.0 |
| de.fayard.refreshVersions | 0.60.6 |

### Gradle wrapper suggestions from refreshVersions

- `./gradlew wrapper --gradle-version 9.5.1`
- `./gradlew wrapper --gradle-version 9.6.0-rc-1`

## npm dependency surface

- npm projects in this repo: `website`, `composeApp/src/wasmJsMain/typescript`
- Audit command per project: `npm outdated --json`

### Direct dependency update candidates

| Project | Package | Type | Current | Wanted | Latest |
| --- | --- | --- | --- | --- | --- |
| website | @tailwindcss/vite | dependency | 4.2.2 | 4.3.0 | 4.3.0 |
| website | @types/node | devDependency | 24.12.0 | 24.12.4 | 25.9.1 |
| website | @unhead/vue | dependency | 2.1.12 | 2.1.15 | 3.1.1 |
| website | @vitejs/plugin-vue | devDependency | 6.0.5 | 6.0.7 | 6.0.7 |
| website | marked | devDependency | 17.0.5 | 17.0.6 | 18.0.4 |
| website | mermaid | dependency | 11.13.0 | 11.15.0 | 11.15.0 |
| website | tailwindcss | dependency | 4.2.2 | 4.3.0 | 4.3.0 |
| website | typescript | devDependency | 5.9.3 | 5.9.3 | 6.0.3 |
| website | vite | devDependency | 8.0.3 | 8.0.14 | 8.0.14 |
| website | vue | dependency | 3.5.31 | 3.5.35 | 3.5.35 |
| website | vue-router | dependency | 4.6.4 | 4.6.4 | 5.1.0 |
| website | vue-tsc | devDependency | 3.2.6 | 3.3.2 | 3.3.2 |
| composeApp/src/wasmJsMain/typescript | typescript | devDependency | 5.9.2 | 5.9.2 | 6.0.3 |

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
