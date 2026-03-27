---
name: Popular Language Localization Rollout
status: In Progress
progress:
  - "[x] Phase 0 - Lock locale codes, translation scope, and completion criteria"
  - "[x] Phase 1 - Add locale resource directories and file skeletons for all target languages"
  - "[x] Phase 2 - Translate all Compose app string domains for each locale"
  - "[x] Phase 3 - Add localization quality checks and fallback-safety validation"
  - "[ ] Phase 4 - Smoke test language resolution on Android, iOS, Desktop, and Web"
  - "[x] Phase 5 - Document contributor workflow for adding and maintaining translations"
---

# Popular Language Localization Rollout

## Goal

Add first-class app localization support for these initial language targets:

1. Spanish
2. French
3. Italian
4. German
5. Russian
6. Chinese
7. Japanese

This plan assumes the base localization architecture from `22-app-localization-plan.md` is already implemented and English strings in `composeApp` are already centralized in Compose resources.

## Scope

### In scope

1. Add complete translations for all current Compose app string domains in `composeApp/src/commonMain/composeResources`.
2. Keep English (`values/`) as source-of-truth fallback locale.
3. Ensure all new locale directories carry the same resource file names and string keys.
4. Validate runtime language selection through system locale on Android, iOS, Desktop, and Web.

### Out of scope

1. In-app manual language picker (system locale remains policy).
2. CLI localization (`cliApp`).
3. Full watch app localization beyond existing Android resource wrappers.
4. Introducing a new localization library (continue using official Compose Multiplatform resources).

## Locale code decisions

Use Android-style Compose resource qualifiers:

1. Spanish: `values-es/`
2. French: `values-fr/`
3. Italian: `values-it/`
4. German: `values-de/`
5. Russian: `values-ru/`
6. Chinese (Simplified, default first target): `values-zh-rCN/`
7. Japanese: `values-ja/`

Notes:

1. Chinese is implemented as Simplified Chinese for the first milestone to avoid ambiguous mixed-script translations.
2. Traditional Chinese can be added later as a follow-up locale, for example `values-zh-rTW/`.

## Target code structure

All localized Compose strings live under `composeApp/src/commonMain/composeResources/`.

```text
composeApp/
  src/commonMain/
    composeResources/
      values/
        strings_core.xml
        strings_home.xml
        strings_accounts.xml
        strings_settings.xml
        strings_backup.xml
        strings_onboarding.xml
        strings_security.xml

      values-es/
        strings_core.xml
        strings_home.xml
        strings_accounts.xml
        strings_settings.xml
        strings_backup.xml
        strings_onboarding.xml
        strings_security.xml

      values-fr/
        strings_core.xml
        strings_home.xml
        strings_accounts.xml
        strings_settings.xml
        strings_backup.xml
        strings_onboarding.xml
        strings_security.xml

      values-it/
        strings_core.xml
        strings_home.xml
        strings_accounts.xml
        strings_settings.xml
        strings_backup.xml
        strings_onboarding.xml
        strings_security.xml

      values-de/
        strings_core.xml
        strings_home.xml
        strings_accounts.xml
        strings_settings.xml
        strings_backup.xml
        strings_onboarding.xml
        strings_security.xml

      values-ru/
        strings_core.xml
        strings_home.xml
        strings_accounts.xml
        strings_settings.xml
        strings_backup.xml
        strings_onboarding.xml
        strings_security.xml

      values-zh-rCN/
        strings_core.xml
        strings_home.xml
        strings_accounts.xml
        strings_settings.xml
        strings_backup.xml
        strings_onboarding.xml
        strings_security.xml

      values-ja/
        strings_core.xml
        strings_home.xml
        strings_accounts.xml
        strings_settings.xml
        strings_backup.xml
        strings_onboarding.xml
        strings_security.xml
```

## Translation and key management rules

1. Never rename keys during translation rollout unless all locales are updated in the same change.
2. Keep key sets identical across all locale directories.
3. Preserve placeholders exactly (`%1$s`, `%2$d`, etc.) and keep argument order safe for grammar.
4. Escape apostrophes and XML-sensitive characters consistently.
5. Do not split a single user sentence into multiple keys if grammar can vary by language.
6. Treat missing translations as blockers for advertising language support.

## Delivery phases

### Phase 0 - Lock locale codes, translation scope, and completion criteria

1. Confirm this rollout uses the seven locales listed above.
2. Lock Chinese to Simplified (`zh-CN`) for v1.
3. Freeze current string keyset in English resource files before translation starts.
4. Define completion criteria:
   - all keys translated in every target locale
   - no placeholder mismatches
   - no missing file domains in any locale directory

Expected output: approved locale matrix and frozen translation baseline.

### Phase 1 - Add locale resource directories and file skeletons for all target languages

1. Create `values-es`, `values-fr`, `values-it`, `values-de`, `values-ru`, `values-zh-rCN`, and `values-ja`.
2. Add all seven domain files in each directory:
   - `strings_core.xml`
   - `strings_home.xml`
   - `strings_accounts.xml`
   - `strings_settings.xml`
   - `strings_backup.xml`
   - `strings_onboarding.xml`
   - `strings_security.xml`
3. Initialize each file by copying key structure from English source files.

Expected output: complete locale directory tree exists with structurally valid XML files.

### Phase 2 - Translate all Compose app string domains for each locale

1. Translate `strings_core.xml` first (global actions/errors/chrome).
2. Translate feature files in this order:
   - home
   - accounts
   - settings
   - backup
   - onboarding
   - security
3. Validate every translated file preserves all source keys.
4. Review high-risk text categories carefully:
   - destructive actions
   - biometric/security prompts
   - backup/export/import messaging
   - onboarding instructional copy

Expected output: full translated content for all seven locales and seven domains.

### Phase 3 - Add localization quality checks and fallback-safety validation

1. Add a simple verification script/test that compares locale keysets against `values/`.
2. Fail validation for:
   - missing keys
   - extra unknown keys
   - placeholder count/type mismatches
3. Ensure build-time generated `Res.string.*` remains unchanged (key-compatible).

Expected output: automated guardrails prevent translation drift and runtime formatting errors.

### Phase 4 - Smoke test language resolution on Android, iOS, Desktop, and Web

1. Android: run app with device locale switched to each target language and verify top-level screens.
2. iOS: run simulator per target locale and verify navigation, dialogs, and settings labels.
3. Desktop: run with locale environment override and verify key screens.
4. Web/Wasm: verify browser language preference resolves expected resource set.
5. Confirm fallback behavior (missing translation path should fall back to English).

Expected output: cross-platform confirmation that runtime locale loading works for all seven languages.

### Phase 5 - Document contributor workflow for adding and maintaining translations

1. Update localization docs in module guidance (including exact folder path and qualifier examples).
2. Add a lightweight translation checklist for contributors:
   - add key in `values/`
   - propagate key to all supported locale directories
   - run localization validation
3. Document policy for introducing new languages and how to choose locale qualifiers.

Expected output: repeatable workflow for maintaining multi-language quality as product copy evolves.

## Validation commands

Use existing repository checks plus module-focused verification:

1. `./gradlew --no-daemon :composeApp:compileKotlinMetadata`
2. `./gradlew --no-daemon :composeApp:check`
3. Optional full baseline: `./gradlew --no-daemon check` (noting known unrelated `cliApp` linker issue if still present)

## Risks and mitigations

1. Placeholder mismatch risk across languages.
   - Mitigation: automated placeholder parity checks in Phase 3.
2. Incomplete locale coverage as strings evolve.
   - Mitigation: enforce keyset parity against English on CI/local checks.
3. Chinese script/region ambiguity.
   - Mitigation: explicitly scope v1 to Simplified Chinese (`zh-CN`) and track Traditional as follow-up.
4. UX truncation on smaller screens for verbose translations.
   - Mitigation: run targeted UI smoke checks for settings, dialogs, and onboarding cards on phone-sized layouts.

## Follow-up backlog (after this plan)

1. Add Traditional Chinese locale (`values-zh-rTW`).
2. Consider in-app language override setting if product requests manual language control.
3. Evaluate watch app localization expansion beyond current wrapper strings.
4. Evaluate CLI localization as a separate plan with terminal-focused copy strategy.

## Final recommendation

Ship this as a translation-completeness milestone in `composeApp` only, using directory-per-locale under `composeResources` with strict key parity and placeholder validation. This keeps the architecture already adopted in the previous localization plan and provides a clean, scalable base for future languages.
