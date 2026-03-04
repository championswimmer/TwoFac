---
name: Wasm TypeScript Interop Migration
status: Planned
progress:
  - "[ ] Phase 0 - Setup TypeScript Project & Gradle Wiring"
  - "[ ] Phase 1 - Migrate WebCrypto Interop"
  - "[ ] Phase 2 - Migrate WebAuthn Interop"
  - "[ ] Phase 3 - Migrate QR & Storage Interop"
  - "[ ] Phase 4 - Clean up & Validate"
---

# Wasm TypeScript Interop Migration Plan

## Goal
Replace the large, inline `@JsFun` and `js("...")` JavaScript string blocks in the `wasmJsMain` target with a proper, type-safe **TypeScript** module. The TypeScript code will be automatically compiled into standard ES Modules (`.mjs`) during the Gradle build process. 

### Note on Type Safety & `.d.ts` files
Based on research, **Kotlin/Wasm cannot currently consume `.d.ts` files to automatically generate Kotlin bindings** (the old `dukat` tool used for Kotlin/JS is deprecated and not supported for Wasm). 

Therefore, generating `.d.ts` files during the TS build won't provide *automated* type safety on the Kotlin compiler side. Type safety in this architecture is achieved in two halves:
1. **TypeScript Side:** The TS compiler ensures your WebCrypto, WebAuthn, and DOM API calls are strictly typed and correct before generating the `.mjs` output.
2. **Kotlin Side:** You manually write `external fun` declarations in Kotlin that mirror your TS exports. You must ensure these signatures match.

## Architecture & Integration Strategy
We will use a **Pre-compile Gradle Task Route** to ensure that running standard Gradle tasks (like `wasmJsBrowserDevelopmentRun` or `wasmJsBrowserProductionWebpack`) automatically handles the TypeScript compilation without any manual steps from the developer.

1. **Dedicated TS folder**: We will create a `composeApp/src/wasmJsMain/typescript/` folder containing a standard `package.json` and `tsconfig.json`.
2. **Gradle `Exec` Tasks**: We will register Gradle tasks to run `npm install` and `npx tsc`.
3. **Gradle Task Graph Hooking**: We will hook these tasks into the Kotlin/Wasm build lifecycle (specifically running *before* `wasmJsProcessResources`) so the outputs are always ready before Webpack bundles them.
4. **Generated Resources**: The TypeScript compiler will output `.mjs` files directly into a generated resources directory (`composeApp/build/generated/wasmJs/resources/`).
5. **Kotlin Integration**: We will add this generated directory to the Kotlin `wasmJs` source sets, and Kotlin will import the functions using `@JsModule("./my-module.mjs")`.

---

## Detailed Phase-by-Phase Roadmap

### Phase 0 - Setup TypeScript Project & Gradle Wiring
1. **Create TypeScript Project Scaffold:**
   - Create directory: `composeApp/src/wasmJsMain/typescript/`.
   - Add `package.json` with dependencies on `typescript`, `tslib`, and `jsqr`.
   - Add `tsconfig.json` configured for `"module": "ESNext"`, `"target": "ES2022"`, and `"outDir": "../../../build/generated/wasmJs/resources/"`.
2. **Wire Gradle Tasks in `composeApp/build.gradle.kts`:**
   - Define the tasks to execute npm and tsc:
     ```kotlin
     val tsDir = file("src/wasmJsMain/typescript")
     
     val installWasmInteropDependencies by tasks.registering(Exec::class) {
         workingDir = tsDir
         commandLine = listOf("npm", "install")
         // Only run if package.json changes
         inputs.file(tsDir.resolve("package.json"))
         outputs.dir(tsDir.resolve("node_modules"))
     }

     val compileWasmInterop by tasks.registering(Exec::class) {
         dependsOn(installWasmInteropDependencies)
         workingDir = tsDir
         commandLine = listOf("npx", "tsc")
         // Watch TS files for changes
         inputs.dir(tsDir)
         // Output directory defined in tsconfig.json
         outputs.dir(layout.buildDirectory.dir("generated/wasmJs/resources"))
     }
     ```
   - **Hook into the Wasm lifecycle:** Ensure compilation happens before resources are processed so Webpack finds them:
     ```kotlin
     tasks.named("wasmJsProcessResources") {
         dependsOn(compileWasmInterop)
     }
     ```
   - Add the generated output directory to the `wasmJs` resources source set so Webpack can bundle the resulting `.mjs` files:
     ```kotlin
     kotlin.sourceSets.wasmJsMain {
         resources.srcDir(layout.buildDirectory.dir("generated/wasmJs/resources"))
     }
     ```
3. **Verify Pipeline:**
   - Create a dummy `test.ts` with a simple export.
   - Run `./gradlew :composeApp:wasmJsBrowserDevelopmentRun` and verify that Gradle automatically runs `npm install`, then `npx tsc`, and finally starts the web server.

### Phase 1 - Migrate WebCrypto Interop
1. **Create `crypto.ts`**:
   - Extract the logic from `encryptPasskeyWithWebCrypto` and `decryptPasskeyWithWebCrypto` (currently in `WebCryptoClient.kt`) into TypeScript functions.
   - Use proper TS typings for the `crypto.subtle` APIs.
   - Update the callback/promise patterns to return native JS Promises, which translate perfectly to Kotlin `kotlin.js.Promise`.
2. **Update Kotlin Bindings**:
   - In `WebCryptoClient.kt`, replace the `@JsFun` blocks with `@JsModule("./crypto.mjs")`.
   - Use `external fun` declarations. For example:
     ```kotlin
     @JsModule("./crypto.mjs")
     external fun encryptPasskey(plaintext: String, prfFirstOutputBase64Url: String, context: String): Promise<JsAny?>
     ```
   - Refactor the caller to use `.await()` instead of manual callbacks.

### Phase 2 - Migrate WebAuthn Interop
1. **Create `webauthn.ts`**:
   - Move `isWebAuthnSupported`, `queryWebAuthnCapabilities`, `createWebAuthnCredential`, and `authenticateWebAuthnCredential` from `WebAuthnClient.kt`.
   - Define TypeScript interfaces for the expected WebAuthn results, matching the models you need in Kotlin.
2. **Update Kotlin Bindings**:
   - Replace the large `@JsFun` blocks with `@JsModule("./webauthn.mjs")`.
   - Wire the calls up using Coroutines and `.await()`, dropping the manual continuation wrapping if possible, or mapping the `Promise` result to the Kotlin sealed classes.

### Phase 3 - Migrate QR & Storage Interop
1. **Create `qr-reader.ts`**:
   - Move the massive `readQRCodeFromClipboard` `@JsFun` string from `WasmClipboardQRCodeReader.kt` into this file.
   - Take advantage of `import jsqr from "jsqr";` directly in TypeScript.
   - Clean up the fallback chains (Async Clipboard API vs Paste Event) with proper async/await TS syntax.
2. **Create `storage.ts`**:
   - Move the small `localStorage` wrappers from `WebStorageClient.kt` (`localStorageGetItem`, etc.). This might be small enough to stay as `js("...")` snippets, but moving them to TS unifies the architecture.
3. **Update Kotlin Bindings**:
   - Bind to `qr-reader.mjs` and `storage.mjs` using `@JsModule` and `external`.

### Phase 4 - Clean up & Validate
1. **Remove `@JsFun` usages**: Ensure no large JS strings are left in the `wasmJsMain` source set.
2. **Test the build process**:
   - Run `./gradlew :composeApp:wasmJsBrowserDevelopmentRun` to ensure the dev server works and auto-reloads.
   - Run `./gradlew :composeApp:wasmJsBrowserProductionWebpack` to ensure production minification successfully bundles the `.mjs` files without errors.
3. **Manual App Validation**:
   - Open the web app.
   - Add an account using the clipboard QR reader.
   - Enable Biometric/WebAuthn unlock in Settings.
   - Add a passkey and verify it encrypts, decrypts, and unlocks the vault properly on a page refresh.

---

## Benefits of this Plan
- **Zero Friction for Developers**: The Gradle `Exec` tasks handle the TypeScript build automatically. Anyone running `wasmJsBrowserDevelopmentRun` will seamlessly get the latest compiled TypeScript logic without running separate terminal commands.
- **Type Safety**: WebCrypto and WebAuthn APIs will be strictly typed via TypeScript's DOM library during the TS compilation phase.
- **Linting & Formatting**: Standard JS/TS tooling (Prettier, ESLint) can be applied to complex browser logic.
- **Coroutines over Callbacks**: We can expose standard `Promise<T>` from TypeScript and use `.await()` in Kotlin, significantly reducing the messy `suspendCoroutine` + callback blocks currently cluttering the Kotlin files.
- **Developer Experience**: No more escaping quotes or losing IDE support inside massive `"""` Kotlin string literals.