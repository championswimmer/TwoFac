This is a Kotlin Multiplatform project.

For the full module map, dependency direction, and platform-to-module routing see
[AGENTS.md](../AGENTS.md) — that file is the single source of truth for contributor
guidance and is kept up to date as the codebase evolves.

## Environment Setup

The project uses `.github/workflows/copilot-setup-steps.yml` to pre-configure the Copilot environment with:

- **JDK 21** (Temurin distribution) - matches the version used in GitHub Actions
- **Node.js 22** - required for Web/Wasm builds
- **Gradle 9.1.0** - pre-downloaded and cached for faster builds

This ensures you can run Gradle tasks (like `./gradlew test`, `./gradlew check`) immediately without waiting for dependency downloads.
