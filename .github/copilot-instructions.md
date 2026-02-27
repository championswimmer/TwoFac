This is a Kotlin Multiplatform project.

This is what each module is for:

- `sharedLib`: shared common business logic code
- `composeApp`: Android, Desktop and Web (wasm) app using Compose
- `iosApp`: iOS app that uses the `TwoFacKit` (from `composeApp`) as a framework
- `cliApp`: command line interface app that uses the `sharedLib` for business logic

## Environment Setup

The project uses `.github/workflows/copilot-setup-steps.yml` to pre-configure the Copilot environment with:

- **JDK 21** (Temurin distribution) - matches the version used in GitHub Actions
- **Node.js 22** - required for Web/Wasm builds
- **Gradle 8.14.4** - pre-downloaded and cached for faster builds

This ensures you can run Gradle tasks (like `./gradlew test`, `./gradlew check`) immediately without waiting for dependency downloads.
