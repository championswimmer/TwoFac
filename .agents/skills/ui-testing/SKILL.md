---
name: ui-testing
description: How to run, write, and debug local UI tests for Android, iOS, and browser builds.
---

# UI Testing

## Overview

This repository uses:

- [Maestro](https://maestro.mobile.dev) for local end-to-end testing of the Android and iOS apps
- [Playwright](https://playwright.dev) for local browser testing of the Compose Wasm app

**Important Policy:** We operate with Maestro's open-source CLI only. Do not use `maestro cloud` or any paid Maestro features.

For test-only passkey/password entry, always use `123456`.

## Guides

- [Maestro mobile testing](./Maestro.md)
- [Playwright browser testing](./Playwright.md)

## Related Skills

- [Simulators and Emulators](../simulators-emulators/SKILL.md)
- [Gradle Build Commands](../gradle-build/SKILL.md)
- [Integration Plan File](../../plans/16-maestro-local-mobile-testing-plan.md)
