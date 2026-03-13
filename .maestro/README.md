# Maestro Testing Strategy & Taxonomy

## Tag Taxonomy

Tags are defined in the header of each Flow file. They allow for selective execution based on the environment, purpose, or platform.

### Environment / Purpose Tags
*   `smoke`: Quick, high-signal tests intended to verify critical paths. Run on every PR.
*   `regression`: Comprehensive tests covering edge cases and full user journeys. Run on demand or nightly.
*   `wip`: Work-in-progress tests that should be ignored by CI.

### Feature / Domain Tags
*   `auth`: Tests involving biometric or passkey authentication, unlock flows.
*   `backup`: Tests covering local or encrypted backup import/export workflows.
*   `settings`: Tests covering configuration, UI preferences, and app settings.

### Platform Tags
*   `shared`: Cross-platform subflows or assertions.
*   `platform-android`: Android-specific flows or overrides.
*   `platform-ios`: iOS-specific flows or overrides.

**Examples:**
```yaml
appId: ${APP_ID}
tags:
  - smoke
  - auth
  - platform-android
```

## Suite Matrix Mapping

This matrix maps Maestro flows to the core user journeys and platforms supported by TwoFac.

| User Journey | Suite | Subflows / Flows | Platforms |
| :--- | :--- | :--- | :--- |
| **Account Onboarding / Import** | `regression`, `backup` | Initial launch -> scan QR / import JSON | Android, iOS |
| **Unlock + OTP Visibility** | `smoke`, `auth` | App Launch -> unlock prompt -> verify OTP code rendering | Android, iOS |
| **Passkey Smoke Journey** | `smoke`, `auth` | Register fixed test passkey (`123456`) -> authenticate | Android, iOS |
| **Settings & Backup Entry** | `regression`, `settings` | Launch -> open settings -> tap backup export -> verify UI | Android, iOS |

## Execution Profiles

*   **Quick Local Smoke:** `maestro test --include-tags=smoke .maestro/`
*   **Local Full Regression:** `maestro test --include-tags=regression .maestro/`
*   **CI PR Gate:** Executed via `run-android-local.sh` and `run-ios-local.sh` with `--suite smoke`.
*   **Nightly Full Regression:** Run via scheduled CI or manually with `--suite regression`.

## Continuous Integration (CI)

Local Maestro runs are integrated into the GitHub Actions pipeline (`.github/workflows/maestro-ui-tests.yml`).

*   **No Cloud Dependency:** CI tests are executed completely locally on GitHub-hosted runners (macOS for both Android and iOS) without requiring any Maestro Cloud API keys.
*   **Artifacts:** In the event of a test failure, screenshots and execution logs are uploaded as artifacts (`maestro-android-outputs`, `maestro-ios-outputs`) to the GitHub Actions run summary. You can download these to inspect the test run step-by-step.
*   **Pull Requests:** The `smoke` suite is required to pass on both Android and iOS before merging.
