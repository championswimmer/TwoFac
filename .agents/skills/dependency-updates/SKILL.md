---
name: dependency-updates
description: Audit repo dependencies by default and only apply library upgrades when explicitly requested.
---

# Skill: Dependency Updates

Use this skill when the user asks to:

- check outdated libraries or dependencies
- prepare an upgrade report
- upgrade Gradle, Kotlin, npm, or CocoaPods dependencies
- understand which dependency surfaces exist in this repo

## Repo dependency surfaces discovered here

- **Gradle / Kotlin Multiplatform**: `sharedLib`, `composeApp`, `cliApp`, `androidApp`, `watchApp`
  - Central catalog: `gradle/libs.versions.toml`
  - Audit mechanism already present in repo: `./gradlew refreshVersions`
- **npm**:
  - `website`
  - `composeApp/src/wasmJsMain/typescript`
- **Apple native wrappers**:
  - `iosApp` currently has Xcode project/workspace files, but no `Podfile`, `Podfile.lock`, `Package.swift`, or `Package.resolved`
  - CocoaPods scripts should remain available for future native additions, but today they will mostly no-op
- **Ignore during normal audits**:
  - `kotlin-js-store/*.yarn.lock` exists without a nearby source `package.json`; treat it as generated/store state, not a first-class package manifest

## Default behavior

**Do not upgrade anything unless the user explicitly asks for upgrades.**

**When upgrades are explicitly requested, prefer the latest stable versions only. Do not move to alpha, beta, RC, or other pre-release versions unless the user explicitly asks for them or approves them after review.**

By default, run the audit/report flow only:

```bash
node .agents/skills/dependency-updates/scripts/dependency-updates.mjs
```

This writes a markdown report into `.agents/plans/dependency-update-reports/`.

## Scripts in this skill

- Aggregator: `.agents/skills/dependency-updates/scripts/dependency-updates.mjs`
- Shared helpers: `.agents/skills/dependency-updates/scripts/dependencyUpdateTools.mjs`
- Gradle/Kotlin audit & upgrade helper: `.agents/skills/dependency-updates/scripts/gradle-updates.mjs`
- npm audit & upgrade helper: `.agents/skills/dependency-updates/scripts/npm-updates.mjs`
- CocoaPods audit & upgrade helper: `.agents/skills/dependency-updates/scripts/cocoapods-updates.mjs`
- Upgrade/runbook reference: `.agents/skills/dependency-updates/UPGRADING.md`

## How to use it

### Audit only

```bash
node .agents/skills/dependency-updates/scripts/dependency-updates.mjs
```

### Explicit upgrade

Default upgrade policy:

- Gradle/Kotlin upgrades should target the latest stable candidate by default
- npm upgrades should prefer the safe `wanted` path by default for routine maintenance
- Do not take alpha/beta/rc/pre-release versions unless the user explicitly asked or approved them
- For Gradle pre-releases specifically, require `--allow-prerelease`

Examples:

```bash
node .agents/skills/dependency-updates/scripts/dependency-updates.mjs upgrade --skip-npm --skip-cocoapods
node .agents/skills/dependency-updates/scripts/dependency-updates.mjs upgrade --skip-gradle --skip-cocoapods --npm-strategy wanted
node .agents/skills/dependency-updates/scripts/dependency-updates.mjs upgrade --skip-gradle --skip-npm --cocoapods-repo-update
```

## Agent workflow

1. Run the default report first unless the user directly asked for upgrades.
2. Read the generated markdown report in `.agents/plans/dependency-update-reports/`.
3. Summarize the main stable candidates and call out any major-version or pre-release jumps.
4. Only if explicitly asked, run the relevant `upgrade` command, keeping to latest stable versions unless the user approved pre-releases.
5. After upgrades, run the validation checklist from `UPGRADING.md`.

## Research basis used for this skill

This skill was based on repo inspection plus web research:

- `refreshVersions` supports `./gradlew refreshVersions` for catalog update discovery
- `npm outdated --json` is the right audit command for direct npm dependencies
- CocoaPods uses `pod outdated` for audit and `pod update` for explicit upgrades

Keep the report-first default even if an ecosystem supports automatic upgrades.
