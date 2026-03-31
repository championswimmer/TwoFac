---
name: Code Simplification Plan - Root Scaffolding & Repo Hygiene
status: Completed
progress:
  - "[x] Phase 0: Correct stale metadata and version drift"
  - "[x] Phase 1: Plan system hygiene (numbering/archive)"
  - "[x] Phase 2: Build config noise reduction"
  - "[x] Phase 3: Asset/source-of-truth cleanup"
  - "[x] Phase 4: CI/workflow consistency"
---

# Code Simplification Plan - Root Scaffolding & Repo Hygiene

## Scope
- Repository root configuration and docs:
  - `README.md`, `.github/*`, root Gradle files, `.agents/plans/*`, shared assets.
- Goal: reduce non-functional clutter and keep contributor guidance trustworthy.

## High-Signal Findings (evidence)
1. **Version metadata drift**
   - `README.md` Kotlin badge shows `2.2.0` while catalog is `2.3.10`.
   - `.github/copilot-instructions.md` says Gradle `8.14.4`; wrapper points to `9.1.0`.
2. **Plan management bloat**
   - `.agents/plans/` has many completed plans in active root.
   - number collision exists across multiple `22-*` plan files.
3. **Version catalog noise**
   - `gradle/libs.versions.toml` contains large volume of historical `##⬆=` suggestion lines.
4. **Deprecated gradle property**
   - `gradle.properties`: `kotlin.native.enableKlibsCrossCompilation=false` (obsolete on modern Kotlin).
5. **Duplicate assets across docs/logo/website public**
   - logo and website public assets are byte-identical duplicates in multiple locations.

## Simplification Roadmap

### Phase 0: Correct stale metadata and version drift
- [ ] Update README badge versions from actual catalog values.
- [ ] Update `.github/copilot-instructions.md` to current wrapper/tooling versions.
- [ ] Replace duplicated module descriptions with pointer to `AGENTS.md` as source-of-truth.

### Phase 1: Plan system hygiene (numbering/archive)
- [ ] Resolve numbering collisions (`22-*`) via renumbering.
- [ ] Create `.agents/plans/archive/` and move completed plans there.
- [ ] Keep active roadmap-only plans in root plan directory.

### Phase 2: Build config noise reduction
- [ ] Remove obsolete native gradle property.
- [ ] Decide policy for `refreshVersions` hints:
  - keep comments only temporarily, or
  - auto-prune before commit.
- [ ] Optionally evaluate migration away from refreshVersions scaffolding if not needed.

### Phase 3: Asset/source-of-truth cleanup
- [ ] Designate one canonical logo source directory.
- [ ] Replace duplicated tracked copies with build-time sync step where needed.
- [ ] Keep generated asset outputs ignored from git where possible.

### Phase 4: CI/workflow consistency
- [ ] Standardize GitHub action versions (`actions/checkout`, setup actions).
- [ ] Document intentionally manual workflows (if triggers remain disabled).

## Success Criteria
- Contributor docs reflect actual versions/toolchain.
- Plan directory is navigable and uniquely numbered.
- Build config files are cleaner with fewer stale/deprecated settings.
- Shared assets have one authoritative source path.
