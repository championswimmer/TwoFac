# Fix: `NullPointerException` in `BarcodeScanning.getClient()` on Play Store builds

## Crash signature

```
Exception java.lang.NullPointerException:
  at com.google.mlkit.vision.barcode.internal.zzg.zzb (zzg.java:1)
  at com.google.mlkit.vision.barcode.BarcodeScanning.getClient (BarcodeScanning.java:3)
  at org.ncgroup.kscan.BarcodeAnalyzer.<init> (BarcodeAnalyzer.kt:45)
  at org.ncgroup.kscan.ScannerView_androidKt.ScannerView$lambda$33$0$0 (ScannerView_android.kt:141)
  …
```

Triggered when the user opens the in-app QR scanner (`AndroidCameraQRCodeReader.RenderScanner` → `ScannerView` from KScan → `BarcodeAnalyzer` → `BarcodeScanning.getClient(...)`). Only reproduces on Play Store / release builds, not on local debug builds.

## Root cause

This is a known incompatibility between `com.google.mlkit:barcode-scanning` and **AGP 9 + R8 full mode** (which is the default in AGP 9, since `android.r8.strictFullModeForKeepRules` flipped to `true`).

Project versions confirming the trigger:

- `gradle/libs.versions.toml`
  - `agp = "9.0.1"`
  - `mlkit-barcode-scanning = "17.3.0"`
  - `kscan = "0.7.0"` (KScan internally calls `BarcodeScanning.getClient(...)`)
- `androidApp/build.gradle.kts` has `isMinifyEnabled = true` for the `release` build type.

ML Kit's barcode-scanning AAR does **not** ship `consumer-rules.pro` keep rules for its internal classes (e.g. `com.google.mlkit.vision.barcode.internal.zzg`, `com.google.android.gms.internal.mlkit_vision_barcode_bundled.*`). Under R8 strict full mode those internal types/fields are stripped or renamed, so when KScan calls `BarcodeScanning.getClient(options)` the underlying `zzg.zzb(...)` returns `null` (or accesses a stripped reflective member), producing the NPE in our stack.

References:

- googlesamples/mlkit issue #1007 — "Barcode scanning is not compatible with AGP9" (same NPE in `zzg.zzb`).
- googlesamples/mlkit issue #1018 — `play-services-code-scanner` crashes with R8 full mode (AGP 9+); same class of issue, recommends adding consumer keep rules for `com.google.mlkit.**` and `com.google.android.gms.internal.mlkit_*.**`.
- ML Kit Known Issues page (Google) recommends keep rules for ML Kit internal reflective classes.

In our codebase, `AndroidCameraQRCodeReader` already wraps the call in `runCatching` to surface a `Failed` state, but the failure happens **inside the AndroidView factory** for `ScannerView` (constructor of `BarcodeAnalyzer`), which is not inside our `runCatching`, so the NPE escapes to the Compose runtime and crashes the app.

## Fix

Add ProGuard/R8 keep rules for ML Kit (and the bundled barcode protobufs) so R8 full mode does not strip the classes accessed reflectively by `BarcodeScanning.getClient(...)`.

The ML Kit dependency is declared in `composeApp/build.gradle.kts` (`androidMain` → `libs.mlkit.barcode.scanning`), and `composeApp` already publishes a `consumer-rules.pro` (see `consumerKeepRules.publish = true`). So the right place to put these rules is **`composeApp/consumer-rules.pro`** — they will then automatically apply to `androidApp` (and any other consumer of `:composeApp`).

### Rules to add

Append to `composeApp/consumer-rules.pro`:

```proguard
# ── Google ML Kit (barcode-scanning) ─────────────────────────────────────────
# AGP 9 enables R8 strict full mode by default. ML Kit's barcode-scanning AAR
# does not ship consumer keep rules for its internal reflective classes, which
# causes BarcodeScanning.getClient(...) to throw NullPointerException at
# com.google.mlkit.vision.barcode.internal.zzg.zzb on release builds.
# See: https://github.com/googlesamples/mlkit/issues/1007
#      https://github.com/googlesamples/mlkit/issues/1018
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_barcode.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_barcode_bundled.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_common.** { *; }
-keep class com.google.android.gms.internal.mlkit_common.** { *; }

# Generated proto fields are accessed via reflection by ML Kit at runtime.
-keepclassmembers class * extends com.google.android.gms.internal.mlkit_vision_barcode_bundled.zzeh {
    <fields>;
}

# Native methods (libbarhopper_v3.so) — preserve names to avoid
# UnsatisfiedLinkError after obfuscation.
-keepclasseswithmembernames class * {
    native <methods>;
}
```

### Why these specific rules

- `-keep class com.google.mlkit.**` — the workaround called out in mlkit#1018.
- `-keep class com.google.android.gms.internal.mlkit_vision_barcode_bundled.**` — covers the exact `zzg`/`zzh`/`zzeh` types named in mlkit#1007's stack trace, which match our crash (`com.google.mlkit.vision.barcode.internal.zzg.zzb`).
- The `<fields>` rule on `zzeh` subclasses is the existing rule cited in the AGP 9 incompatibility report; it handles the strict full mode requirement that field keep rules must come from a keep rule, not just from `@Keep`.
- The `native <methods>` rule guards against a follow-on `UnsatisfiedLinkError` from the bundled `libbarhopper_v3.so`.

## Defensive code change (secondary)

The existing probe in `AndroidCameraQRCodeReader.RenderScanner` catches initialization failures only for the probe call. The actual `ScannerView` AndroidView factory (in KScan) constructs another `BarcodeAnalyzer`, and any failure there bypasses our `runCatching` and crashes Compose.

Even after the ProGuard fix, harden this path so future regressions surface as a `DecodeFailure` instead of an app crash:

- Wrap the `ScannerView { … }` invocation in `AndroidCameraQRCodeReader.RenderScanner` with a `runCatching { … }` around its first composition (or guard via a `DisposableEffect`/`SideEffect` that observes a thrown CompositionLocal). Practical approach:
  - Keep the existing probe.
  - Additionally, place the `ScannerView(...)` call inside a Compose `key(activeScan.id) { … }` plus a try/catch via a small `runCatching`-style wrapper that, on failure, calls `finishScan(activeScan, QRCodeReadResult.DecodeFailure(...))`. Compose itself can't catch exceptions thrown during AndroidView factory inside a normal try/catch in a composable lambda, so the more reliable mitigation is to keep the probe (which we already have) and rely on the ProGuard fix to prevent the actual failure.

The probe already prevents the crash on devices where the `BarcodeScanning.getClient()` call itself fails consistently. The ProGuard fix removes the failure mode entirely on release builds.

## Verification plan

1. **Reproduce locally (release/minified build)**
   - `./gradlew :androidApp:assembleRelease` (or `bundleRelease`) with current `main` — should crash on opening the scanner if AGP 9 R8 full mode is in use locally.
   - To force the same R8 mode locally if needed, ensure no `android.r8.strictFullModeForKeepRules=false` override is present in `gradle.properties`.
2. **Apply fix** — append the ML Kit rules to `composeApp/consumer-rules.pro`.
3. **Rebuild release**
   - `./gradlew :androidApp:assembleRelease`
   - Install APK on a physical device or emulator.
4. **Manual test**
   - Open the app → "Add account" → "Scan QR code".
   - Confirm the camera preview opens without crash and a QR code is decoded successfully.
5. **Regression check**
   - Smoke test: account list, manual entry, backup/import flows.
   - Confirm `:androidApp:lintRelease` and `:composeApp:assembleRelease` pass.
6. **Rollout**
   - Ship as a Play Store hotfix targeting the production track; monitor Crashlytics / Play Console "ANRs and crashes" for the `com.google.mlkit.vision.barcode.internal.zzg.zzb` signature for 48–72 hours.

## Files to change

- `composeApp/consumer-rules.pro` — add the ML Kit keep rules block above.

No code changes strictly required; the ProGuard rule update is sufficient to eliminate the crash. The defensive note about `ScannerView` is an optional follow-up for resilience.

## Out of scope / follow-ups

- Upstream fix request: file/track googlesamples/mlkit issue #1007 for `barcode-scanning` to ship its own consumer keep rules.
- Consider upgrading `kscan` if a newer release surfaces ML Kit init errors as a callback rather than throwing inside the AndroidView factory.
- Optional: add an integration check in `proguard-android-optimize.txt` chain to print `-printconfiguration` once per release to detect future stripping regressions.
