---
name: Code Simplification Program Overview
status: Planned
progress:
  - "[ ] Wave 1: P0/P1 safety and artifact cleanup"
  - "[ ] Wave 2: module-level abstraction reduction"
  - "[ ] Wave 3: cross-module contract alignment"
  - "[ ] Wave 4: docs and repo hygiene finalization"
---

# 27 - Code Simplification Program Overview

This folder contains module-by-module simplification plans produced from a parallel audit across the codebase.

## Plans
1. `01-simplify-cli.md`
2. `02-simplify-sharedlib.md`
3. `03-simplify-compose.md`
4. `04-simplify-android.md`
5. `05-simplify-ios.md`
6. `06-simplify-watch.md`
7. `07-simplify-root-scaffolding.md`
8. `08-simplify-website.md`

## Recommended execution order
1. **P0/P1 cleanup first**
   - remove tracked artifacts (`composeApp/bin`, tracked `.dylib`), dead iOS sync coordinator, CLI root-command dispatch bug.
2. **Core library correctness second**
   - sharedLib OTP URI/HOTP semantics and duplicate mapping/JSON cleanup.
3. **Platform simplification third**
   - compose/android/ios/watch abstraction and wiring reductions.
4. **Repo hygiene last**
   - plan archival, stale docs/version drift, asset source-of-truth cleanup.

## Global guardrails
- Keep behavior and data formats backward-compatible unless migration is explicitly documented.
- Run module-targeted test/build checks after each wave.
- Prefer deleting dead abstractions over renaming them unless API compatibility requires transitional wrappers.
