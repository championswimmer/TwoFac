---
name: Desktop Secure Unlock Plan
status: Planned
progress:
  - "[x] Phase 0 - Research platform APIs, constraints, and candidate libraries"
  - "[x] Phase 0.5 - Validate research assumptions with independent verification"
  - "[ ] Phase 1 - Finalize desktop secure-unlock product contract and UX copy"
  - "[ ] Phase 2 - Build macOS native helper + biometric backend"
  - "[ ] Phase 3 - Build Windows secure-storage backend and Hello consent/helper spike"
  - "[ ] Phase 4 - Build Linux Secret Service backend and polkit auth gate"
  - "[ ] Phase 5 - Wire desktop DI, settings, and unlock flows"
  - "[ ] Phase 6 - Package, sign, and manually validate on all desktop OSes"
---

# Desktop Secure Unlock Plan

## Goal

Allow Desktop users to opt into secure vault unlock so the app can remember the vault passkey without storing it in plaintext, while preserving the current shared unlock model:

1. The vault passkey remains the source of truth.
2. Secure unlock is opt-in only.
3. Manual passkey entry always remains the fallback.
4. `sharedLib` stays platform-agnostic.

This should feel similar to mobile, but the implementation must be honest about desktop platform differences instead of forcing false parity.

## Short answer from research

### Can we do it securely?

**macOS:** Yes. This is the closest desktop equivalent to iOS. Apple supports biometric-gated Keychain items through Security + LocalAuthentication. This is the strongest and cleanest desktop target.

**Windows:** Partially. Windows absolutely supports secure at-rest storage for app secrets, but Windows Hello is primarily exposed as a key/authentication system, not as a simple "store arbitrary secret and require biometric every time you read it" primitive. A production plan should start with secure storage first, then add an explicit Windows Hello consent/helper path behind a feature flag.

**Linux:** Partially, and with weaker portability guarantees. Linux desktops support secure secret storage via Secret Service (`gnome-keyring`, KWallet-compatible implementations), and biometrics are commonly routed via PAM/fprintd, but there is no single cross-distro, cross-desktop equivalent of iOS Keychain or Android Keystore biometric-gated secret retrieval.

## Recommended product contract

Do **not** promise identical semantics across desktop OSes.

Instead, define a desktop secure-unlock contract with three support tiers:

1. **Tier A - Biometric/user-presence protected secret retrieval**
   - macOS production target
2. **Tier B - Secure OS-backed storage + explicit OS auth gate**
   - Windows target
3. **Tier C - Secure OS-backed storage bound to desktop session/keyring state**
   - Linux target

UI copy should stay generic (`Secure unlock`, `Use device authentication`, `Unlock with system authentication`) and avoid claiming biometrics everywhere.

## Architecture recommendation

### 1. Keep common contracts generic

Reuse the existing `SessionManager` / `SecureSessionManager` direction and add a desktop-specific backend seam only in `desktopMain`.

Recommended shape:

```kotlin
interface DesktopSecureUnlockBackend {
    fun isAvailable(): Boolean
    fun isSecureUnlockAvailable(): Boolean
    fun supportsStrongUserPresence(): Boolean
    fun currentMode(): DesktopSecureUnlockMode
    suspend fun getSavedPasskey(): String?
    fun savePasskey(passkey: String)
    fun clearPasskey()
    suspend fun enrollPasskey(passkey: String): Boolean
}

enum class DesktopSecureUnlockMode {
    NONE,
    MACOS_KEYCHAIN_BIOMETRIC,
    WINDOWS_SECURE_STORAGE,
    WINDOWS_SECURE_STORAGE_WITH_CONSENT,
    LINUX_SECRET_SERVICE,
}
```

Then expose it to the rest of the app through the already-generic `SecureSessionManager` contract so `commonMain` does not become desktop-API aware.

### 2. Prefer OS secret stores over app-managed plaintext

The desktop implementation should never persist the raw vault passkey in app files or plain preferences.

Preferred storage model:

1. **macOS**: store the passkey directly in Keychain with access-control flags.
2. **Windows**: store either the passkey or a wrapping secret in a user-scoped OS-protected store (DPAPI or Credential Manager).
3. **Linux**: store the passkey in Secret Service / system keyring.

Only fall back to app-managed encrypted blobs when the native API cannot securely hold the secret itself.

### 3. Treat "device auth prompt" and "secret storage" as separate capabilities

Desktop platforms do not expose the same combined primitive everywhere.

The implementation should model:

1. **secure storage available**
2. **explicit user-presence prompt available**
3. **strong OS-enforced binding between prompt and secret retrieval**

macOS can satisfy all three cleanly.
Windows and Linux cannot be assumed to.

## Platform research and implementation direction

### macOS

#### Recommended APIs

1. `SecAccessControlCreateWithFlags`
2. `SecItemAdd`
3. `SecItemCopyMatching`
4. `SecItemDelete`
5. Optional `LAContext` customization through `kSecUseAuthenticationContext`

#### Why this is the best target

Apple explicitly supports protecting Keychain items with Face ID / Touch ID. Keychain Services automatically uses LocalAuthentication when the item is configured with access-control flags. This is very close to the current iOS model already used in the repo.

#### Recommended design

Store a generic-password Keychain item for the vault passkey with:

1. `kSecAttrAccessControl`
2. `kSecAttrAccessibleWhenUnlockedThisDeviceOnly` or `kSecAttrAccessibleWhenPasscodeSetThisDeviceOnly`
3. `userPresence` or `biometryCurrentSet`

Read it using `SecItemCopyMatching` with `kSecUseOperationPrompt` so the OS shows the reason string.

#### JVM integration recommendation

**Primary approach: custom native helper (.dylib)**

The biometric-protected Keychain flow requires the **modern** `SecItem*` APIs (`SecItemAdd`, `SecItemCopyMatching`, `SecItemDelete`) with `SecAccessControlCreateWithFlags`. These APIs accept `CFDictionary` parameters, which require constructing CoreFoundation objects.

While JNA does have CoreFoundation bindings (`com.sun.jna.platform.mac.CoreFoundation`), building the complex `CFDictionary` queries with access-control flags, operation-prompt strings, and result-type keys is significantly more awkward than a native helper.

The recommended approach is a **small native C or Swift helper (.dylib)** that exposes simple C functions:

```c
// Example helper API surface
int twofac_keychain_store(const char* passkey, int passkey_len);
int twofac_keychain_retrieve(char* buffer, int buf_len, int* out_len);
int twofac_keychain_delete(void);
int twofac_keychain_is_available(void);
```

The helper internally handles all `SecAccessControl` + `SecItem*` + CoreFoundation logic and exposes a flat C ABI that JNA can call trivially via `Native.load()`.

**Why not `jkeychain`?** The [`jkeychain`](https://github.com/davidafsilva/jkeychain) library (pt.davidafsilva.apple:jkeychain) uses the **legacy** Keychain APIs (`SecKeychainAddGenericPassword`, `SecKeychainFindGenericPassword`) which are part of the file-based keychain system. These legacy APIs do **not** support `SecAccessControl` biometric flags. We need the modern Data Protection Keychain APIs instead.

**Fallback approach:** If maintaining a native .dylib proves burdensome, pure JNA calling `SecItemAdd`/`SecItemCopyMatching` directly through CoreFoundation bindings is theoretically possible but significantly more complex to implement and test.

#### Entitlements and signing

Non-sandboxed codesigned macOS apps (like jpackage-produced .app bundles) **can** use the Data Protection Keychain with `SecAccessControlCreateWithFlags` and `.userPresence` / `.biometryCurrentSet` flags. Key findings:

1. **No App Sandbox entitlement required** — non-sandboxed apps have access to the Data Protection keychain
2. **No `NSFaceIDUsageDescription`** needed — macOS uses Touch ID (not Face ID), and Touch ID prompts via Keychain don't require this plist key
3. **Codesigning is required** — the app must be properly codesigned for Keychain access to work
4. The existing DMG/notarization pipeline already codesigns, so the native .dylib just needs to be included in the bundle and signed alongside the app

#### Caveats

1. Reads may block and prompt; keep them off the UI thread.
2. The native .dylib must be built as a universal binary (x86_64 + arm64) and bundled inside the .app.
3. Packaging/signing matters. The app already ships DMG builds and has notarization-related tasks, so the helper .dylib must be signed and included correctly.
4. Handle biometric re-enrollment or item invalidation by falling back to manual passkey entry and re-enrollment.
5. Touch ID availability should be detected at runtime — not all Macs have Touch ID hardware.

#### Recommendation

**Implement first.** This is the highest-confidence platform and offers the closest desktop parity with mobile secure unlock.

### Windows

#### Relevant APIs

1. `CryptProtectData` / `CryptUnprotectData` (DPAPI)
2. Credential Manager (`CredWrite`, `CredRead`)
3. `UserConsentVerifier` / desktop interop (`RequestVerificationForWindowAsync`)
4. `KeyCredentialManager`

#### What each API is good for

1. **DPAPI**
   - Strong user-scoped at-rest encryption
   - Very suitable for protecting local app secrets
   - Not equivalent to "prompt for Windows Hello every read"
2. **Credential Manager**
   - Native Windows credential store
   - Good for persistence and account-scoped secret storage
   - Also not equivalent to per-access Windows Hello enforcement
3. **UserConsentVerifier**
   - Can trigger explicit device-auth verification (PIN / Windows Hello)
   - Works as an auth gate, not as the secure store itself
4. **KeyCredentialManager**
   - Creates app/user-scoped asymmetric credentials protected by Windows Hello
   - Best for identity/challenge signing
   - Not a simple arbitrary-secret vault API

#### Recommended design

Ship Windows in two phases:

##### Phase W1 - production storage

Use **DPAPI** as the secure-storage primitive for the saved vault passkey (or a small wrapping secret), bound to the current Windows user.

This gives strong at-rest protection immediately and is practical from a JVM app via JNA.

##### Phase W2 - optional consent gate

Add a native Windows helper that uses **UserConsentVerifier** to show the Windows Hello / PIN verification dialog before unprotecting the DPAPI blob.

This improves UX parity, but we should document it accurately:

1. the secret is OS-protected at rest by DPAPI
2. the Hello/PIN step is an explicit gate before release
3. this is **not** as strong or as cleanly bound as macOS Keychain access-control semantics

##### Phase W3 - research spike only

Investigate whether a stronger Hello-bound design is worth the complexity:

1. packaged desktop helper
2. WinRT interop
3. possible `KeyCredentialManager`-based local unwrap/signing workflow

Do not block the main plan on this spike.

#### JVM integration recommendation

1. Use **JNA** for DPAPI — `Crypt32Util.cryptProtectData()` / `cryptUnprotectData()` are already included in `jna-platform` and ready to use out of the box.
2. **CredWrite/CredRead are NOT included** in `jna-platform`. If Credential Manager is needed, custom JNA bindings for `advapi32.dll` / `wincred.h` functions must be written, or use a separate library.
3. Use a small **C# / WinRT** or **C++/WinRT** sidecar helper for `UserConsentVerifier` if needed. `RequestVerificationForWindowAsync` with HWND interop is confirmed to work from unpackaged desktop apps.
4. Avoid betting the initial rollout on deep WinRT interop from Kotlin/JVM directly.

**Note on DPAPI security:** JNA's `Crypt32Util` has a known issue ([JNA #1362](https://github.com/java-native-access/jna/issues/1362)) where wrapper methods do not securely zero sensitive data from memory after use. For production, consider using the lower-level `Crypt32` bindings and manually zeroing byte arrays after use.

#### Caveats

1. Windows Hello APIs are not shaped like a Keychain secret API.
2. Desktop interop for consent flows needs an HWND-aware helper.
3. Packaged/unpackaged app behavior must be tested explicitly.
4. DPAPI binds secrets to the current Windows user profile — they cannot be transferred across machines or users.

#### Recommendation

**Implement secure storage first, Hello gating second.** Do not market Windows as identical to macOS/iOS unless the stronger helper path proves reliable.

### Linux

#### Relevant APIs and services

1. Secret Service spec (`org.freedesktop.secrets`)
2. `gnome-keyring` / compatible Secret Service implementations
3. KWallet compatibility paths where practical
4. `fprintd`
5. PAM (`pam_fprintd`, `pam_gnome_keyring`, distro login/session config)

#### What is realistic

Linux can securely store secrets in the user session keyring via Secret Service. This is the correct default for a Linux desktop app.

What Linux does **not** offer cleanly is one universal app-level biometric-protected secret-retrieval API across GNOME, KDE, X11, Wayland, and distro-specific PAM stacks.

#### Security caveat: CVE-2018-19358 (CVSS 7.8 HIGH)

**Any D-Bus application can read secrets from an unlocked Secret Service keyring.** This is a fundamental design limitation of the Secret Service specification itself — not a bug in any particular implementation. When `gnome-keyring` or KWallet's Secret Service is unlocked (which happens automatically on user login), any process running in the user session can call `GetSecret` on any item without additional authentication.

**Implications for TwoFac:**
- Secrets stored via Secret Service are protected at rest (when the keyring is locked) but NOT protected against other apps running in the same user session
- This makes Linux Tier C inherently weaker than macOS Tier A (Keychain biometric binding) or Windows Tier B (DPAPI user-profile binding)
- We should document this honestly in the settings UI and not present Linux secure unlock as equivalent to macOS/iOS biometric protection
- This is still significantly better than storing passkeys in plaintext in app files

#### Recommended design

##### Phase L1 - production storage

Use **Secret Service** as the default Linux secure-storage backend.

Recommended JVM options:

1. **`dbus-java`** ([hypfvieh/dbus-java](https://github.com/hypfvieh/dbus-java)) as the primary option — it's actively maintained and gives us full control over D-Bus communication
2. `de.swiesend:secret-service` as a convenience option **with caveats** — the last release was Nov 2023 (v2.0.1-alpha), and maintenance status is uncertain. If it becomes abandoned, we'd need to migrate to direct `dbus-java` usage anyway

**KWallet compatibility:** Confirmed that KWallet supports the Secret Service D-Bus spec natively since v5.97 (Feb 2023). This means our Secret Service integration works on both GNOME (gnome-keyring) and KDE (KWallet) without separate KWallet-specific code.

##### Phase L2 - platform capability detection

Detect whether a Secret Service daemon is available and whether the default collection can be used. If not, disable secure unlock and keep manual passkey entry only.

##### Phase L3 - optional polkit auth gate (experimental)

Use **polkit** as an optional auth gate before retrieving secrets from Secret Service.

Polkit (`org.freedesktop.PolicyKit1.Authority.CheckAuthorization`) is a better fit than direct PAM for desktop app-level auth gating because:

1. It automatically shows desktop-appropriate auth dialogs via registered authentication agents
2. It uses PAM under the hood, so it can leverage fprintd for fingerprint auth where configured
3. It's callable from JVM via `dbus-java` (D-Bus system bus call)
4. No root/setuid needed — any unprivileged app can request authorization

This would make Linux Tier C flow: polkit auth gate → retrieve secret from Secret Service.

Implementation requires:
- A custom polkit `.policy` XML file installed to `/usr/share/polkit-1/actions/`
- D-Bus call to `CheckAuthorization` from `dbus-java`
- Handle the case where no polkit agent is running (headless/minimal installs)

**Keep polkit behind a feature flag** until tested across Ubuntu/Fedora/Arch with GNOME and KDE.

##### Phase L4 - PAM/fprintd direct (research only)

Treat **PAM/fprintd** as a separate experimental track, not a baseline feature.

Reasons:

1. PAM is a system authentication framework, not a portable app-secret API.
2. fprintd is a fingerprint daemon, but app-level prompting and policy vary widely.
3. Depending on PAM stack configuration may require native helpers, system integration, or admin changes that are inappropriate for a baseline desktop feature.
4. Polkit (Phase L3) already provides a standardized way to trigger PAM-backed auth from desktop apps.

#### Recommendation

**Implement Secret Service support first, add polkit auth gate as an experimental enhancement, and keep direct PAM/fprintd as research-only.**

## Library/tooling recommendations

### Strong candidates

1. **JNA** (`/java-native-access/jna`) + **jna-platform**
   - Windows DPAPI: `Crypt32Util.cryptProtectData()` / `cryptUnprotectData()` — ready to use
   - macOS: CoreFoundation bindings exist but are complex for `SecItem*` queries — prefer native helper
   - Well-maintained, widely used
2. **dbus-java** ([hypfvieh/dbus-java](https://github.com/hypfvieh/dbus-java))
   - Actively maintained Linux D-Bus integration
   - Covers both Secret Service and polkit D-Bus APIs
   - Preferred over `de.swiesend:secret-service` for long-term maintenance

### Worth evaluating

1. **`de.swiesend:secret-service`** ([swiesend/secret-service](https://github.com/swiesend/secret-service))
   - Higher-level Secret Service client for Linux
   - ⚠️ Last release Nov 2023 (v2.0.1-alpha), maintenance status uncertain
   - Good for a quick start but may need replacement with direct `dbus-java` if abandoned
2. **Java 21 FFM (Foreign Function & Memory API)**
   - Alternative to JNA for new native bindings
   - Available since JDK 21 (which this project uses)
   - Worth evaluating for the macOS native helper interop if we want to avoid bundling a separate .dylib

### Important non-recommendation

Do **not** rely on generic cross-platform keyring wrappers (e.g., `java-keyring`) as the primary abstraction if they hide platform differences we explicitly care about. Most cross-platform keyring libraries are good at storage, but not at exposing macOS biometric access-control flags or a nuanced Windows/Linux support matrix.

Do **not** use `jkeychain` (`pt.davidafsilva.apple:jkeychain`) — it wraps the legacy `SecKeychainAddGenericPassword` APIs which do NOT support biometric access control.

## Rollout order

### Phase 1 - finalize the app contract

1. Decide UI copy and support-tier wording.
2. Define what "secure unlock ready" means on desktop.
3. Decide whether Windows/Linux can expose the same toggle label as macOS.

### Phase 2 - desktop session/backend scaffolding

1. Add `DesktopSecureUnlockBackend` in `desktopMain`
2. Add a desktop `SecureSessionManager` implementation
3. Centralize OS detection and capability reporting
4. Add a small result model for:
   - available
   - enrolled
   - protected-storage-only
   - strong-user-presence-supported

### Phase 3 - macOS backend

1. Build a small native C or Swift helper (.dylib) wrapping modern `SecItem*` APIs with `SecAccessControl` biometric flags
2. Expose a flat C ABI (store/retrieve/delete/isAvailable) callable via JNA `Native.load()`
3. Build the helper as a universal binary (x86_64 + arm64)
4. Implement `DesktopSecureUnlockBackend` for macOS using the helper
5. Wire clear/re-enroll/failure handling
6. Add helper to the DMG build pipeline with codesigning
7. Manually test signed DMG build on a Touch ID-capable Mac

### Phase 4 - Windows backend

1. Add DPAPI passkey encryption using JNA's existing `Crypt32Util.cryptProtectData()` / `cryptUnprotectData()` — no custom bindings needed
2. Implement secure passkey persistence using DPAPI (user-scoped)
3. Manually zero sensitive byte arrays after use (mitigate JNA #1362 memory cleanup concern)
4. Add capability checks for `UserConsentVerifier` helper presence
5. Build small Windows native helper (C#/WinRT) for consent prompt, if the phase is greenlit
6. Gate this path behind a desktop feature flag until reliability is proven

### Phase 5 - Linux backend

1. Add Secret Service integration using `dbus-java` (or `de.swiesend:secret-service` for quick start, with migration plan)
2. Detect runtime support and locked/unlocked state
3. Implement save/read/delete flows
4. KWallet confirmed compatible with Secret Service spec since v5.97 — no separate KWallet code needed
5. Document CVE-2018-19358 limitation in settings UI (secrets readable by any session app when keyring is unlocked)
6. Optionally add polkit auth gate as experimental enhancement behind feature flag
7. Keep direct PAM/fprintd as a follow-up investigation

### Phase 6 - shared desktop UX wiring

1. Add desktop-only settings controls
2. Reuse current common unlock flows through `SecureSessionManager`
3. Ensure failure/cancel always falls back to manual passkey entry
4. Ensure disabling secure unlock clears persisted secret material everywhere

### Phase 7 - packaging and validation

1. macOS: signed/notarized DMG, helper signing if introduced
2. Windows: MSI packaging and native helper distribution
3. Linux: DEB packaging and runtime dependency expectations
4. Manual test matrix:
   - secure unlock enable/disable
   - app restart
   - logout/login
   - biometric unavailable
   - secret store unavailable
   - cancel auth prompt
   - wrong/changed biometric enrollment

## Likely files/modules affected

1. `composeApp/src/desktopMain/kotlin/tech/arnav/twofac/session/...`
2. `composeApp/src/desktopMain/kotlin/tech/arnav/twofac/di/...`
3. `composeApp/src/commonMain/kotlin/tech/arnav/twofac/screens/SettingsScreen.kt`
4. `composeApp/src/commonMain/kotlin/tech/arnav/twofac/viewmodels/AccountsViewModel.kt` if desktop capability reporting needs minor shared wiring
5. `composeApp/build.gradle.kts` for desktop dependencies

## Acceptance criteria

1. Desktop never persists the vault passkey in plaintext.
2. macOS secure unlock uses OS-enforced user presence / biometrics.
3. Windows desktop stores the passkey in an OS-protected store and optionally gates reads behind explicit device authentication.
4. Linux desktop stores the passkey in Secret Service when available and cleanly falls back when unavailable.
5. Shared unlock behavior remains:
   - secure success -> unlock
   - cancel/failure -> prompt manually
   - disable -> clear secure material

## Final recommendation

Proceed with a **phased rollout**:

1. **macOS first** as the "true biometric desktop unlock" target
2. **Windows second** with DPAPI-backed secure storage and an optional Windows Hello consent helper
3. **Linux third** with Secret Service support, while explicitly treating PAM/fprintd as a separate experimental track rather than a guaranteed product feature

This gives us a desktop secure-unlock feature that is genuinely secure, aligned with native platform capabilities, and honest about where desktop OSes do and do not match the mobile model.

## Research references

### Apple

1. https://docs.developer.apple.com/tutorials/data/documentation/LocalAuthentication/accessing-keychain-items-with-face-id-or-touch-id.json
2. https://docs.developer.apple.com/tutorials/data/documentation/security/secaccesscontrolcreatewithflags%28_%3A_%3A_%3A_%3A%29.json

### Microsoft

1. https://learn.microsoft.com/en-us/windows/apps/develop/security/windows-hello
2. https://learn.microsoft.com/en-us/windows/win32/api/dpapi/nf-dpapi-cryptprotectdata
3. https://learn.microsoft.com/en-us/windows/win32/seccng/cng-dpapi
4. https://learn.microsoft.com/en-us/windows/win32/api/wincred/nf-wincred-credwritew
5. https://learn.microsoft.com/en-us/uwp/api/windows.security.credentials.keycredentialmanager
6. https://learn.microsoft.com/en-us/uwp/api/windows.security.credentials.ui.userconsentverifier

### Linux

1. https://specifications.freedesktop.org/secret-service-spec/latest-single/
2. https://fprint.freedesktop.org/
3. https://www.man7.org/linux/man-pages/man8/pam.8.html

### Library/tooling references

1. https://context7.com/java-native-access/jna
2. https://github.com/hypfvieh/dbus-java
3. https://github.com/swiesend/secret-service
4. https://github.com/davidafsilva/jkeychain (evaluated — uses legacy APIs, NOT suitable for biometric)
5. https://java-native-access.github.io/jna/4.2.1/com/sun/jna/platform/win32/Crypt32Util.html
6. https://github.com/java-native-access/jna/issues/1362 (DPAPI memory cleanup concern)
7. https://github.com/java-native-access/jna/blob/master/contrib/platform/src/com/sun/jna/platform/mac/CoreFoundation.java

### Security references

1. CVE-2018-19358 — Secret Service D-Bus secrets readable by any session app (CVSS 7.8 HIGH)
2. https://www.freedesktop.org/software/polkit/docs/latest/ref-dbus-api.html (polkit D-Bus API)
