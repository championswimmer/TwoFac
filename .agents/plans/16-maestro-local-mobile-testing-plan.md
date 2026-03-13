---
name: Maestro Local Mobile UI Testing Integration Plan
status: Planned
progress:
  - "[ ] Phase 0 - Confirm constraints, tooling baseline, and support matrix"
  - "[ ] Phase 1 - Establish repository structure for Maestro YAML flows"
  - "[ ] Phase 2 - Add Android local execution path (emulator + USB device)"
  - "[ ] Phase 3 - Add iOS local execution path (simulator-first)"
  - "[ ] Phase 4 - Build shared flow architecture, tags, and suite segmentation"
  - "[ ] Phase 5 - Integrate local-only Maestro runs into CI workflows"
  - "[ ] Phase 6 - Add .agents/skills/ui-testing skill for Maestro usage"
  - "[ ] Phase 7 - Stabilization, governance, and rollout"
---

# Maestro Local Mobile UI Testing Integration Plan

## Objective

Integrate Maestro as this repository's local UI E2E test tool for Android and iOS without using Maestro Cloud or any paid offering.  
The end state should support:

1. Android emulator runs
2. Android real-device runs (USB-connected)
3. iOS simulator runs
4. A clear repository layout for Maestro YAML flows
5. A dedicated `ui-testing` skill under `.agents/skills/` for contributor guidance

## Non-goals

1. No Maestro Cloud integration
2. No paid Maestro offerings
3. No migration/replacement of existing unit/integration test suites (`sharedLib`, `composeApp`, etc.)

## Research summary (web + repo)

1. Maestro flows are YAML and run via `maestro test ...` (CLI docs, command reference).
2. Test discovery is root-only by default when running a folder; nested flow discovery requires `flows` globs in `config.yaml`.
3. Tags are supported via flow headers and can be filtered with `--include-tags` / `--exclude-tags`.
4. Output artifacts and CI-friendly reports are available via `--test-output-dir`, `--format junit|html`, and `--output`.
5. Supported platforms docs currently describe Android emulators + physical devices and iOS simulators.
6. Repository already has reusable simulator/emulator picker scripts:
   - `.agents/skills/simulators-emulators/scripts/android-emulator-picker.mjs`
   - `.agents/skills/simulators-emulators/scripts/ios-simulator-picker.mjs`
7. App identifiers relevant for Maestro:
   - Android app id: `tech.arnav.twofac` (`androidApp/build.gradle.kts`)
   - iOS bundle id baseline to validate in implementation: `tech.arnav.twofac.app` (`iosApp` project settings)

## Proposed repository structure for Maestro tests

```text
.maestro/
  config.yaml
  flows/
    smoke/
      shared/
      android/
      ios/
    regression/
      shared/
      android/
      ios/
    wip/
  subflows/
    shared/
    android/
    ios/
  fixtures/
    testdata/
  outputs/               # gitignored run artifacts (optional local default)
  scripts/
    run-android-local.sh
    run-ios-local.sh
    run-smoke-matrix.sh
```

### Structure rationale

1. Keep `.maestro/` at repo root for predictable discovery and onboarding.
2. Separate `flows/` (entry suites) from `subflows/` (reusable components).
3. Use `shared/android/ios` split to maximize reuse while still handling platform-specific selectors.
4. Use tags + folder segmentation (`smoke`, `regression`, `wip`) for CI and local selectivity.

## `config.yaml` direction

Initial direction for `.maestro/config.yaml`:

1. Use recursive flow inclusion to support nested suites (`flows/**`).
2. Define `testOutputDir` under repo (for reproducible local/CI artifacts).
3. Keep app id injected via env var (`${APP_ID}`) for cross-platform reuse.
4. Use optional global include/exclude tags for branch-specific pipelines.

## Phase-by-phase roadmap

## Phase 0 - Confirm constraints, tooling baseline, and support matrix

1. Verify Maestro CLI install path(s) and pinned version strategy for contributors and CI.
2. Confirm Java requirement and local prerequisites (Android SDK/ADB, Xcode CLI tools).
3. Validate current platform support assumptions in docs and capture in `Known Constraints`:
   - Android: emulator + USB physical device
   - iOS: simulator path is primary and required
4. Lock explicit policy: **no Cloud commands** (`maestro cloud`) in scripts/workflows/docs.

### Deliverables

1. Constraint matrix and prerequisites section in plan-linked docs.
2. A "local-only" policy checklist for code reviews.

## Phase 1 - Establish repository structure for Maestro YAML flows

1. Create `.maestro/` tree (flows, subflows, fixtures, scripts).
2. Add `.maestro/config.yaml` with recursive flow discovery and artifact path policy.
3. Add initial baseline flow files:
   - `flows/smoke/shared/app-launch.yaml`
   - `flows/smoke/android/basic-navigation.yaml`
   - `flows/smoke/ios/basic-navigation.yaml`
4. Add shared subflows:
   - launch/reset helpers
   - unlock/session helpers
   - reusable assertions
5. Add `.gitignore` updates for Maestro outputs (if needed).

### Deliverables

1. Working folder layout and starter YAML flow set.
2. Documented naming conventions for flows/subflows/tags.

## Phase 2 - Add Android local execution path (emulator + USB device)

1. Reuse existing emulator picker script flow:
   - `eval "$(node .agents/skills/simulators-emulators/scripts/android-emulator-picker.mjs --boot --shell)"`
2. Build/install app using existing module commands:
   - `ANDROID_SERIAL="$ANDROID_SERIAL" ./gradlew :androidApp:installDebug`
3. Add scriptized Maestro commands for Android:
   - target explicit device serial
   - pass `APP_ID=tech.arnav.twofac`
   - optionally run tag-filtered suites (`smoke`, `regression`)
4. Add USB-device mode:
   - discover via `adb devices -l`
   - explicit serial targeting with `--device`
5. Add deterministic reset behavior:
   - launch with `clearState` where appropriate
   - isolate data-changing tests

### Deliverables

1. `run-android-local.sh` script + usage docs.
2. Smoke suite passing on emulator and at least one USB-device run path.

## Phase 3 - Add iOS local execution path (simulator-first)

1. Reuse existing iOS picker script flow:
   - `eval "$(node .agents/skills/simulators-emulators/scripts/ios-simulator-picker.mjs --boot --shell)"`
2. Build and launch iOS app for simulator using existing project workflow (`xcodebuild` path from current skills/docs).
3. Add scriptized Maestro commands for iOS simulator:
   - explicit UDID targeting with `--device`
   - pass platform app id env var (`APP_ID`).
4. Normalize iOS-specific selectors/permission handling via shared subflows.
5. Track iOS real-device support status in a dedicated constraints note and revisit cadence.

### Deliverables

1. `run-ios-local.sh` script + usage docs.
2. Smoke suite passing on booted simulator.
3. Explicit issue/decision log entry for physical iOS status.

## Phase 4 - Build shared flow architecture, tags, and suite segmentation

1. Introduce tagging strategy in YAML headers:
   - `smoke`, `regression`, `auth`, `backup`, `settings`, `platform-android`, `platform-ios`, `wip`
2. Define journey-first suites for this app:
   - account onboarding/import
   - unlock + OTP visibility
   - settings and backup entry points
3. Keep platform-specific divergence in thin wrappers; maximize shared subflows.
4. Define execution profiles:
   - quick local smoke
   - local full regression
   - CI PR gate (smoke only)
   - nightly/local runner full regression

### Deliverables

1. Tag taxonomy doc + examples.
2. Suite matrix mapping flows to user journeys and platforms.

## Phase 5 - Integrate local-only Maestro runs into CI workflows

1. Add/extend GitHub Actions workflow(s) to run local Maestro on hosted runners:
   - Android job on Ubuntu/macOS with emulator
   - iOS job on macOS with simulator
2. Keep CI strictly local execution:
   - no API keys
   - no `maestro cloud` commands
3. Publish artifacts for failures:
   - `--test-output-dir`
   - JUnit and/or HTML reports (`--format`, `--output`)
4. Make smoke suite a required signal for PRs once stabilized.
5. Add optional shard strategy for local multi-device runs where available.

### Deliverables

1. CI workflow updates with artifact publishing and deterministic device boot/install/test steps.
2. CI README snippet for contributors.

## Phase 6 - Add `.agents/skills/ui-testing` skill for Maestro usage

1. Create `.agents/skills/ui-testing/SKILL.md` with repository-specific guidance.
2. Skill sections should include:
   - when to use Maestro vs existing tests
   - local Android flow (emulator + USB)
   - local iOS simulator flow
   - test folder conventions and YAML patterns
   - tag strategy and command examples
   - troubleshooting and artifact capture
   - local-only policy (no cloud/paid usage)
3. Cross-link this skill to:
   - `.agents/skills/simulators-emulators/SKILL.md`
   - `.agents/skills/gradle-build/SKILL.md`
   - this plan file

### Deliverables

1. New `ui-testing` skill file ready for team adoption.
2. Consistent command snippets aligned with repo scripts.

## Phase 7 - Stabilization, governance, and rollout

1. Add contribution rules for flow authoring and review:
   - selector robustness
   - subflow reuse
   - no flaky sleeps unless justified
2. Define owner rotation for flaky test triage.
3. Add baseline quality gate:
   - smoke must pass before merge (after burn-in window)
4. Add release-readiness checklist:
   - Android + iOS smoke green
   - artifacts preserved for any failure
5. Document upgrade policy for Maestro CLI versions.

### Deliverables

1. Governance note + triage workflow.
2. Stable PR gate criteria.

## Risks and mitigations

1. **Platform capability mismatch (especially iOS real-device expectations)**  
   Mitigation: keep simulator path as hard requirement, track physical-device status explicitly, and avoid hidden assumptions.

2. **Flaky selectors on dynamic Compose screens**  
   Mitigation: shared selector conventions, explicit wait/assert subflows, and burn-in before enforcing hard CI gates.

3. **CI runtime/boot instability**  
   Mitigation: deterministic boot scripts, readiness probes, retries for boot/install stages only, and artifact-first debugging.

4. **Flow sprawl over time**  
   Mitigation: folder taxonomy + tag policy + mandatory reuse of subflows.

## Success criteria

1. Repository has a standard `.maestro/` structure with maintainable YAML flow organization.
2. Contributors can run smoke flows locally on Android emulator and iOS simulator with one command path each.
3. Android USB real-device path is documented and validated.
4. CI executes local Maestro smoke runs for Android and iOS without cloud dependencies.
5. `.agents/skills/ui-testing/SKILL.md` exists and is referenced in contributor workflow.
6. No Maestro Cloud/API key dependency exists in committed scripts or workflows.

## Source links used for planning research

1. `https://docs.maestro.dev/maestro-cli/maestro-cli-commands-and-options`
2. `https://docs.maestro.dev/maestro-cli`
3. `https://docs.maestro.dev/maestro-flows/workspace-management/workspace-management-overview`
4. `https://docs.maestro.dev/maestro-flows/workspace-management/test-discovery-and-tags`
5. `https://docs.maestro.dev/reference/workspace-configuration`
6. `https://docs.maestro.dev/cli/test-output-directory`
7. `https://docs.maestro.dev/getting-started/build-and-install-your-app/android`
8. `https://docs.maestro.dev/get-started/supported-platform/ios`
9. `https://docs.maestro.dev/advanced/specify-a-device`
10. `https://docs.maestro.dev/getting-started/installing-maestro/windows`
11. `https://github.com/mobile-dev-inc/maestro`
