---
name: App Localization Plan
status: Planned
progress:
  - "[ ] Phase 0 - Audit all user-facing strings and confirm first-pass scope"
  - "[ ] Phase 1 - Add a shared Compose localization structure and string access helpers"
  - "[ ] Phase 2 - Migrate common Compose UI strings from hardcoded Kotlin into resources"
  - "[ ] Phase 3 - Migrate platform-specific prompts and non-Compose app surfaces"
  - "[ ] Phase 4 - Add localization validation, translation workflow, and optional language override support"
---

# App Localization Plan

## Goal

Make the app's user-facing strings localizable by moving hardcoded text into a shared resource location, using a folder layout that can scale to however many languages we decide to support later.

The primary implementation target should be the Compose Multiplatform app surface in `composeApp`, because that is where Android, iOS, Desktop, and Web already share UI code. Android and watch-only wrappers should then be aligned around the same approach where practical.

## Current repository state

### Relevant code and build facts

- `composeApp` already depends on the official Compose resources library:
  - `/home/runner/work/TwoFac/TwoFac/composeApp/build.gradle.kts`
- `composeApp` already has a shared resources directory, but it currently contains only drawables:
  - `/home/runner/work/TwoFac/TwoFac/composeApp/src/commonMain/composeResources/drawable/...`
- There is currently no usage of `stringResource(...)` anywhere in the repository.
- Many user-facing strings are still hardcoded directly in shared Compose code, for example:
  - `/home/runner/work/TwoFac/TwoFac/composeApp/src/commonMain/kotlin/tech/arnav/twofac/screens/AddAccountScreen.kt`
  - `/home/runner/work/TwoFac/TwoFac/composeApp/src/commonMain/kotlin/tech/arnav/twofac/screens/SettingsScreen.kt`
  - `/home/runner/work/TwoFac/TwoFac/composeApp/src/commonMain/kotlin/tech/arnav/twofac/components/onboarding/OnboardingStepCard.kt`
- There are also platform-specific hardcoded user strings in:
  - Android biometric prompts: `/home/runner/work/TwoFac/TwoFac/composeApp/src/androidMain/kotlin/tech/arnav/twofac/session/AndroidBiometricSessionManager.kt`
  - Desktop-only settings UI: `/home/runner/work/TwoFac/TwoFac/composeApp/src/desktopMain/kotlin/tech/arnav/twofac/screens/PlatformSettings.jvm.kt`
  - Watch UI: `/home/runner/work/TwoFac/TwoFac/watchApp/src/main/java/tech/arnav/twofac/watch/ui/EmptyState.kt`
  - CLI prompts/help text: `/home/runner/work/TwoFac/TwoFac/cliApp/src/commonMain/kotlin/tech/arnav/twofac/cli/commands/*.kt`
- Android and watch app wrappers already have traditional Android `strings.xml` files, but only for thin wrapper text today:
  - `/home/runner/work/TwoFac/TwoFac/androidApp/src/main/res/values/strings.xml`
  - `/home/runner/work/TwoFac/TwoFac/watchApp/src/main/res/values/strings.xml`

### Baseline verification status

- Baseline `./gradlew --no-daemon check` was run before creating this plan.
- Current pre-existing failure is unrelated to localization work:
  - `:cliApp:linkDebugTestLinuxX64` fails with a duplicate Clikt symbol during Kotlin/Native linking.
- `:composeApp:check` completed during that baseline run, so the Compose app area is otherwise in a workable state for a localization migration.

## External research summary

### Research used

- JetBrains Compose Multiplatform localization guide:
  - <https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-localize-strings.html>
- JetBrains Compose Multiplatform resources usage guide:
  - <https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-multiplatform-resources-usage.html>
- moko-resources repository and docs:
  - <https://github.com/icerockdev/moko-resources>

### Key takeaways

1. **The official Compose Multiplatform resources API is now the default fit for shared Compose UI.**
   - Localized strings live under `commonMain/composeResources/values/` and `values-<locale>/`.
   - Building the module generates typed accessors in `Res`.
   - Shared Compose UI reads them with `stringResource(...)`.

2. **The official API already matches this repository's current direction.**
   - `composeApp` already includes the Compose resources dependency.
   - `composeApp` already uses `composeResources` for images, so the build pipeline is partly in place already.
   - This makes the official API the lowest-risk choice for Android + iOS + Desktop + Web shared UI.

3. **moko-resources remains a valid alternative, but it would add extra Gradle/plugin complexity that this repo likely does not need for the first pass.**
   - It is strongest when a project needs more extensive non-Compose/iOS-native localization bridging immediately.
   - For this repo, adding moko would be a larger architectural move than extending the existing Compose resource setup.

4. **The official resource layout already scales to any number of supported languages.**
   - Add one locale directory per language.
   - Keep the same keys across locale directories.
   - Use placeholders/plurals instead of string concatenation where grammar can vary.

## Recommended architecture decision

Use **official Compose Multiplatform string resources** as the primary localization mechanism for the app UI.

### Why this is the right fit here

- It works across the `composeApp` targets that already share UI code:
  - Android
  - iOS
  - Desktop
  - Web/Wasm
- It avoids introducing a new third-party localization library when the repo already has the necessary first-party dependency.
- It centralizes shared app strings in one cross-platform location instead of leaving them scattered across composables.

### Recommended first-pass scope

**In scope first:**

- `composeApp/src/commonMain` user-visible strings
- `composeApp` platform-specific user-visible prompts that are part of app UX
- thin wrapper strings that must remain in Android resources (`app_name`, notification labels, etc.)

**Explicitly not first-pass scope unless requested:**

- internal logs
- protocol/storage constants
- backup IDs / filenames
- non-user-facing exception text
- full CLI localization

CLI localization can be planned later once the app UI migration is stable. It has different ergonomics because it is not Compose-based.

## Proposed localization folder design

Use `composeApp/src/commonMain/composeResources` as the shared app localization root.

### Folder structure

```text
composeApp/
  src/commonMain/
    composeResources/
      values/
        strings_core.xml
        strings_accounts.xml
        strings_settings.xml
        strings_onboarding.xml
        strings_backup.xml
        strings_errors.xml
      values-es/
        strings_core.xml
        strings_accounts.xml
        strings_settings.xml
        strings_onboarding.xml
        strings_backup.xml
        strings_errors.xml
      values-fr/
        strings_core.xml
        strings_accounts.xml
        strings_settings.xml
        strings_onboarding.xml
        strings_backup.xml
        strings_errors.xml
```

### Design rules

1. `values/` is the default fallback locale, likely English.
2. Every supported language gets its own sibling `values-<locale>/` directory.
3. Split strings by feature/domain instead of keeping one giant `strings.xml`.
4. Keep **the same file names and keys** across locales to reduce translation drift.
5. Prefer Android-style locale qualifiers (`values-es`, `values-fr`, `values-pt-rBR`, etc.) so the structure remains familiar.
6. Use placeholders/plurals instead of concatenating messages in Kotlin when values are dynamic.

### Naming conventions

- Use stable snake_case keys:
  - `settings_title`
  - `settings_back_content_description`
  - `add_account_cta`
  - `backup_export_success`
- Suffix by intent where useful:
  - `_title`
  - `_subtitle`
  - `_description`
  - `_label`
  - `_placeholder`
  - `_cta`
  - `_error`
  - `_content_description`

## Access pattern design

### In composables

Use the generated resources directly:

- `stringResource(Res.string.settings_title)`
- `stringResource(Res.string.backup_export_success, backupId)`

This should replace direct string literals in `Text(...)`, `contentDescription`, dialog titles, button labels, snackbar messages, and helper text.

### In non-composable code

Do **not** push localized strings deeper into shared business logic by default.

Instead:

1. Keep `sharedLib` returning domain results/errors, not localized UI text.
2. Resolve user-facing text near the UI boundary.
3. For platform APIs that need immediate `String` values outside composition (for example biometric prompt builders), add a small app-layer abstraction such as:
   - `StringResolver`
   - `AppStrings`
   - or `expect/actual` locale-aware helpers if required

That resolver should live in `composeApp`, not `sharedLib`, because localization is a presentation concern here.

## Delivery phases

### Phase 0 - Audit all user-facing strings and confirm first-pass scope

1. Produce a repository inventory of user-facing hardcoded strings grouped by module:
   - `composeApp/commonMain`
   - `composeApp/androidMain`
   - `composeApp/desktopMain`
   - `watchApp`
   - `cliApp`
2. Classify each string as one of:
   - shared Compose UI string
   - platform-specific app UI string
   - wrapper/app-name string
   - CLI-only string
   - not user-facing / do not localize
3. Decide whether watch and CLI are:
   - included in the same milestone,
   - or tracked as follow-up phases after `composeApp`.

**Expected output of Phase 0:** a migration checklist of files and keys to create.

### Phase 1 - Add a shared Compose localization structure and string access helpers

4. Create initial string XML files under:
   - `composeApp/src/commonMain/composeResources/values/`
5. Add the first locale directory for the default language only.
6. Define the first set of keys for:
   - navigation/common app chrome
   - accounts
   - add account
   - settings
   - onboarding
   - backup/import/export
   - common errors
7. Add a tiny localization access layer for non-composable code if needed:
   - likely `StringResolver` or similar app-layer utility in `composeApp`
8. Document a rule that new user-facing text in `composeApp` should not be hardcoded in Kotlin.

**Expected output of Phase 1:** generated `Res.string.*` accessors exist and can be used by shared Compose UI.

### Phase 2 - Migrate common Compose UI strings from hardcoded Kotlin into resources

9. Migrate top-level screens first:
   - `AccountsScreen`
   - `AddAccountScreen`
   - `AccountDetailScreen`
   - `SettingsScreen`
   - `OnboardingGuideScreen`
10. Migrate reusable UI components next:
   - account fields/buttons/errors
   - onboarding cards and CTA labels
   - home/locked/empty/loading states
   - backup/settings cards and dialogs
11. Replace string concatenation with formatted resources where needed:
   - delete confirmation messages
   - snackbar messages
   - backup/import/export success and failure messages
12. Replace accessibility strings too:
   - icon `contentDescription`
   - scanner/expand/collapse labels

**Expected output of Phase 2:** shared Compose UI no longer contains hardcoded user-visible copy except intentionally dynamic content.

### Phase 3 - Migrate platform-specific prompts and non-Compose app surfaces

13. Migrate `composeApp/androidMain` prompts that users see directly:
   - biometric titles/subtitles/buttons
   - camera/clipboard permission-related messages if they surface to users
14. Migrate `composeApp/desktopMain` user-visible labels:
   - tray/menu bar text
   - quit button text
15. Align wrapper resources where needed:
   - `androidApp/src/main/res/values/strings.xml`
   - platform manifest/app-name strings
16. Decide how to handle `watchApp`:
   - either keep Android XML strings there,
   - or create a follow-up to share more localized copy if watch UI grows.
17. Leave `cliApp` for a follow-up unless explicit multi-language CLI support is required now.

**Expected output of Phase 3:** all user-facing app strings across main app surfaces are no longer hardcoded.

### Phase 4 - Add localization validation, translation workflow, and optional language override support

18. Add tests or validations that catch accidental regressions:
   - compile-time verification through generated resource accessors
   - targeted tests for helper/resolver logic if one is added
   - optional snapshot/smoke tests for major screens
19. Add contributor guidance:
   - where new keys go
   - how to add a new locale
   - how to name keys
   - how to handle formatted strings and plurals
20. Decide whether the app needs **system-locale only** support first or a **manual in-app language override**.

#### Recommended initial policy

Start with **system locale selection** first, because it is the simplest path and is already supported by the Compose resource model.

If a manual override is later required:

21. Add an app setting such as `AppLanguage`.
22. Store it in the existing settings layer.
23. Inject/apply the locale override at the app root.
24. Recompose the UI from that chosen locale.

This should be a second step, not a blocker for removing hardcoded strings.

## Migration rules

1. Only localize **user-facing text**.
2. Keep business logic and storage models language-neutral.
3. Do not embed English sentences in view models if the UI can decide the message.
4. Prefer keys that describe intent, not current English wording.
5. Keep translations complete per locale before advertising support for that locale.
6. Avoid assembling sentences from several small strings when grammar may differ by language.

## Validation plan

For actual implementation work, validate in small steps using existing repository commands:

- Baseline repository state:
  - `./gradlew --no-daemon check`
  - note: currently blocked by the pre-existing `:cliApp:linkDebugTestLinuxX64` issue
- Compose app validation:
  - `./gradlew --no-daemon :composeApp:desktopTest :composeApp:compileKotlinMetadata`
  - `./gradlew --no-daemon :composeApp:check`

Manual verification should also include:

- Desktop app smoke check for visible screen labels
- Android app smoke check for biometric prompt labels
- Web/Wasm smoke check for generated resource loading

## Recommended implementation order

1. Set up `composeResources/values/*.xml` and move a small vertical slice first:
   - app bars
   - buttons
   - dialogs
2. Convert shared screens/components.
3. Convert platform-specific prompts in `composeApp`.
4. Clean up remaining Android/watch wrapper strings.
5. Decide whether CLI localization deserves its own separate plan.

## Open decisions to confirm before implementation

1. Is the first milestone only for `composeApp`, or should `watchApp` be included immediately too?
2. Should CLI localization be out of scope for now?
3. Is **system locale** enough for v1, or do we need an in-app language selector in the same project?
4. Which language should be the default source locale?

## Final recommendation

For this repository, the smallest and most maintainable path is:

1. Keep localization centered in `composeApp/src/commonMain/composeResources`.
2. Use the official Compose Multiplatform resources API already present in the build.
3. Move user-visible text out of Kotlin files incrementally.
4. Add locale directories over time as translations become available.
5. Treat watch and CLI localization as explicit follow-up scope unless product requirements say otherwise.
