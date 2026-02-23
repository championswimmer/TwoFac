---
name: Gradle Verify Commands
description: Use Gradle commands to quickly validate the impact of changes in the codebase.
---
# Skill: Gradle Verify Commands

Use these commands to validate likely impact quickly.

## Common checks

- Repository baseline: `./gradlew check`
- Shared library tests: `./gradlew :sharedLib:test`
- CLI tests: `./gradlew :cliApp:test`
- Compose tests: `./gradlew :composeApp:test`

## Guidance

- Prefer module-scoped tasks for faster feedback.
- Run full checks only when change scope spans multiple modules.
