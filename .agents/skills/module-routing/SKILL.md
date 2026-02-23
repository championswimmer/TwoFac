---
name: Module Routing
description: Guidelines for where to place different types of code in a Kotlin Multiplatform project.
---
# Skill: Module Routing

Use this when deciding where a change belongs.

## Rules

1. Put reusable OTP/business logic in `sharedLib`.
2. Put cross-platform UI in `composeApp/src/commonMain`.
3. Put platform-specific UI/entrypoints in each platform source set.
4. Put terminal UX and command wiring in `cliApp`.
5. Put Wear-only UI and Android watch integration in `watchApp`.
