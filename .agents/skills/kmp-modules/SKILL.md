---
name: Kotlin Multiplatform Module Structure
description: Guidance on where to place different types of code in this Kotlin Multiplatform project.
---
# Skill: Kotlin Multiplatform Layout

This repository follows standard Kotlin Multiplatform source-set conventions, organized into distinct modules, each serving a specific purpose.

## Project Modules

The project is divided into three primary modules. Understanding their responsibilities and dependencies is key to placing code in the correct location.

### 1. `sharedLib` (Shared Logic)
This is the core module containing all business logic, data models, state management, and storage mechanisms.
- **Purpose**: Centralizes the common logic so it can be reused across different user interfaces (CLI and GUI).
- **Dependencies**: It is completely independent and does **not** depend on `cliApp` or `composeApp`.
- **What goes here**:
  - OTP generation algorithms (e.g., HOTP, TOTP).
  - Data storage operations, database schemas, and repository patterns.
  - Core domain models and use cases/interactors.
  - Platform-specific implementations for storage or cryptography via `expect`/`actual` declarations.

### 2. `composeApp` (GUI Library)
This module contains most of the common Graphical User Interface (GUI) application code, built using Compose Multiplatform.
- **Purpose**: Provides the visual interface for the application (Desktop, Android library, iOS, Web/Wasm).
- **Dependencies**: Depends heavily on `sharedLib` to fetch data, observe state, and trigger actions.
- **What goes here**:
  - UI components, screens, and visual layouts written in Jetpack/Compose Multiplatform.
  - ViewModels or UI state holders that bridge the UI with the `sharedLib`.
  - Platform-specific shared code (e.g., Android DI modules, biometric session, wear sync) within respective `<platform>Main` source sets.
  - Platform-specific GUI bootstrapping for desktop (`main` window) and iOS (framework) within their source sets.

### 3. `androidApp` (Android App Entry Point)
This is a thin wrapper module that hosts the Android application entry point.
- **Purpose**: Contains only the Android `Application` class, `Activity`, manifest, and app-level resources (icons, strings).
- **Dependencies**: Depends on `composeApp` which provides all the shared UI and business logic.
- **What goes here**:
  - `TwoFacApplication` (Application class) and `MainActivity` (Activity).
  - `AndroidManifest.xml` with application/activity declarations.
  - App-level resources (launcher icons, app name string).
  - **Do not** put shared Android code here — that belongs in `composeApp/androidMain`.

### 4. `cliApp` (Command Line Interface)
This module contains code specifically related to the Command Line Interface application.
- **Purpose**: Provides a terminal-based interface for interacting with the application.
- **Dependencies**: Depends on `sharedLib` to perform corresponding operations without needing a graphical interface.
- **What goes here**:
  - Command-line argument parsing and routing.
  - Terminal UI (TUI) drawing and dynamic text layouts (e.g., using libraries like `mordant`).
  - Terminal-specific interactive components (prompts, tables, secure input for secrets).

## Source-Set Mapping (within modules)

Within each Kotlin Multiplatform module (especially `sharedLib` and `composeApp`), the code is further organized by source sets:

- `commonMain`: Shared production code that is platform-agnostic. **Most of the code should live here.**
- `commonTest`: Shared tests for testing code in `commonMain`.
- `<platform>Main` (e.g., `jvmMain`, `iosMain`, `androidMain`): Platform-specific implementations. Place code here when you need to access platform APIs (e.g., `java.io.File` on JVM, `NSUserDefaults` on iOS) that are not available in the common standard library.
- `<platform>Test`: Platform-specific tests.

## Practical Reminders

- When adding new features, always start by implementing the core logic in `sharedLib/src/commonMain`.
- Expose APIs from `sharedLib` that are easy to consume for both declarative reactive UIs (`composeApp`) and procedural/terminal UIs (`cliApp`).
- Keep function signatures in common code and push only unavoidable platform differences into platform source sets using Kotlin's `expect`/`actual` mechanism.
