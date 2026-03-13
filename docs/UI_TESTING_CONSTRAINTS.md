# Maestro UI Testing Constraints & Prerequisites

This document outlines the constraints, tooling baseline, and support matrix for using Maestro locally in the TwoFac repository.

## Known Constraints & Support Matrix

*   **Maestro Version:** Pinned to `2.3.0` for all local and CI runs.
*   **Android Support:** Emulator or USB physical device. At least one must be used for smoke validation.
*   **iOS Support:** Simulator or USB physical device. At least one must be used for smoke validation.
*   **Java Requirement:** JDK 21+ (aligns with project requirements).
*   **Android Prerequisites:** Android SDK and `adb` available in `$PATH`.
*   **iOS Prerequisites:** Xcode CLI tools installed and configured.

## Local-Only Policy

*   **Strict No-Cloud Policy:** The use of `maestro cloud` commands is strictly prohibited.
*   **API Keys:** No API keys or external service integrations should be used for UI testing.
*   **Code Review Checklist:**
    *   [ ] Verify no `maestro cloud` commands are present in new scripts or workflows.
    *   [ ] Verify tests execute successfully on local emulator/simulator.
    *   [ ] Ensure no paid Maestro features are being utilized.
