---
name: CLI Dual Mode (Interactive TUI + Non-Interactive CLI) Plan
status: In Progress
progress:
  - "[x] Phase 0 - Lock command IA, UX scope, and migration strategy"
  - "[x] Phase 1 - Refactor command tree into explicit one-shot command groups"
  - "[x] Phase 2 - Implement root mode switch (interactive TUI vs non-interactive help)"
  - "[x] Phase 3 - Build TUI runtime (event loop, renderer, screen navigation, animation)"
  - "[x] Phase 4 - Implement HomeScreen (live OTP list + filters + countdown)"
  - "[ ] Phase 5 - Implement AccountScreen (details + add/remove flows)"
  - "[ ] Phase 6 - Implement SettingsScreen (storage backend + backup provider surfaces)"
  - "[ ] Phase 7 - Wire storage/backup subcommands under storage namespace"
  - "[ ] Phase 8 - Add tests for parsing, mode detection, and TUI state reducers"
  - "[ ] Phase 9 - Rollout, docs, and compatibility/deprecation messaging"
---

# CLI Dual Mode (Interactive TUI + Non-Interactive CLI) Plan

## Goal
Add **two operation modes** to `cliApp`:

1. **Non-interactive, one-shot CLI mode** (full command/subcommand/option invocation)
2. **Interactive TUI mode** when user runs just `2fac` in an interactive terminal

If user runs `2fac` with **no subcommand** and terminal is **not interactive**, print help and exit.

## Requested UX and Command IA (Target)

```text
2fac
  display
    --issuer <issuer>
    --account <account>
  info
  accounts
    add
    remove
  storage
    --use-backend <backend>    # future-facing backend selection
    clean | delete | reinitialize
    backup
      ... provider/list/auth flows
```

All actions above must remain possible as one-shot commands.

---

## Research Summary (Clikt + Mordant)

### Clikt findings applied to this plan
1. Parent commands with children do not run by default unless a child is invoked.
2. `invokeWithoutSubcommand = true` allows root command `run()` to execute without child command.
3. `currentContext.invokedSubcommand` / `invokedSubcommands` can be checked to detect whether a child command is being executed.
4. `printHelpOnEmptyArgs = true` is available, but for this feature we need custom root behavior (TUI vs help), so explicit branching in root `run()` is preferred.
5. Clikt testing API (`command.test(...)`) supports parser and prompt-flow validation for non-interactive command behavior.

### Mordant findings applied to this plan
1. `Terminal.animation { ... }` / `Terminal.textAnimation { ... }` are frame-based; rendering happens only on `update(...)` calls.
2. `Animation` does **not** auto-run; caller drives updates and timing.
3. Mordant animation output is suppressed on non-interactive terminals; this aligns with the root-mode split.
4. Interactivity is exposed via terminal info (`inputInteractive`, `outputInteractive`, and combined interactive semantics).
5. For TUI input handling, Mordant provides raw input APIs:
   - `receiveKeyEvents { ... }`
   - `enterRawMode(...)`
   - `interactiveSelectList(...)` (useful for quick menu flows)
6. Progress/animated widgets can be run with coroutine animators or manual refresh loops; for app-like screen rendering, a unified animation/update loop is preferable.

---

## Architectural Plan

## 1) Root bootstrap and mode switch

### Files
- `cliApp/src/commonMain/kotlin/tech/arnav/twofac/cli/Main.kt`
- New: `cliApp/src/commonMain/kotlin/tech/arnav/twofac/cli/runtime/CliModeResolver.kt`

### Plan
1. Keep root as `MainCommand : CliktCommand` with `invokeWithoutSubcommand = true`.
2. In `run()`, if a subcommand is invoked, return immediately.
3. Else compute `isInteractiveTerminal` from Clikt/Mordant terminal info.
4. Branch behavior:
   - interactive -> launch TUI app
   - non-interactive -> print root help and exit cleanly

### Notes
- Introduce small abstraction (`CliModeResolver`) for deterministic tests.
- Keep all one-shot subcommands available regardless of interactivity.

---

## 2) Command tree refactor (one-shot mode)

### Current state
- Root subcommands currently include: `display`, `add`, `info`, `backup`, `storage`.

### Target state
- Root subcommands become: `display`, `info`, `accounts`, `storage`.
- Move `add` under `accounts add`.
- Introduce `accounts remove`.
- Move existing backup workflows under `storage backup ...`.

### Files (expected)
- `cliApp/src/commonMain/kotlin/tech/arnav/twofac/cli/commands/Main` (existing `Main.kt` wiring)
- New: `commands/AccountsCommand.kt`
- Refactor: `commands/AddCommand.kt` -> `AccountsAddCommand` (or move into accounts file)
- New: `commands/RemoveCommand.kt` (or `AccountsRemoveCommand`)
- Refactor: `commands/BackupCommand.kt` under storage hierarchy
- Refactor: `commands/StorageCommand.kt` to include clean/delete/reinitialize + backup group

### Compatibility strategy
- Option A (reject): keep aliases for one release (`add` and `backup` as hidden/deprecated wrappers).
- Option B (confirmed): hard cutover (simpler, potentially disruptive).

---

## 3) TUI runtime design (screen-driven)

### New package
`cliApp/src/commonMain/kotlin/tech/arnav/twofac/cli/tui/`

### Core building blocks
1. `TuiApp.kt`
   - entrypoint for interactive mode
   - owns terminal, main loop, coroutine scope
2. `TuiNavigator.kt`
   - stack/state navigation: Home -> Account -> Settings
3. `TuiScreen` interface
   - `render(state): Widget`
   - `onKey(event): Action`
4. `TuiRenderLoop.kt`
   - wraps `terminal.animation<AppUiState> { ... }`
   - serializes updates
   - ticks every second for OTP countdown refresh

### State model / ViewModels
- `AppViewModel` (global): unlock state, account cache, current screen
- `HomeViewModel`: account list, issuer/account filter, selected row
- `AccountViewModel`: selected account details + actions
- `SettingsViewModel`: storage backend flag + backup provider state

Use `StateFlow`/immutable state data classes to keep updates testable.

---

## 4) Screen scope and behavior

## HomeScreen
- Default TUI landing screen.
- Shows OTP list similar to GUI account list semantics.
- Live countdown refresh every second.
- Key actions:
  - up/down select account
  - enter open account screen
  - `/` or `f` filter by issuer/account
  - `s` settings
  - `q` quit

## AccountScreen
- Focused account details.
- Actions: remove account, back navigation.
- Optional: show issuer metadata and OTP timing detail.

## SettingsScreen
- Info + storage/backup settings surface.
- Include future-facing backend selector (`--use-backend` equivalent state).
- Backup provider status area (list/add/remove/auth placeholders if backend not yet implemented).

---

## 5) Animation strategy (Mordant-specific)

1. Use `Terminal.animation<AppUiState>` for full-screen-like redraws.
2. Drive updates from:
   - key events (input)
   - 1-second ticker (OTP and countdown freshness)
   - async operation results (unlock/add/remove/backup)
3. Ensure update calls are serialized (single event loop coroutine).
4. Avoid interleaving arbitrary `println` between animation frames; route status to state and render region.
5. Stop animation cleanly on exit (`stop`) and restore cursor/raw mode.

---

## 6) Data/service layer for shared use by CLI + TUI

Create a thin orchestration layer to avoid command logic duplication:
- `cli/services/AccountsService.kt`
- `cli/services/StorageService.kt`
- `cli/services/BackupServiceFacade.kt`

Commands and TUI call same service methods for:
- unlock
- list/filter accounts
- add/remove
- storage delete/reinitialize
- backup provider/list/import/export operations

This keeps behavior consistent across one-shot and TUI flows.

---

## 7) Storage command and backend flag roadmap

`storage --use-backend` is future-facing. Plan in two steps:

1. **Phase A (now):**
   - Add option and plumbing in command/TUI settings.
   - Validate accepted values (`standalone`, `common` or equivalent enum).
   - Persist selected value in local CLI config (new small config file in app dir).
   - No hard backend switch yet if shared backend integration is not available.

2. **Phase B (future):**
   - Implement backend provider factory selection using this config.

Also include:
- `storage clean/delete/reinitialize`
- `storage backup ...` namespace and provider lifecycle actions

---

## 8) Test Plan

## Unit / parser tests
- Update existing parsing tests for new tree:
  - `2fac --help`
  - `2fac accounts add ...`
  - `2fac accounts remove ...`
  - `2fac storage backup export ...`
- Add root no-subcommand behavior tests via mode resolver injection:
  - interactive => launches TUI runner
  - non-interactive => prints help

## Service/state tests
- ViewModel reducer tests for navigation and key actions.
- Home filter logic tests (issuer/account).
- Remove-account flow tests.

## Integration tests
- `:cliApp:test`
- manual smoke checks on macOS/Linux/Windows native builds:
  - interactive terminal
  - piped/non-interactive run (`2fac | cat`, `echo | 2fac` style)

---

## 9) Implementation Phases (Detailed)

## Phase 0 - Lock IA + migration policy
- [x] Confirm exact subcommand names and aliases.
- [x] Confirm how much backup provider functionality is immediate vs placeholder.

### Phase 0 decisions (locked)
- Command IA for one-shot mode is now locked to:
  - `2fac display`
  - `2fac info`
  - `2fac accounts add`
  - `2fac accounts remove`
  - `2fac storage delete|clean|reinitialize` (rolled out incrementally)
  - `2fac storage backup ...`
- Migration strategy is a **hard cutover** (no deprecated root-level `add` or `backup` aliases).
- Backup functionality in this roadmap remains:
  - Immediate: local provider export/import flows under `storage backup ...`
  - Placeholder/future-facing: additional provider lifecycle/auth surfaces in Settings and storage subcommands as phased follow-up work.

## Phase 1 - Command tree refactor
- [x] Introduce `accounts` group.
- [x] Move/refactor add/remove.
- [x] Nest backup under storage.

## Phase 2 - Root mode switch
- [x] Add terminal interactivity detection and branch logic in `MainCommand.run()`.

## Phase 3 - TUI runtime scaffold
- [x] Add navigator, base screen contract, render loop, key event loop.

## Phase 4 - HomeScreen
- [x] Render accounts table + live OTP countdown + filters.

## Phase 5 - AccountScreen
- Selection, details, remove flow with confirmation.

## Phase 6 - SettingsScreen
- Info, backend setting, backup provider surface.

## Phase 7 - Storage/backup integration
- Wire concrete command/service behavior to same backend as TUI.

## Phase 8 - Tests and stabilization
- Parser, state, behavior tests; manual cross-platform checks.

## Phase 9 - Docs and deprecations
- Update CLI help text and docs.
- Add migration notes for any renamed commands.

---

## Risks and Mitigations

1. **Terminal capability differences across native targets**
   - Mitigation: central capability checks; graceful fallback text rendering.
2. **Key event inconsistencies across terminals**
   - Mitigation: normalize key mappings and keep alternate hotkeys (`j/k`, arrows, esc).
3. **Animation flicker / garbled output**
   - Mitigation: single serialized render loop and no out-of-band printing.
4. **Behavior drift between one-shot commands and TUI**
   - Mitigation: shared service layer used by both modes.

---

## Definition of Done

- `2fac` opens TUI only when terminal is interactive.
- `2fac` with no subcommands in non-interactive context prints help and exits.
- One-shot command tree supports display/info/accounts/storage (with nested actions).
- TUI has HomeScreen, AccountScreen, SettingsScreen with keyboard navigation.
- Live OTP countdown is animated/refreshed in TUI safely.
- Tests cover parser migration, mode split behavior, and core TUI state flows.
