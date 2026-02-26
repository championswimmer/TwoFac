# GitHub Copilot Environment Setup

This directory contains the configuration for GitHub Copilot's agent environment.

## What is this?

The `setup.sh` script in this directory runs automatically when GitHub Copilot starts working on this repository. It pre-installs and configures all the necessary tools and dependencies so that Copilot can immediately run build and test commands without waiting for downloads.

## What gets installed?

### Java Development Kit (JDK) 21
- **Distribution**: Eclipse Temurin (AdoptiumJDK)
- **Version**: 21.0.6+7
- **Why**: This project requires JDK 17+ (recommends JDK 21) for Kotlin Multiplatform compilation
- **Matches**: The same JDK version used in GitHub Actions workflows

### Node.js 22
- **Why**: Required for building Web/Wasm targets in the `composeApp` module
- **Matches**: The same Node.js version used in GitHub Actions workflows

### Gradle 8.14.4
- **Pre-download**: The Gradle distribution is downloaded and cached
- **Dependency caching**: Common project dependencies are pre-fetched
- **Why**: Speeds up initial build and test commands significantly
- **Matches**: The version specified in `gradle/wrapper/gradle-wrapper.properties`

## Benefits

With this setup, Copilot can:
- Run `./gradlew test` immediately without waiting for Gradle wrapper download
- Execute `./gradlew check` to verify code quality
- Build specific modules like `./gradlew :sharedLib:test`
- Run platform-specific tests without manual environment configuration

## Maintenance

When updating project requirements:
1. Update the versions in `setup.sh` to match GitHub Actions workflows
2. Ensure JDK version matches the project's minimum requirement (currently JDK 17+)
3. Keep Node.js version in sync with web build requirements
4. Update Gradle version when `gradle/wrapper/gradle-wrapper.properties` changes

## References

- [GitHub Copilot Environment Customization](https://docs.github.com/en/copilot/how-tos/use-copilot-agents/coding-agent/customize-the-agent-environment)
- [Project Development Guide](../README.md#development)
