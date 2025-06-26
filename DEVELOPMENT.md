# TwoFac Development Guide

A Kotlin Multiplatform two-factor authentication (2FA) library and applications supporting TOTP/HOTP.

## Project Overview

TwoFac is a Kotlin Multiplatform project that provides:

- **Shared Library**: Core 2FA functionality (TOTP/HOTP, OTP URI parsing, cryptography)
- **CLI Application**: Native command-line interface
- **Compose Multiplatform UI**: Cross-platform GUI application for Android, iOS, Desktop, and Web

## Prerequisites

- **JDK 17+** (recommended: JDK 21)
- **Android SDK** (if building Android targets)
- **Xcode** (if building iOS targets on macOS)
- **Native toolchains** for your platform (GCC/Clang for Linux, MSVC for Windows)

## Project Structure

```
TwoFac/
├── build.gradle.kts              # Root build configuration
├── settings.gradle.kts           # Project settings and module includes
├── gradle.properties             # Global Gradle properties
├── gradle/
│   └── libs.versions.toml        # Version catalog for dependencies
├── sharedLib/                    # Core 2FA library (Kotlin Multiplatform)
├── cliApp/                       # Command-line application (Native targets)
├── composeApp/                   # Compose Multiplatform UI application
└── iosApp/                       # iOS application wrapper
```

### Module Details

#### 📚 `sharedLib/` - Core Library

**Purpose**: Contains the core 2FA functionality that all applications depend upon.

**Key Components**:

- `otp/` - OTP implementations (TOTP, HOTP interfaces and classes)
- `crypto/` - Cryptographic tools and encoding utilities
- `uri/` - OtpAuthURI parser for handling `otpauth://` URIs
- `storage/` - Storage abstractions for persisting 2FA secrets
- `backup/` - Backup and restore functionality

**Targets**:

- `jvm` - Used by Android and Desktop applications
- `native` - Used by CLI and exported as iOS framework
- `wasmJs` - Used by Web application

**Key Files**:

- `OTP.kt` - Common interface for OTP generation/validation
- `TOTP.kt` - Time-based OTP implementation (RFC 6238)
- `HOTP.kt` - HMAC-based OTP implementation (RFC 4226)
- `OtpAuthURI.kt` - Parser for otpauth:// URIs with Builder pattern
- `CryptoTools.kt` - Cryptographic operations interface
- `DefaultCryptoTools.kt` - Platform-specific crypto implementations

#### 🖥️ `cliApp/` - CLI Application

**Purpose**: Native command-line application for managing 2FA codes.

**Targets**: `macosArm64`, `macosX64`, `linuxX64`, `mingwX64`

**Dependencies**:

- `sharedLib` (native variant)
- `clikt` - Command-line interface framework
- `kotlin-multiplatform-appdirs` - Cross-platform app directories
- `koin` - Dependency injection

**Key Files**:

- `Main.kt` - Application entry point
- `Platform.kt` - Platform-specific implementations

**Binary Output**: `2fac` executable

#### 🎨 `composeApp/` - Compose Multiplatform UI

**Purpose**: Cross-platform GUI application with native look and feel.

**Targets**:

- `androidTarget` - Android application
- `iosX64`, `iosArm64`, `iosSimulatorArm64` - iOS framework
- `jvm("desktop")` - Desktop application (JVM)
- `wasmJs` - Web application

**Dependencies**:

- `sharedLib` (platform-specific variants)
- Compose Multiplatform UI framework
- `koin` - Dependency injection
- AndroidX lifecycle components

**Key Files**:

- `App.kt` - Main Compose UI entry point
- `MainActivity.kt` - Android-specific entry point
- `MainViewController.kt` - iOS-specific entry point
- `main.kt` - Desktop and Web entry points

#### 📱 `iosApp/` - iOS Application Wrapper

**Purpose**: iOS application project that uses the Compose framework.

**Contents**:

- Xcode project configuration
- iOS-specific resources and assets
- Swift UI integration points

## Building the Project

### Setup

1. **Clone the repository**:
   ```bash
   git clone <repository-url>
   cd TwoFac
   ```

2. **Verify prerequisites**:
   ```bash
   ./gradlew --version
   java -version
   ```

### Build Commands

#### Build All Targets

```bash
./gradlew build
```

#### Build Shared Library

```bash
./gradlew :sharedLib:build
```

#### Build CLI Application

```bash
./gradlew :cliApp:build
```

#### Build Compose Applications

```bash
./gradlew :composeApp:build
```

### Platform-Specific Builds

#### Android

```bash
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:assembleRelease
```

#### iOS (macOS only)

```bash
./gradlew :composeApp:iosSimulatorArm64Test
./gradlew :sharedLib:iosArm64Binaries
```

#### Desktop

```bash
./gradlew :composeApp:packageDistributionForCurrentOS
```

#### Web

```bash
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
```

#### CLI Native Binaries

```bash
# macOS
./gradlew :cliApp:macosArm64Binaries
./gradlew :cliApp:macosX64Binaries

# Linux
./gradlew :cliApp:linuxX64Binaries

# Windows
./gradlew :cliApp:mingwX64Binaries
```

## Running Applications

### CLI Application

```bash
# After building
./cliApp/build/bin/macosArm64/debugExecutable/2fac.kexe
```

### Desktop Application

```bash
./gradlew :composeApp:run
```

### Web Application

```bash
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
# Then open http://localhost:8080
```

### Android Application

```bash
./gradlew :composeApp:installDebug
# Or use Android Studio
```

## Testing

### Run All Tests

```bash
./gradlew test
```

### Module-Specific Tests

```bash
./gradlew :sharedLib:test
./gradlew :cliApp:test
./gradlew :composeApp:test
```

### Platform-Specific Tests

```bash
./gradlew :sharedLib:jvmTest
./gradlew :sharedLib:nativeTest
./gradlew :composeApp:desktopTest
```

## Code Coverage

The project uses Kotlinx Kover for code coverage:

```bash
./gradlew koverHtmlReport
# Report available at: build/reports/kover/html/index.html
```

## Key Dependencies

- **Kotlin**: 2.1.21
- **Compose Multiplatform**: 1.8.1
- **Koin**: 4.1.0 (Dependency Injection)
- **Clikt**: 5.0.3 (CLI framework)
- **Cryptography-KT**: 0.4.0 (Cross-platform cryptography)
- **Kotlinx Coroutines**: 1.10.2

## Development Tips

### IDE Setup

- **Android Studio**: Recommended for Android development
- **IntelliJ IDEA**: Good for all platforms
- **Xcode**: Required for iOS development

### Debugging

- Set breakpoints in platform-specific source sets
- Use platform-specific debugging tools
- Check `Platform.kt` files for platform-specific behavior

### Adding New Platforms

1. Add target in `build.gradle.kts`
2. Create platform-specific source set
3. Implement platform-specific code in `Platform.<target>.kt`

### Code Style

- Follow official Kotlin coding conventions
- Use `kotlin.code.style=official` (already configured)
- 4-space indentation
- 120-character line limit

## Troubleshooting

### Common Issues

**Gradle Build Fails**:

- Clean and rebuild: `./gradlew clean build`
- Check Java version compatibility
- Verify all required SDKs are installed

**Native Compilation Issues**:

- Ensure native toolchains are installed
- Check platform-specific requirements
- For macOS: Verify Xcode command line tools

**iOS Build Issues**:

- Open iOS project in Xcode and check configuration
- Verify iOS deployment target compatibility
- Clean derived data in Xcode

**Web Application Issues**:

- Clear browser cache
- Check WASM support in browser
- Verify development server is running

### Performance Configuration

The project is configured for optimal performance:

- Gradle daemon with 6GB heap (`kotlin.daemon.jvmargs=-Xmx6G`)
- Configuration cache enabled
- Build cache enabled
- Native cache optimization for development

## Contributing

1. Follow the existing code structure
2. Add tests for new functionality
3. Update documentation as needed
4. Ensure all platforms build successfully
5. Follow Kotlin coding conventions

## Architecture Notes

### Dependency Flow

```
┌─────────────┐    ┌──────────────┐    ┌─────────────┐
│   cliApp    │───▶│  sharedLib   │◀───│ composeApp  │
│  (native)   │    │ (all targets)│    │(all targets)│
└─────────────┘    └──────────────┘    └─────────────┘
                           ▲
                           │
                   ┌───────────────┐
                   │    iosApp     │
                   │   (Swift)     │
                   └───────────────┘
```

### Platform Strategy

- **Common logic** in `sharedLib/commonMain`
- **Platform-specific implementations** in respective `Main` source sets
- **Expect/Actual pattern** for platform differences
- **Native exports** for C interop and iOS framework