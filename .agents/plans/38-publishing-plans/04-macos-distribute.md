# macOS Distribution: TwoFac Desktop App

Step-by-step guide for distributing the TwoFac Compose Desktop app on macOS (outside the Mac App Store and optionally via the Mac App Store).

## App Details

- **Bundle ID**: `tech.arnav.twofac`
- **Package Name**: TwoFac
- **Format**: DMG (primary), MSI (Windows), DEB (Linux)
- **Runtime**: JVM (Compose Desktop)
- **Entitlements**: `composeApp/macos.entitlements`

## Prerequisites

- **Apple Developer Program** membership ($99/year) — [Enroll here](https://developer.apple.com/programs/)
- Xcode command line tools installed (`xcode-select --install`)
- Keychain access on the build machine
- Notarization tasks already wired in `build.gradle.kts`

## 1. Obtain a Developer ID Certificate

For distribution **outside** the Mac App Store:

1. Go to [Certificates, Identifiers & Profiles](https://developer.apple.com/account/resources/certificates/list)
2. Click **+** → select **Developer ID Application**
3. Generate a CSR via Keychain Access → Certificate Assistant → Request a Certificate From a Certificate Authority
4. Upload the CSR, download the `.cer`, double-click to install in Keychain
5. Verify: `security find-identity -v -p codesigning` — look for "Developer ID Application: ..."
- Docs: [Developer ID overview](https://developer.apple.com/developer-id/)
- Docs: [Create certificates](https://developer.apple.com/help/account/create-certificates/create-developer-id-certificates/)

## 2. Configure Code Signing in Gradle

The Compose Multiplatform Gradle plugin handles signing natively. In `build.gradle.kts`:

```kotlin
compose.desktop {
    application {
        nativeDistributions {
            macOS {
                bundleID = "tech.arnav.twofac"
                signing {
                    sign.set(true)
                    identity.set("Developer ID Application: Your Name (TEAM_ID)")
                }
                entitlementsFile.set(project.file("macos.entitlements"))
                // notarization config below
            }
        }
    }
}
```

- Docs: [Compose native distributions — signing](https://kotlinlang.org/docs/multiplatform/compose-native-distribution.html)

## 3. Entitlements

The entitlements file (`composeApp/macos.entitlements`) must include keys needed for the hardened runtime with a JVM app:

- `com.apple.security.cs.allow-jit` — required for JVM JIT compilation
- `com.apple.security.cs.allow-unsigned-executable-memory` — required for JVM runtime
- `com.apple.security.cs.disable-library-validation` — may be needed for bundled native libs

Review and adjust based on what the JVM runtime needs. Test signing without unnecessary entitlements first.

- Docs: [Hardened Runtime entitlements](https://developer.apple.com/documentation/security/hardened-runtime)

## 4. Build the DMG

```bash
./gradlew packageDmg
```

This builds the app, signs it with your Developer ID, and packages it into a DMG. The output is in `composeApp/build/compose/binaries/main/dmg/`.

## 5. Notarize

Notarization lets macOS Gatekeeper verify the app is from an identified developer and hasn't been tampered with.

### Option A: Via Gradle (if notarization tasks are configured)

```bash
./gradlew notarizeDmg
```

Configure credentials in `build.gradle.kts` or via environment variables:

```kotlin
macOS {
    notarization {
        appleID.set("your@email.com")
        password.set("@keychain:NOTARIZATION_PASSWORD")  // app-specific password
        teamID.set("YOUR_TEAM_ID")
    }
}
```

### Option B: Manual with `notarytool`

1. **Create an app-specific password** at [appleid.apple.com](https://appleid.apple.com) → Sign-In and Security → App-Specific Passwords
2. Store credentials:
   ```bash
   xcrun notarytool store-credentials "notary-profile" \
     --apple-id your@email.com \
     --team-id YOUR_TEAM_ID \
     --password <app-specific-password>
   ```
3. Submit for notarization:
   ```bash
   xcrun notarytool submit TwoFac.dmg --keychain-profile "notary-profile" --wait
   ```
4. Staple the ticket to the DMG:
   ```bash
   xcrun stapler staple TwoFac.dmg
   ```

- Docs: [Notarizing macOS software before distribution](https://developer.apple.com/documentation/security/notarizing_macos_software_before_distribution)

## 6. Verify

```bash
# Verify code signature
codesign --verify --deep --strict --verbose=2 /path/to/TwoFac.app

# Verify Gatekeeper acceptance
spctl -a -t exec -vv /path/to/TwoFac.app

# Verify notarization staple
stapler validate /path/to/TwoFac.dmg
```

## 7. Distribute

- Host the notarized, stapled DMG on your website, GitHub Releases, or other download platform
- Users can download and open without Gatekeeper warnings
- Consider [Sparkle](https://sparkle-project.org/) for auto-updates

---

## Optional: Mac App Store Distribution

If you also want to distribute via the Mac App Store:

1. **Certificate**: Use an **Apple Distribution** certificate (not Developer ID)
2. **Provisioning profile**: Create a Mac App Store provisioning profile
3. **Sandboxing**: Required — add `com.apple.security.app-sandbox` entitlement
4. **Build**: `./gradlew packageReleasePkg` (produces a `.pkg` installer)
5. **Upload**: Via [App Store Connect](https://appstoreconnect.apple.com) or Transporter
6. **Review**: Apple reviews the app (stricter than notarization alone)

Note: JVM apps may face additional sandboxing challenges. See the Compose Desktop reference for App Store-specific configuration.

- Docs: [Distribute apps on Mac App Store](https://developer.apple.com/macos/submit/)
- Docs: [App Store Connect help](https://developer.apple.com/help/app-store-connect/)
- Blog: [Publishing Compose Desktop to Mac App Store](https://www.marcogomiero.com/posts/2024/compose-macos-app-store/)

## Quick Reference Links

| Resource | URL |
|----------|-----|
| Apple Developer ID | https://developer.apple.com/developer-id/ |
| Notarizing macOS software | https://developer.apple.com/documentation/security/notarizing_macos_software_before_distribution |
| Hardened Runtime | https://developer.apple.com/documentation/security/hardened-runtime |
| Compose native distributions | https://kotlinlang.org/docs/multiplatform/compose-native-distribution.html |
| App Store Connect | https://appstoreconnect.apple.com |
| Sparkle (auto-updates) | https://sparkle-project.org/ |
