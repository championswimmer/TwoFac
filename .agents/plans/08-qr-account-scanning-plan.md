---
name: QR Account Scanning (Camera + Clipboard)
status: In Progress
progress:
  - "[x] Phase 0 - Lock QR stack and DI contract shape"
  - "[x] Phase 1 - Add common QR reader contracts and result model"
  - "[x] Phase 2 - Implement Android+iOS camera QR readers with permission flows"
  - "[x] Phase 3 - Implement Desktop+Web clipboard image QR readers"
  - "[x] Phase 4 - Wire Add Account UX for scan/paste/manual entry"
  - "[ ] Phase 5 - Register per-platform DI modules and host configuration"
  - "[ ] Phase 6 - Add tests and run cross-target validation"
---

# QR Account Scanning (Camera + Clipboard)

## Goal

Add new account onboarding through QR codes with this platform scope:

1. **Android + iOS**: scan QR using camera.
2. **Web + Desktop**: paste an image from clipboard, decode QR from pasted image.
3. **CLI**: no changes for now.

The implementation should follow existing DI patterns in this repo by introducing a generic `QRCodeReader` contract and specialized `CameraQRCodeReader` and `ClipboardQRCodeReader` platform services.

## Locked scope and defaults

1. Keep existing manual `otpauth://...` entry as a fallback path.
2. Reuse existing `AccountsViewModel.addAccount(uri, passkey)` and `TwoFacLib.addAccount(uri)` flow; QR work only resolves a valid URI.
3. Bind readers as **optional** dependencies (same `getOrNull(...)` pattern already used in settings/backup/session flows), so unsupported targets degrade gracefully.
4. Camera flow is **mobile-only** (Android+iOS).
5. Clipboard-image flow is **web+desktop-only**.
6. No `cliApp` QR feature in this plan.

## Research summary

### 1) Kotlin Multiplatform QR scanning library choice

#### Candidate A: KScan
- Compose Multiplatform barcode scanner for Android+iOS.
- Readme explicitly states platform backends: Android uses ML Kit, iOS uses AVFoundation.
- Latest release is recent (`0.7.0`, 2026-02-28), Apache-2.0.
- Repo activity/popularity signal is stronger than alternatives reviewed.

#### Candidate B: EasyQRScan
- Compose Multiplatform scanner for Android+iOS.
- Also recent (`0.7.1`, 2026-02-20), Apache-2.0.
- Readme explicitly notes implementation is “rather rudimentary”.

#### Candidate C: zxing-cpp Kotlin/Native wrapper
- Strong decoder library for Kotlin/Native image decoding.
- Useful for raw image decode, but not a drop-in camera UX solution and includes native distribution/linking complexity on some targets.
- Does not solve browser/wasm flow directly.

**Recommendation for this plan:** use **KScan** as the camera scanning foundation for Android+iOS, and keep desktop/web pasted-image decoding separate using platform-appropriate decoders.

### 2) Camera permission research (Android+iOS)

#### Android
- Runtime permissions are required for dangerous permissions on API 23+; camera must be requested in-context at runtime.
- `ActivityResultContracts.RequestPermission` is the recommended AndroidX contract path for permission requests.
- Manifest must include `android.permission.CAMERA`.

#### iOS
- Camera scanning should be implemented through AVFoundation capture metadata output (`AVCaptureMetadataOutput`) pipeline.
- Access must be requested via `AVCaptureDevice.requestAccess(for: .video, ...)`.
- `NSCameraUsageDescription` is required in `Info.plist`; missing usage description causes runtime exception when capture access is requested.

### 3) Web/desktop pasted-image research

#### Web (Wasm)
- `navigator.clipboard.read()` (Async Clipboard API) supports reading `ClipboardItem` entries and retrieving image blobs via `getType(...)`.
- Clipboard read is restricted to secure contexts and user-permitted/user-gesture flows.
- `paste` event fallback (`ClipboardEvent.clipboardData`) and `DataTransfer.files` can be used to capture pasted image files.
- `jsQR` is a pure JavaScript QR decoder taking RGBA `Uint8ClampedArray` + width + height, which maps well to canvas/ImageData processing in wasm.

#### Desktop (JVM)
- Clipboard image retrieval can be done through AWT system clipboard:
  - `Toolkit.getSystemClipboard()`
  - `Clipboard.isDataFlavorAvailable(DataFlavor.imageFlavor)`
  - `Clipboard.getData(DataFlavor.imageFlavor)`
- `DataFlavor.imageFlavor` maps to `java.awt.Image`.
- ZXing `MultiFormatReader` is the canonical Java entrypoint for decoding barcode/QR from image data; suitable for JVM desktop decode path.

## Proposed architecture (DI-first)

### Common contracts (`composeApp/commonMain`)

Create a new QR package (for example `tech.arnav.twofac.qr`) with:

1. `QRCodeReader` (base contract)
2. `CameraQRCodeReader : QRCodeReader`
3. `ClipboardQRCodeReader : QRCodeReader`
4. `QRCodeReadResult` sealed result model (success with decoded URI, canceled, permission denied, unsupported, decode failure)

Design note: keep result/error mapping platform-agnostic in common code and isolate API/platform details to platform source sets.

### Platform implementations

1. **Android (`androidMain`)**
   - `AndroidCameraQRCodeReader` using KScan scanner composable/backend.
   - Permission gate using Android runtime permission flow.
2. **iOS (`iosMain`)**
   - `IosCameraQRCodeReader` using KScan iOS backend (AVFoundation under the hood).
   - Permission gate via `AVCaptureDevice.requestAccess`.
3. **Desktop (`desktopMain`)**
   - `DesktopClipboardQRCodeReader`:
     - read clipboard image via AWT clipboard APIs
     - decode via ZXing
4. **Wasm (`wasmJsMain`)**
   - `WasmClipboardQRCodeReader`:
     - primary: Async Clipboard API (`navigator.clipboard.read`)
     - fallback: paste-event image extraction
     - decode with `jsQR`

### UI integration strategy

`AddAccountScreen` should evolve from manual-only to source-aware:

1. Source actions:
   - manual URI entry (existing)
   - camera scan (if `CameraQRCodeReader` exists)
   - paste image (if `ClipboardQRCodeReader` exists)
2. On successful decode:
   - populate URI field (and optionally allow one-tap add)
   - keep passkey requirement behavior unchanged
3. Keep clear error states:
   - permission denied
   - no image in clipboard
   - invalid/unsupported QR payload
   - decode failure

## Detailed implementation roadmap

### Phase 0 - Lock QR stack and DI contract shape

Status: ✅ Completed

1. Finalize implementation choice:
   - KScan for Android+iOS camera scanning ✅
   - jsQR for web paste decode ✅
   - ZXing for desktop paste decode ✅
2. Lock contract boundaries:
   - `QRCodeReader`, `CameraQRCodeReader`, `ClipboardQRCodeReader` ✅
   - common result model ✅
3. Lock fallback behavior for unsupported targets (optional DI + disabled UI action) ✅
4. Implemented in:
   - `composeApp/src/commonMain/kotlin/tech/arnav/twofac/qr/QRCodeReader.kt`
   - `composeApp/src/commonMain/kotlin/tech/arnav/twofac/qr/QRCodeReadResult.kt`
   - `composeApp/src/commonMain/kotlin/tech/arnav/twofac/di/modules.kt`
   - `composeApp/src/commonMain/kotlin/tech/arnav/twofac/viewmodels/AccountsViewModel.kt`
   - `composeApp/build.gradle.kts`
   - `gradle/libs.versions.toml`

### Phase 1 - Add common QR reader contracts and result model

Status: ✅ Completed

1. Add QR contract/result types in `composeApp/src/commonMain/.../qr/`. ✅
2. Add helper(s) in common code to validate decoded strings as `otpauth://...` before sending to `addAccount`. ✅
3. Keep business logic in sharedLib unchanged (decode only feeds existing URI workflow). ✅
4. Implemented in:
   - `composeApp/src/commonMain/kotlin/tech/arnav/twofac/qr/QRCodePayloadValidation.kt`
   - `composeApp/src/commonTest/kotlin/tech/arnav/twofac/qr/QRCodePayloadValidationTest.kt`

### Phase 2 - Implement Android+iOS camera QR readers with permission flows

1. Add mobile camera reader implementations under `androidMain` and `iosMain`.
2. Android:
   - ensure `android.permission.CAMERA` in manifest
   - wire runtime request flow with AndroidX activity-result contract
3. iOS:
   - add `NSCameraUsageDescription` in `iosApp/iosApp/Info.plist`
   - request camera authorization before starting scan
4. Normalize platform scanner outputs to common `QRCodeReadResult`.

### Phase 3 - Implement Desktop+Web clipboard image QR readers

1. Desktop:
   - read image from AWT clipboard (`DataFlavor.imageFlavor`)
   - convert to decode input and decode via ZXing
2. Web:
   - implement clipboard image acquisition via Async Clipboard API
   - add paste-event fallback path
   - decode image data via `jsQR`
3. Normalize failures (no image, unsupported mime type, decode failure) into common result model.

### Phase 4 - Wire Add Account UX for scan/paste/manual entry

1. Update `AddAccountScreen` with scan/paste actions that appear only when corresponding readers are available.
2. Keep manual URI path as always-available fallback.
3. On successful read:
   - fill URI field (or directly submit)
   - preserve existing unlock/passkey behavior
4. Add user-visible messages for permission and decode errors.

### Phase 5 - Register per-platform DI modules and host configuration

1. Add reader bindings to platform DI modules:
   - Android: camera reader
   - iOS: camera reader
   - Desktop: clipboard reader
   - Wasm: clipboard reader
2. Update platform entry points to include new modules:
   - `androidApp` application init modules list
   - `MainViewController` module list
   - desktop and wasm entry module lists
3. Keep `viewModelModule` and shared `initKoin` patterns intact.

### Phase 6 - Add tests and run cross-target validation

1. Add common tests for URI validation/result mapping helpers.
2. Add desktop reader tests around clipboard-image decode path where feasible.
3. Add wasm unit/integration tests for paste/image decode adapters where feasible.
4. Generate deterministic QR PNG fixtures with `qrencode` and commit them as test resources for CI:
   - `qrencode -o composeApp/src/commonTest/resources/qr/valid-totp.png "otpauth://totp/Example:alice@example.com?secret=JBSWY3DPEHPK3PXP&issuer=Example"`
   - `qrencode -o composeApp/src/commonTest/resources/qr/invalid-http.png "https://example.com/not-otpauth"`
   - Optional stability flags for fixture generation: `-s 8 -m 2 -l M`
5. Use generated fixtures in desktop+wasm decode tests to validate full decode + payload validation behavior in CI.
6. Manual smoke validation matrix:
    - Android: permission deny/allow + scan success/failure
    - iOS: permission deny/allow + scan success/failure
    - Desktop: paste image with/without QR
    - Web: paste image with Async API and paste-event fallback
7. Run compose module compile/tests for changed targets.

## Files likely impacted

- `composeApp/src/commonMain/kotlin/tech/arnav/twofac/screens/AddAccountScreen.kt`
- `composeApp/src/commonMain/kotlin/tech/arnav/twofac/di/modules.kt` (if shared injection helpers are added)
- `composeApp/src/commonMain/kotlin/tech/arnav/twofac/qr/*` (new)
- `composeApp/src/androidMain/kotlin/tech/arnav/twofac/di/AndroidModules.kt`
- `composeApp/src/androidMain/kotlin/tech/arnav/twofac/qr/*` (new)
- `androidApp/src/main/AndroidManifest.xml`
- `androidApp/src/main/kotlin/tech/arnav/twofac/TwoFacApplication.kt`
- `composeApp/src/iosMain/kotlin/tech/arnav/twofac/di/IosModules.kt`
- `composeApp/src/iosMain/kotlin/tech/arnav/twofac/qr/*` (new)
- `composeApp/src/iosMain/kotlin/tech/arnav/twofac/MainViewController.kt`
- `iosApp/iosApp/Info.plist`
- `composeApp/src/desktopMain/kotlin/tech/arnav/twofac/di/DesktopModules.kt`
- `composeApp/src/desktopMain/kotlin/tech/arnav/twofac/qr/*` (new)
- `composeApp/src/wasmJsMain/kotlin/tech/arnav/twofac/di/WasmModules.kt`
- `composeApp/src/wasmJsMain/kotlin/tech/arnav/twofac/qr/*` (new)
- `composeApp/src/wasmJsMain/kotlin/tech/arnav/twofac/qr/interop/*` (new)
- `composeApp/src/commonTest/resources/qr/*` (new fixtures generated via `qrencode`)
- `composeApp/build.gradle.kts`
- `gradle/libs.versions.toml`

## Explicitly out of scope

1. CLI QR scanning support (`cliApp`).
2. Bulk QR import workflow (multi-QR batch from album/files) beyond single-image paste.
3. Non-QR barcode onboarding semantics (we only accept valid OTP `otpauth://` payloads).

## References used

### Repository baseline
- `composeApp/src/commonMain/kotlin/tech/arnav/twofac/screens/AddAccountScreen.kt`
- `composeApp/src/commonMain/kotlin/tech/arnav/twofac/viewmodels/AccountsViewModel.kt`
- `composeApp/src/commonMain/kotlin/tech/arnav/twofac/di/modules.kt`
- `composeApp/src/androidMain/kotlin/tech/arnav/twofac/di/AndroidModules.kt`
- `composeApp/src/iosMain/kotlin/tech/arnav/twofac/di/IosModules.kt`
- `composeApp/src/desktopMain/kotlin/tech/arnav/twofac/di/DesktopModules.kt`
- `composeApp/src/wasmJsMain/kotlin/tech/arnav/twofac/di/WasmModules.kt`
- `androidApp/src/main/AndroidManifest.xml`
- `androidApp/src/main/kotlin/tech/arnav/twofac/TwoFacApplication.kt`
- `composeApp/src/iosMain/kotlin/tech/arnav/twofac/MainViewController.kt`
- `iosApp/iosApp/Info.plist`
- `composeApp/build.gradle.kts`
- `gradle/libs.versions.toml`

### External research
- KScan repository: https://github.com/ismai117/KScan
- KScan latest release metadata: https://api.github.com/repos/ismai117/KScan/releases/latest
- KScan repository metadata: https://api.github.com/repos/ismai117/KScan
- EasyQRScan repository: https://github.com/kalinjul/EasyQRScan
- EasyQRScan latest release metadata: https://api.github.com/repos/kalinjul/EasyQRScan/releases/latest
- EasyQRScan repository metadata: https://api.github.com/repos/kalinjul/EasyQRScan
- ZXing-C++ Kotlin/Native wrapper: https://raw.githubusercontent.com/zxing-cpp/zxing-cpp/master/wrappers/kn/README.md
- ML Kit barcode scanning on Android: https://developers.google.com/ml-kit/vision/barcode-scanning/android
- Android runtime permission request flow: https://developer.android.com/training/permissions/requesting
- AndroidX permission contract: https://developer.android.com/reference/androidx/activity/result/contract/ActivityResultContracts.RequestPermission
- Apple `AVCaptureMetadataOutput`: https://docs.developer.apple.com/tutorials/data/documentation/avfoundation/avcapturemetadataoutput.md
- Apple `AVCaptureDevice.requestAccess`: https://docs.developer.apple.com/tutorials/data/documentation/avfoundation/avcapturedevice/requestaccess(for:completionhandler:).md
- Apple `NSCameraUsageDescription`: https://docs.developer.apple.com/tutorials/data/documentation/bundleresources/information-property-list/nscamerausagedescription.md
- web.dev paste images pattern: https://web.dev/patterns/clipboard/paste-images
- MDN Clipboard read: https://developer.mozilla.org/en-US/docs/Web/API/Clipboard/read
- MDN ClipboardItem: https://developer.mozilla.org/en-US/docs/Web/API/ClipboardItem
- MDN ClipboardEvent.clipboardData: https://developer.mozilla.org/en-US/docs/Web/API/ClipboardEvent/clipboardData
- MDN paste event: https://developer.mozilla.org/en-US/docs/Web/API/Element/paste_event
- MDN DataTransfer.files: https://developer.mozilla.org/en-US/docs/Web/API/DataTransfer/files
- jsQR repository/docs: https://github.com/cozmo/jsQR
- Java Toolkit system clipboard: https://docs.oracle.com/en/java/javase/21/docs/api/java.desktop/java/awt/Toolkit.html#getSystemClipboard()
- Java Clipboard API: https://docs.oracle.com/en/java/javase/21/docs/api/java.datatransfer/java/awt/datatransfer/Clipboard.html
- Java DataFlavor API (`imageFlavor`): https://docs.oracle.com/en/java/javase/21/docs/api/java.datatransfer/java/awt/datatransfer/DataFlavor.html
- ZXing `MultiFormatReader` API: https://zxing.github.io/zxing/apidocs/com/google/zxing/MultiFormatReader.html
- `qrencode` CLI usage: `qrencode --help`
