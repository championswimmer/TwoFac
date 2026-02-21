# Skill: Kotlin Multiplatform Layout

This repository follows standard Kotlin Multiplatform source-set conventions.

## Quick mapping

- `commonMain`: shared production code
- `commonTest`: shared tests
- `<platform>Main`: platform implementations
- `<platform>Test`: platform-specific tests

## Practical reminder

When adding APIs, keep signatures in common code and push only unavoidable platform differences into platform source sets.
