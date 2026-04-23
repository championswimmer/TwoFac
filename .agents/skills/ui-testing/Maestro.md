# Maestro Mobile Testing

## Overview

Use Maestro for full black-box user journeys on Android and iOS.

For unit and integration tests, use the module-specific testing practices instead.

For test-only passkey/password entry, always use `123456`.

## Setup & Prerequisites

- Maestro CLI must be installed: `curl -Ls "https://get.maestro.mobile.dev" | bash` (pinned internally to v2.3.0)
- Java JDK 21+
- Android: `adb` and `emulator` on `$PATH`
- iOS: Xcode 16+ and Command Line Tools

## Running Tests Locally

The repository provides wrapper scripts that boot a device, build/install the app, and run the flows.

### Android

Use a local emulator or a connected USB device.

```bash
# Run the smoke suite on an interactive emulator selection
.maestro/scripts/run-android-local.sh --suite smoke

# Run a specific suite on a specific AVD automatically
.maestro/scripts/run-android-local.sh --suite regression --avd Pixel_9

# Run tests on a connected physical USB device
.maestro/scripts/run-android-local.sh --suite smoke --usb
```

### iOS

Use a local simulator or a connected USB device.

```bash
# Run the smoke suite on an interactive simulator selection
.maestro/scripts/run-ios-local.sh --suite smoke

# Run tests on a specific simulator UDID
.maestro/scripts/run-ios-local.sh --suite smoke --udid E3D48ABA-2D66-4980-8EFB-E3B880AA8A3D

# Run tests on a connected physical USB device
.maestro/scripts/run-ios-local.sh --suite smoke --usb
```

## Writing Tests

Test flows live in `.maestro/`.

- **Flows:** `.maestro/flows/`
- **Subflows:** `.maestro/subflows/`
- **Structure:** group by purpose (`smoke`, `regression`), then by `shared`, `android`, or `ios`

### Tagging Strategy

Include a YAML header with `appId` and `tags`:

```yaml
appId: ${APP_ID} # Injected by the wrapper scripts.
tags:
  - smoke
  - platform-android
  - auth
---
- runFlow: ../../../subflows/shared/launch-app.yaml
- assertVisible:
    id: "login-button"
```

### Passkey / Password Input

The shared smoke flow uses the fixed test passkey `123456`:

```yaml
- inputText: "123456"
```

Keep that value for local mobile UI testing unless the flows are intentionally being rewritten.

## Troubleshooting & Artifacts

- **Outputs:** logs, JUnit reports, and failure screenshots go to `.maestro/outputs/`
- **`APP_ID` undefined:** run via the wrapper scripts so the environment variable is injected
- **Selectors failing:** use `maestro hierarchy`; for Compose UIs, prefer stable IDs or `testTags`

## Governance & Rollout

- **Selector robustness:** prefer IDs and semantic text over brittle indexes
- **Subflow reuse:** reuse shared launch/auth subflows instead of duplicating them
- **No flaky sleeps:** avoid `sleep` or long waits unless absolutely necessary
- **Quality gate:** the `smoke` suite must pass on PRs
- **Triage:** if `smoke` fails on CI, inspect the `maestro-android-outputs` or `maestro-ios-outputs` artifacts
- **Upgrade policy:** validate Maestro CLI upgrades locally with the full `regression` suite before merging
