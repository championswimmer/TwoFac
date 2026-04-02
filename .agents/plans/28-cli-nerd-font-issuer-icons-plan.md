---
name: CLI Nerd Font Issuer Icons Plan
status: Planned
progress:
  - "[ ] Phase 0 - Research and Confirm Nerd Font Unicode Mapping"
  - "[ ] Phase 1 - Expose and Resolve Issuer Metadata in CLI"
  - "[ ] Phase 2 - Implement CLI-Specific Nerd Font Glyph Resolution"
  - "[ ] Phase 3 - Update `DisplayCommand` UI (Mordant Table)"
  - "[ ] Phase 4 - Add CLI Toggle / Environment Detection for Icons"
  - "[ ] Phase 5 - Testing and Validation"
---

# CLI Nerd Font Issuer Icons Plan

## Goal
Enhance the `cliApp` terminal output by prefixing account labels with their respective brand icons. Since standard terminals do not render images, we will leverage **Nerd Fonts**, which include patched Font Awesome and Devicon glyphs. The goal is to reuse the existing `IssuerIconCatalog` logic from `sharedLib` while adapting the unicode glyphs specifically for CLI rendering via Mordant.

## Research Summary & Nerd Font Mechanics
1. **Existing Mechanism in `sharedLib`:** 
   - `IssuerIconCatalog.kt` already parses raw strings into normalized issuer keys (e.g., `"githubcom"` -> `"github"`).
   - It maps these keys to Font Awesome Brands unicodes (`iconGlyphs`).
2. **Nerd Font Compatibility:**
   - Nerd Fonts patch many icon sets. Font Awesome (FA) and Material Design Icons (MDI) are prominent. 
   - While `IssuerIconCatalog.kt` has unicodes for FA Brands (e.g., Github `\uF09B`), the exact unicode point in a Nerd Font environment might sometimes match perfectly (especially for legacy FA icons), but for newer FA 6 Brands, Nerd Fonts typically places them in specific ranges or relies on Devicons (e.g., `\uE700` range) or Font Logos (`\uF300` - `\uF32F`).
   - We need to define a CLI-specific glyph map that specifically targets known Nerd Font unicodes, falling back to a generic lock/key symbol (`\uF023`  or `\uF084` ) for unknown issuers.
3. **Mordant Integration:**
   - Mordant (used in `cliApp`) supports rendering standard unicode characters.
   - We can concatenate the icon and the `accountLabel` inside the `cell()` definition of the table body.

---

## Phase-by-Phase Implementation Roadmap

### Phase 0 - Research and Confirm Nerd Font Unicode Mapping
1. **Audit Existing Issuers against Nerd Font Cheat Sheet:**
   - Look up the `iconKey`s defined in `IssuerIconCatalog.kt` (e.g., `amazon`, `google`, `github`, `slack`, `discord`).
   - Find their exact hex codes in the Nerd Font cheat sheet (https://www.nerdfonts.com/cheat-sheet).
   - Identify discrepancies between standard Font Awesome 6 unicodes used in `composeApp` and the Nerd Font patches.
2. **Actionable Output:**
   - Create a dedicated CLI-friendly mapping layer if the unicodes diverge heavily, OR confirm that the existing `IssuerIconCatalog.glyphForIconKey()` works accurately in modern terminals (like Kitty, iTerm2, Windows Terminal with Nerd Fonts V3).

### Phase 1 - Expose and Resolve Issuer Metadata in CLI
1. `StoredAccount.DisplayAccount` already has an `issuer: String?` field.
2. In `TwoFacLib` (or wherever accounts are fetched for the CLI), ensure that the `issuer` string is correctly populated during the `getAllAccountOTPs()` mapping.
3. Verify that `DisplayCommand.kt` receives the `issuer` inside the `displayAccounts` pairs.

### Phase 2 - Implement CLI-Specific Nerd Font Glyph Resolution
1. Create a helper in `cliApp` (e.g., `cliApp/src/commonMain/kotlin/tech/arnav/twofac/cli/theme/CliIssuerIcons.kt`).
2. Implement a resolution function that bridges the shared library to the CLI presentation:
   ```kotlin
   fun getNerdFontIconForIssuer(rawIssuer: String?): String {
       val match = IssuerIconCatalog.resolveIssuerIcon(rawIssuer)
       if (match.isPlaceholder) {
           return "\uF084" // Nerd Font generic key icon
       }
       // Retrieve glyph. If Nerd Fonts require different unicodes than sharedLib,
       // this is the place to intercept and override specific iconKeys.
       return IssuerIconCatalog.glyphForIconKey(match.iconKey) ?: "\uF084"
   }
   ```
3. (Optional) Assign terminal-specific brand colors to these icons (e.g., blue for Discord, green for GitHub) using `TextColors`.

### Phase 3 - Update `DisplayCommand` UI (Mordant Table)
1. Open `cliApp/src/commonMain/kotlin/tech/arnav/twofac/cli/commands/DisplayCommand.kt`.
2. Locate the `createTable` function.
3. For each row in `displayAccounts.forEach`, resolve the icon:
   ```kotlin
   val icon = CliIssuerIcons.getNerdFontIconForIssuer(account.issuer)
   ```
4. Update the "Account" cell to include the icon:
   ```kotlin
   row {
       // e.g. " GitHub:alice@example.com"
       cell("$icon  ${account.accountLabel}")
       cell(otp.split("").joinToString(" "))
       // ... timer cell
   }
   ```

### Phase 4 - Add CLI Toggle / Environment Detection for Icons
1. Not all users will have a Nerd Font installed, which will result in missing glyph (tofu ``) characters.
2. Add a flag to `DisplayCommand` to disable icons explicitly:
   ```kotlin
   val noIcons by option("--no-icons", help = "Disable Nerd Font issuer icons").flag(default = false)
   ```
3. Alternatively, check environment variables (e.g., `TERMINAL_EMULATOR`, `TERM`, or a custom `TWOFAC_CLI_ICONS=1`) to smartly default `noIcons` to `false` or `true`.
4. In the `createTable` loop:
   ```kotlin
   val iconStr = if (noIcons) "" else "${CliIssuerIcons.getNerdFontIconForIssuer(account.issuer)}  "
   cell("$iconStr${account.accountLabel}")
   ```

### Phase 5 - Testing and Validation
1. **Local Terminal Validation:**
   - Compile and run the CLI locally using a terminal equipped with a Nerd Font (e.g., FiraCode Nerd Font, Hack Nerd Font).
   - Ensure the alignment and spacing of the Mordant table aren't broken by the double-width or misaligned font glyphs. (Add an extra space after the icon if necessary).
2. **Fallback Testing:**
   - Test with `--no-icons` to ensure the table cleanly renders text-only.
   - Test an account with an unknown issuer to verify the fallback key/lock icon appears.
3. **Automated Tests:**
   - Update `DisplayCommandTest.kt` to verify that the table output contains the expected unicode glyphs when `noIcons` is false, and excludes them when true.

## Files Likely Touched
- `cliApp/src/commonMain/kotlin/tech/arnav/twofac/cli/commands/DisplayCommand.kt`
- `cliApp/src/commonMain/kotlin/tech/arnav/twofac/cli/theme/CliIssuerIcons.kt` (New)
- `cliApp/src/commonTest/kotlin/tech/arnav/twofac/cli/commands/DisplayCommandTest.kt`

## Scope Guardrails
- **Do not** alter `sharedLib/src/commonMain/kotlin/tech/arnav/twofac/lib/presentation/issuer/IssuerIconCatalog.kt` unless absolute necessary (e.g., exposing a mapping).
- Keep the UI impact restricted to the CLI terminal table.
