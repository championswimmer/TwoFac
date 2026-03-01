---
name: Module Routing
description: Guidelines for where to place different types of code in a Kotlin Multiplatform project.
---
# Skill: Module Routing

Use this when deciding where a change belongs.

## Rules

1. Put reusable OTP/business logic in `sharedLib`.
2. Put cross-platform UI in `composeApp/src/commonMain`.
3. Put platform-specific shared code (DI modules, biometrics, wear sync, storage) in `composeApp/src/<platform>Main`.
4. Put Android app entry point (Application, Activity, manifest, icons) in `androidApp`.
5. Put terminal UX and command wiring in `cliApp`.
6. Put Wear-only UI and Android watch integration in `watchApp`.
