---
name: gradle-build
description: Use Gradle commands to build and run the app by platform.
---
# Skill: Gradle Build Commands

Use these commands to build and run the app by platform.

## Common checks

- Repository baseline: `./gradlew check`
- Shared library tests: `./gradlew :sharedLib:test`
- CLI tests: `./gradlew :cliApp:test`
- Compose tests: `./gradlew :composeApp:test`

## Guidance

- Prefer module-scoped tasks for faster feedback.
- Run full checks only when change scope spans multiple modules.

---

## Build and Run by Platform

### CLI App (`cliApp`)

Native CLI executables for desktop platforms:

| Platform | Build Debug | Build Release | Output Location |
|----------|-------------|---------------|-----------------|
| macOS (Intel) | `./gradlew :cliApp:linkDebugExecutableMacosX64` | `./gradlew :cliApp:linkReleaseExecutableMacosX64` | `cliApp/build/bin/macosX64/` |
| macOS (Apple Silicon) | `./gradlew :cliApp:linkDebugExecutableMacosArm64` | `./gradlew :cliApp:linkReleaseExecutableMacosArm64` | `cliApp/build/bin/macosArm64/` |
| Linux | `./gradlew :cliApp:linkDebugExecutableLinuxX64` | `./gradlew :cliApp:linkReleaseExecutableLinuxX64` | `cliApp/build/bin/linuxX64/` |
| Windows | `./gradlew :cliApp:linkDebugExecutableMingwX64` | `./gradlew :cliApp:linkReleaseExecutableMingwX64` | `cliApp/build/bin/mingwX64/` |

Run the CLI: `./cliApp/build/bin/<target>/debugExecutable/twofac.kexe`

### Desktop GUI App (`composeApp` - Desktop)

| Action | Command |
|--------|---------|
| Run dev build | `./gradlew :composeApp:run` |
| Run release build | `./gradlew :composeApp:runRelease` |
| Create distributable | `./gradlew :composeApp:createDistributable` |
| Package for current OS | `./gradlew :composeApp:packageDistributionForCurrentOS` |
| Package release for current OS | `./gradlew :composeApp:packageReleaseDistributionForCurrentOS` |

Platform-specific packages (run on respective OS):

| Platform | Package Commands |
|----------|-----------------|
| macOS | `:composeApp:packageDmg`, `:composeApp:packageReleaseDmg` |
| Linux | `:composeApp:packageDeb`, `:composeApp:packageReleaseDeb` |
| Windows | `:composeApp:packageMsi`, `:composeApp:packageReleaseMsi` |

Uber JAR (cross-platform):

| Action | Command |
|--------|---------|
| Debug uber JAR | `./gradlew :composeApp:packageUberJarForCurrentOS` |
| Release uber JAR | `./gradlew :composeApp:packageReleaseUberJarForCurrentOS` |

Output: `composeApp/build/compose/binaries/main/`

### Android Mobile App (`androidApp`)

| Action | Command |
|--------|---------|
| Build debug APK | `./gradlew :androidApp:assembleDebug` |
| Build release APK | `./gradlew :androidApp:assembleRelease` |
| Install debug | `./gradlew :androidApp:installDebug` |
| Uninstall | `./gradlew :androidApp:uninstallDebug` |
| Build bundle (AAB) | `./gradlew :androidApp:bundle` |

Output: `androidApp/build/outputs/apk/` or `androidApp/build/outputs/bundle/`

Run/install on a specific emulator and launch the app explicitly:

```bash
ANDROID_SERIAL=emulator-5554 ./gradlew :androidApp:installDebug
adb -s emulator-5554 shell am start -n tech.arnav.twofac.app/.MainActivity
adb -s emulator-5554 shell dumpsys activity activities | rg 'Resumed: ActivityRecord|mCurrentFocus|mFocusedApp'
```

When multiple Android devices are connected, always set `ANDROID_SERIAL` for Gradle and use `adb -s <serial>` for launch/verification commands.

### Wear OS Watch App (`watchApp`)

| Action | Command |
|--------|---------|
| Build debug APK | `./gradlew :watchApp:assembleDebug` |
| Build release APK | `./gradlew :watchApp:assembleRelease` |
| Install debug | `./gradlew :watchApp:installDebug` |
| Uninstall | `./gradlew :watchApp:uninstallDebug` |
| Build bundle (AAB) | `./gradlew :watchApp:bundle` |

Output: `watchApp/build/outputs/apk/` or `watchApp/build/outputs/bundle/`

### iOS App (`composeApp` + `iosApp`)

Framework linking (for Xcode integration):

| Action | Command |
|--------|---------|
| Debug framework (device) | `./gradlew :composeApp:linkDebugFrameworkIosArm64` |
| Debug framework (simulator) | `./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64` |
| Release framework (device) | `./gradlew :composeApp:linkReleaseFrameworkIosArm64` |
| Release framework (simulator) | `./gradlew :composeApp:linkReleaseFrameworkIosSimulatorArm64` |

Output: `composeApp/build/bin/<target>/debugFramework/` or `releaseFramework/`

**Running the iOS app**: Open `iosApp/iosApp.xcworkspace` in Xcode and run from there. Xcode triggers `embedAndSignAppleFrameworkForXcode` automatically.

**Xcode integration**: The task `embedAndSignAppleFrameworkForXcode` is called by Xcode's build phase.

### watchOS (future)

Not yet implemented. See `.agents/plans/02-watchos-companion-app.md` for planned architecture.

### Web/Wasm (`composeApp` - Wasm)

| Action | Command |
|--------|---------|
| Dev server | `./gradlew :composeApp:wasmJsBrowserDevelopmentRun` |
| Production build | `./gradlew :composeApp:wasmJsBrowserProductionWebpack` |
| Combined distribution | `./gradlew :composeApp:composeCompatibilityBrowserDistribution` |

Output: `composeApp/build/dist/wasmJs/`
