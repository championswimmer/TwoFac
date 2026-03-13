---
name: Local UI Testing with Maestro
description: How to run, write, and debug Maestro E2E tests for Android and iOS using local simulators/devices.
---

# Maestro UI Testing

## Overview

This repository uses [Maestro](https://maestro.mobile.dev) for local End-to-End (E2E) UI testing across Android and iOS. 

**Important Policy:** We operate strictly with Maestro's open-source CLI. **Do not use `maestro cloud` or any paid Maestro features.** All testing should run locally or on our local-hosted CI runners without requiring API keys.

For unit and integration tests, please refer to the specific module's testing practices. Maestro is exclusively used for full black-box user journeys.

## Setup & Prerequisites

*   Maestro CLI must be installed: `curl -Ls "https://get.maestro.mobile.dev" | bash` (Pinned internally to v2.3.0).
*   Java JDK 21+ installed.
*   Android: SDK tools (`adb`) and `emulator` must be available in `$PATH`.
*   iOS: Xcode 16+ and Command Line Tools must be installed.

## Running Tests Locally

We provide wrapper scripts to seamlessly boot a device, build/install the app, and run the flows.

### Android

You can use a local emulator (interactive picker) or a connected USB device.

```bash
# Run the smoke suite on an interactive emulator selection
.maestro/scripts/run-android-local.sh --suite smoke

# Run a specific suite on a specific AVD automatically
.maestro/scripts/run-android-local.sh --suite regression --avd Pixel_9

# Run tests on a connected physical USB device
.maestro/scripts/run-android-local.sh --suite smoke --usb
```

### iOS

You can use a local simulator (interactive picker) or a connected USB device.

```bash
# Run the smoke suite on an interactive simulator selection
.maestro/scripts/run-ios-local.sh --suite smoke

# Run tests on a specific simulator UDID
.maestro/scripts/run-ios-local.sh --suite smoke --udid E3D48ABA-2D66-4980-8EFB-E3B880AA8A3D

# Run tests on a connected physical USB device
.maestro/scripts/run-ios-local.sh --suite smoke --usb
```

## Writing Tests

Test flows are located in the `.maestro/` directory. 
*   **Flows:** Full user journeys located in `.maestro/flows/`
*   **Subflows:** Reusable, composable components located in `.maestro/subflows/`
*   **Structure:** We group by purpose (`smoke`, `regression`), then by `shared`, `android`, or `ios` to maximize reuse while handling platform-specific quirks.

### Tagging Strategy

Include a YAML header with the `appId` and `tags`:

```yaml
appId: ${APP_ID} # Required! Inherited from the shell wrapper scripts.
tags:
  - smoke            # Quick tests for PRs
  - platform-android # Target platform
  - auth             # Domain/Feature
---
- runFlow: ../../../subflows/shared/launch-app.yaml
- assertVisible:
    id: "login-button"
```

## Troubleshooting & Artifacts

*   **Test Outputs:** When tests run via our scripts or CI, logs, reports (JUnit), and screenshots for failures are saved to `.maestro/outputs/`. Check here first for debugging.
*   **AppId Undefined:** Ensure you are running flows using the provided wrapper scripts, which inject the `APP_ID` environment variable.
*   **Selectors Failing:** Use `maestro hierarchy` to inspect the UI tree. Note that Jetpack Compose selectors may differ from standard Android Views. Consider adding `testTags` in the Compose code.

## Governance & Rollout

*   **Flow Authoring Rules:**
    *   **Selector Robustness:** Prefer IDs and semantic text over brittle index-based locators.
    *   **Subflow Reuse:** Do not duplicate launch/auth flows. Use the `subflows/` library.
    *   **No Flaky Sleeps:** Avoid hardcoded `extendedWaitUntil` or `sleep` commands unless absolutely necessary (e.g., waiting for an external system).
*   **Quality Gate:** The `smoke` suite must pass on PRs before they are merged. 
*   **Triage Workflow:** Flaky tests should be tagged `wip` and investigated. If a test fails in `smoke` on CI, review the `maestro-android-outputs` or `maestro-ios-outputs` artifacts.
*   **Release-Readiness Checklist:** Ensure `smoke` is green on Android and iOS and verify artifacts are captured upon any failure.
*   **Upgrade Policy:** Maestro CLI upgrades (from v2.3.0) should be validated locally with the full `regression` suite before merging into the main CI and `UI_TESTING_CONSTRAINTS.md`.

## Related Skills

*   [Simulators and Emulators](../simulators-emulators/SKILL.md)
*   [Gradle Build Commands](../gradle-build/SKILL.md)
*   [Integration Plan File](../../plans/16-maestro-local-mobile-testing-plan.md)
