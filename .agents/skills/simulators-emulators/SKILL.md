---
name: simulators-emulators
description: How to list, pick, and boot Android emulators and iOS simulators for local app runs.
---
# Skill: Simulators and Emulators

> IMPORTANT: 
> Preferred iOS Simulator Name: `iPhone 16 w Watch` (local), `iPhone 16` CI
> Preferred Android Emulator Name: `Pixel_9` (local), `pixel_6` CI

Use this skill to discover local Android emulators and iOS simulators, pick a target interactively, and boot it before running the app.

It also supports clearing app data for a specific Android package or resetting an iOS Simulator app by uninstalling its bundle from a selected simulator.

## Companion scripts

- Shared helpers: `.agents/skills/simulators-emulators/scripts/simulatorTools.mjs`
- Android picker: `.agents/skills/simulators-emulators/scripts/android-emulator-picker.mjs`
- iOS picker: `.agents/skills/simulators-emulators/scripts/ios-simulator-picker.mjs`

## Common checks

- Android AVD list: `emulator -list-avds`
- Running Android devices: `adb devices -l`
- iOS devices/runtimes (JSON): `xcrun simctl list --json`

## Android emulators

### List and pick

- List with script: `node .agents/skills/simulators-emulators/scripts/android-emulator-picker.mjs --list`
- Interactive pick: `node .agents/skills/simulators-emulators/scripts/android-emulator-picker.mjs`
- Pick and boot: `node .agents/skills/simulators-emulators/scripts/android-emulator-picker.mjs --boot`
- Pick by AVD name: `node .agents/skills/simulators-emulators/scripts/android-emulator-picker.mjs --avd Pixel_8_API_34 --boot`
- Clear app data for a package on the selected emulator: `node .agents/skills/simulators-emulators/scripts/android-emulator-picker.mjs --boot --clear-app-data tech.arnav.twofac`
- Clear app data for a package on a specific AVD: `node .agents/skills/simulators-emulators/scripts/android-emulator-picker.mjs --avd Pixel_8_API_34 --clear-app-data tech.arnav.twofac`

### Run app on selected emulator

Use shell output to set `ANDROID_SERIAL`:

```bash
eval "$(node .agents/skills/simulators-emulators/scripts/android-emulator-picker.mjs --boot --shell)"
ANDROID_SERIAL="$ANDROID_SERIAL" ./gradlew :androidApp:installDebug
adb -s "$ANDROID_SERIAL" shell am start -n tech.arnav.twofac/.MainActivity
adb -s "$ANDROID_SERIAL" shell dumpsys activity activities | rg 'Resumed: ActivityRecord|mCurrentFocus|mFocusedApp'
```

When multiple emulators/devices are connected, always pass `ANDROID_SERIAL` (or `adb -s <serial>`) to avoid ambiguous target errors.

### Clear app data on Android

The Android script uses `adb shell pm clear <package>` against the selected emulator. This keeps the app installed but removes its local state:

```bash
node .agents/skills/simulators-emulators/scripts/android-emulator-picker.mjs \
  --avd Pixel_8_API_34 \
  --clear-app-data tech.arnav.twofac
```

Use `--boot` as well if you want the script to explicitly boot the emulator first; the script will also boot automatically when `--clear-app-data` targets a stopped emulator.

## iOS simulators

### List and pick

- List with script: `node .agents/skills/simulators-emulators/scripts/ios-simulator-picker.mjs --list`
- Interactive pick: `node .agents/skills/simulators-emulators/scripts/ios-simulator-picker.mjs`
- Pick and boot: `node .agents/skills/simulators-emulators/scripts/ios-simulator-picker.mjs --boot`
- Pick by UDID: `node .agents/skills/simulators-emulators/scripts/ios-simulator-picker.mjs --udid <UDID> --boot`
- Clear app data for a bundle on the selected simulator: `node .agents/skills/simulators-emulators/scripts/ios-simulator-picker.mjs --clear-app-data tech.arnav.twofac`
- Clear app data for a bundle on a specific simulator: `node .agents/skills/simulators-emulators/scripts/ios-simulator-picker.mjs --udid <UDID> --clear-app-data tech.arnav.twofac`

### Run app on selected simulator

Use shell output to set `IOS_SIMULATOR_UDID`:

```bash
eval "$(node .agents/skills/simulators-emulators/scripts/ios-simulator-picker.mjs --boot --shell)"
xcodebuild \
  -workspace iosApp/iosApp.xcworkspace \
  -scheme iosApp \
  -destination "platform=iOS Simulator,id=$IOS_SIMULATOR_UDID"
```

UDID is preferred over simulator name because names are often duplicated across runtimes.

### Clear app data on iOS Simulator

For iOS Simulator, the script clears app data by uninstalling the selected app bundle with `xcrun simctl uninstall <UDID> <bundleId>`. This removes the app sandbox, so reinstall the app afterward before launching it again:

```bash
node .agents/skills/simulators-emulators/scripts/ios-simulator-picker.mjs \
  --udid "$IOS_SIMULATOR_UDID" \
  --clear-app-data tech.arnav.twofac
```

## Script behavior notes

- Android boot readiness checks `sys.boot_completed` before returning.
- Android app-data clearing uses `adb shell pm clear <package>` on the selected emulator.
- iOS boot uses `xcrun simctl bootstatus <UDID> -b` to wait for full readiness.
- iOS app-data clearing uses `xcrun simctl uninstall <UDID> <bundleId>` to reset the app sandbox.
- Android script reads AVD metadata from `avdmanager list avd` and running serials from `adb devices -l`.
- iOS script reads simulator inventory from `xcrun simctl list --json` and filters to available iOS runtimes/devices.
