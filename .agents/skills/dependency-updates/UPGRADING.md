# Dependency Upgrade Reference

Default behavior for this skill is **audit only**. Use the scripts below to generate a report first, then only apply upgrades when explicitly requested.

Default upgrade policy is **latest stable versions only**. Do **not** upgrade to alpha, beta, RC, or other pre-release builds unless the user explicitly asks for them or approves them after review.

## Audit only

```bash
node .agents/skills/dependency-updates/scripts/dependency-updates.mjs
```

That writes a markdown report to `.agents/plans/dependency-update-reports/dependency-update-report-<timestamp>.md`.

## Explicit upgrade commands

### Upgrade Gradle/Kotlin catalog entries to the latest stable candidates

This is the normal/default path.

```bash
node .agents/skills/dependency-updates/scripts/dependency-updates.mjs upgrade --skip-npm --skip-cocoapods
```

### Upgrade only selected Gradle keys

```bash
node .agents/skills/dependency-updates/scripts/dependency-updates.mjs upgrade \
  --skip-npm --skip-cocoapods \
  --gradle-keys kotlin,ktor,composeMultiplatform
```

### Allow Gradle pre-releases too

Only do this when the user explicitly requests or approves pre-release adoption.

```bash
node .agents/skills/dependency-updates/scripts/dependency-updates.mjs upgrade \
  --skip-npm --skip-cocoapods \
  --allow-prerelease
```

### Upgrade npm projects within existing semver ranges

```bash
node .agents/skills/dependency-updates/scripts/dependency-updates.mjs upgrade \
  --skip-gradle --skip-cocoapods \
  --npm-strategy wanted
```

### Upgrade npm direct dependencies to latest, including major bumps

Only do this after review, because major jumps can require code changes even when they are stable releases.

```bash
node .agents/skills/dependency-updates/scripts/dependency-updates.mjs upgrade \
  --skip-gradle --skip-cocoapods \
  --npm-strategy latest
```

### Upgrade a subset of npm projects

```bash
node .agents/skills/dependency-updates/scripts/dependency-updates.mjs upgrade \
  --skip-gradle --skip-cocoapods \
  --projects website
```

### Upgrade CocoaPods if the repo adds a Podfile later

```bash
node .agents/skills/dependency-updates/scripts/dependency-updates.mjs upgrade \
  --skip-gradle --skip-npm \
  --cocoapods-repo-update
```

Or target specific pods:

```bash
node .agents/skills/dependency-updates/scripts/dependency-updates.mjs upgrade \
  --skip-gradle --skip-npm \
  --cocoapods-pods Alamofire,Kingfisher
```

## Validation checklist after any upgrade

### Core Gradle checks

```bash
./gradlew check
./gradlew :sharedLib:allTests :cliApp:allTests
./gradlew :composeApp:desktopTest :composeApp:wasmJsBrowserTest :composeApp:checkXcodeProjectConfiguration
./gradlew :androidApp:testDebugUnitTest :watchApp:testDebugUnitTest
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64 :sharedLib:linkDebugFrameworkIosSimulatorArm64
```

### npm / web checks

```bash
cd website && npm run build
cd composeApp/src/wasmJsMain/typescript && npm run build
```

### Optional deeper verification

- Run module-scoped builds from `.agents/skills/gradle-build/SKILL.md`
- Run UI/E2E flows from `.agents/skills/ui-testing/SKILL.md`
- If Android or Wear libraries changed substantially, add emulator/device smoke tests
- If iOS-facing libraries changed, open `iosApp/iosApp.xcodeproj` or workspace in Xcode and build the app/watch targets once

## Current repo-specific notes

- The main KMP dependency surface is centralized in `gradle/libs.versions.toml`
- `settings.gradle.kts` still contains hardcoded plugin versions that should be reviewed manually
- Current npm projects are `website` and `composeApp/src/wasmJsMain/typescript`
- `iosApp` currently has no `Podfile`, `Podfile.lock`, `Package.swift`, or `Package.resolved`
- `kotlin-js-store/*.yarn.lock` should be ignored for dependency audits unless a real package manifest is introduced there
