---
name: Simulators and Emulators
description: How to list, pick, and boot Android emulators and iOS simulators for local app runs.
---
# Skill: Simulators and Emulators

Use this skill to discover local Android emulators and iOS simulators, pick a target interactively, and boot it before running the app.

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

### Run app on selected emulator

Use shell output to set `ANDROID_SERIAL`:

```bash
eval "$(node .agents/skills/simulators-emulators/scripts/android-emulator-picker.mjs --boot --shell)"
ANDROID_SERIAL="$ANDROID_SERIAL" ./gradlew :androidApp:installDebug
adb -s "$ANDROID_SERIAL" shell am start -n tech.arnav.twofac/.MainActivity
adb -s "$ANDROID_SERIAL" shell dumpsys activity activities | rg 'Resumed: ActivityRecord|mCurrentFocus|mFocusedApp'
```

When multiple emulators/devices are connected, always pass `ANDROID_SERIAL` (or `adb -s <serial>`) to avoid ambiguous target errors.

## iOS simulators

### List and pick

- List with script: `node .agents/skills/simulators-emulators/scripts/ios-simulator-picker.mjs --list`
- Interactive pick: `node .agents/skills/simulators-emulators/scripts/ios-simulator-picker.mjs`
- Pick and boot: `node .agents/skills/simulators-emulators/scripts/ios-simulator-picker.mjs --boot`
- Pick by UDID: `node .agents/skills/simulators-emulators/scripts/ios-simulator-picker.mjs --udid <UDID> --boot`

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

## Script behavior notes

- Android boot readiness checks `sys.boot_completed` before returning.
- iOS boot uses `xcrun simctl bootstatus <UDID> -b` to wait for full readiness.
- Android script reads AVD metadata from `avdmanager list avd` and running serials from `adb devices -l`.
- iOS script reads simulator inventory from `xcrun simctl list --json` and filters to available iOS runtimes/devices.
