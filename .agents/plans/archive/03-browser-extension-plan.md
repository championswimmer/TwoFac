---
task: Create Browser Extensions for Chrome and Firefox
status: Completed
progress:
  - "[x] PWA foundation"
  - "[x] Chrome extension"
  - "[x] Firefox extension"
  - "[x] Build pipeline and packaging"
---
# Browser Extension Packaging Plan (Chrome + Firefox)

## Goal

Ship the existing Kotlin Compose Multiplatform web app (wasmJs target) as a **Chrome** and **Firefox browser extension**, while retaining the ability to deploy the same core webapp as a **standalone PWA**. No Kotlin Compose UI code should be rewritten — the extension wraps the identical Wasm output.

---

## Current state of the web target

| Aspect | Detail |
|---|---|
| Build target | `wasmJs` in `composeApp/build.gradle.kts` |
| Entry point | `composeApp/src/wasmJsMain/kotlin/tech/arnav/twofac/main.kt` → `ComposeViewport(document.body!!) { App() }` |
| HTML shell | `composeApp/src/wasmJsMain/resources/index.html` loads `composeApp.js` |
| Storage | Browser `localStorage` via `kstore-storage` (`AppDirUtils.wasmJs.kt`) |
| Build output | `./gradlew :composeApp:wasmJsBrowserProductionWebpack` → `composeApp/build/dist/wasmJs/productionExecutable/` |
| PWA support | **None** — no `manifest.webmanifest`, no service worker |
| Output size | Expect ~3–7 MB total (`.wasm` + JS glue + Skiko rendering) |

---

## Why a browser extension?

1. **Always-available OTP codes** — one click from the toolbar; no need to switch to a separate tab or mobile device.
2. **Distribution via Chrome Web Store / Firefox Add-ons** — discoverability and auto-updates.
3. **Optional future features** — side panel UI, autofill integration, context-menu copy, per-tab code suggestions.
4. **Complements the PWA** — users who prefer a standalone installable web app can still use the PWA; power users wanting tighter browser integration get the extension.

---

## High-level architecture

```
composeApp/
  src/wasmJsMain/          ← shared Kotlin Compose UI + business logic (unchanged)
  src/wasmJsMain/resources/
    index.html             ← used by PWA
    popup.html             ← NEW: extension popup shell (loads same composeApp.js)
    sidepanel.html         ← NEW: optional side-panel shell
    manifest.json          ← NEW: Chrome MV3 / Firefox extension manifest
    manifest.webmanifest   ← NEW: PWA web app manifest
    service-worker.js      ← NEW: PWA offline cache service worker
    background.js          ← NEW: extension background service worker (minimal)
    icons/                 ← NEW: extension + PWA icons (multiple sizes)
```

### Shared vs. target-specific files

| File | PWA | Extension | Notes |
|---|:---:|:---:|---|
| `composeApp.js` + `.wasm` + `.data` | ✅ | ✅ | Identical production build output |
| `index.html` | ✅ | ❌ | Standalone web page entry |
| `popup.html` | ❌ | ✅ | Extension popup (loads composeApp.js) |
| `sidepanel.html` | ❌ | ✅ | Chrome side-panel (optional) |
| `manifest.json` (extension) | ❌ | ✅ | MV3 extension manifest |
| `manifest.webmanifest` | ✅ | ❌ | PWA install manifest |
| `service-worker.js` | ✅ | ❌ | Offline caching for PWA |
| `background.js` | ❌ | ✅ | Extension service worker |
| `icons/*` | ✅ | ✅ | Shared icon set |

The key insight is that **the Compose Wasm build output is identical** — only the HTML shell and manifest files differ per distribution target.

---

## Detailed plan

### Phase 0 — PWA foundation (prerequisite, do first)

Before building the extension, make the web app a proper PWA so it works offline. This also validates the Wasm caching story.

#### 0.1 Add `manifest.webmanifest`

```json
{
  "name": "TwoFac",
  "short_name": "TwoFac",
  "start_url": "/index.html",
  "display": "standalone",
  "background_color": "#ffffff",
  "theme_color": "#6200EE",
  "icons": [
    { "src": "icons/icon-192.png", "sizes": "192x192", "type": "image/png" },
    { "src": "icons/icon-512.png", "sizes": "512x512", "type": "image/png" }
  ]
}
```

Link it from `index.html`:
```html
<link rel="manifest" href="manifest.webmanifest">
```

#### 0.2 Add a service worker for offline caching

A minimal `service-worker.js` that caches the Wasm bundle, JS glue, and HTML on install:

```js
const CACHE_NAME = 'twofac-v1';
const ASSETS = [
  '/',
  '/index.html',
  '/composeApp.js',
  '/composeApp.wasm',
  '/styles.css',
  '/favicon.png'
];

self.addEventListener('install', (e) => {
  e.waitUntil(caches.open(CACHE_NAME).then(c => c.addAll(ASSETS)));
});

self.addEventListener('fetch', (e) => {
  e.respondWith(
    caches.match(e.request).then(r => r || fetch(e.request))
  );
});
```

Register from `index.html`:
```html
<script>
  if ('serviceWorker' in navigator) {
    navigator.serviceWorker.register('/service-worker.js');
  }
</script>
```

> **Alternative**: Use the [ComposePWA Gradle plugin](https://github.com/yu-ko-ba/ComposePWA) (`dev.yuyuyuyuyu.composepwa`) to auto-generate the manifest, service worker registration, and Workbox config. This avoids manual maintenance and integrates with the Gradle build.

#### 0.3 Generate icon assets

Create icons at 16, 32, 48, 128, 192, 512 px from the existing `logo/` assets. Place them in `composeApp/src/wasmJsMain/resources/icons/`.

---

### Phase 1 — Chrome extension (Manifest V3)

#### 1.1 Create `manifest.json` (Chrome MV3)

```json
{
  "manifest_version": 3,
  "name": "TwoFac Authenticator",
  "version": "1.0.2",
  "description": "Two-factor authentication codes in your browser",
  "icons": {
    "16": "icons/icon-16.png",
    "32": "icons/icon-32.png",
    "48": "icons/icon-48.png",
    "128": "icons/icon-128.png"
  },
  "action": {
    "default_popup": "popup.html",
    "default_icon": {
      "16": "icons/icon-16.png",
      "32": "icons/icon-32.png"
    },
    "default_title": "TwoFac"
  },
  "side_panel": {
    "default_path": "sidepanel.html"
  },
  "permissions": ["sidePanel", "storage"],
  "background": {
    "service_worker": "background.js"
  },
  "content_security_policy": {
    "extension_pages": "script-src 'self' 'wasm-unsafe-eval'; object-src 'self';"
  }
}
```

##### Key decisions

| Decision | Rationale |
|---|---|
| `wasm-unsafe-eval` in CSP | **Required** for `WebAssembly.compile()` / `WebAssembly.instantiate()` to work in extension pages. Without this, Chrome blocks Wasm compilation. |
| `action.default_popup` | The primary UI surface. Chrome popup max dimensions are 800×600 px — our Compose UI should render within ~360×500 px. |
| `side_panel` (optional) | Chrome 114+ supports a persistent sidebar. This gives a larger canvas than the popup and stays open while the user interacts with websites. |
| `storage` permission | Allows use of `chrome.storage.local` as an alternative/complement to `localStorage` for secrets. |
| No `host_permissions` | The 2FA app does not need to access web page content. This keeps the review process simpler. |

#### 1.2 Create `popup.html`

```html
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>TwoFac</title>
  <link rel="icon" type="image/png" href="icons/icon-32.png">
  <link type="text/css" rel="stylesheet" href="styles.css">
  <style>
    html, body {
      width: 360px;
      height: 500px;
    }
  </style>
  <script type="application/javascript" src="composeApp.js"></script>
</head>
<body>
</body>
</html>
```

The only difference from `index.html` is the fixed dimensions for the popup viewport. The Compose app renders inside `document.body` exactly as it does in the web version.

#### 1.3 Create `sidepanel.html` (optional)

Same as `popup.html` but with larger dimensions (`width: 100%; height: 100%`). The side panel is resizable by the user.

#### 1.4 Create `background.js`

```js
// Minimal background service worker for TwoFac extension
// Opens side panel when the extension icon is clicked (optional behavior)
chrome.sidePanel.setPanelBehavior({ openPanelOnActionClick: false });
```

This is intentionally minimal. The background worker can later be extended for:
- Periodic badge updates showing the current TOTP countdown
- Clipboard write on code click
- Context menu integration

#### 1.5 Content Security Policy: `wasm-unsafe-eval`

Chrome MV3 **does not** allow `unsafe-eval` in extension pages. However, it **does** support `wasm-unsafe-eval`, which permits WebAssembly compilation without opening the door to arbitrary JS eval. This is the only CSP relaxation needed.

Firefox also supports `wasm-unsafe-eval` in extension manifests.

---

### Phase 2 — Firefox extension

Firefox supports MV3 extensions (v109+) and WebAssembly with GC. The same manifest.json works with minor adjustments.

#### 2.1 Firefox-specific manifest fields

Firefox requires a `browser_specific_settings` block:

```json
{
  "browser_specific_settings": {
    "gecko": {
      "id": "twofac@arnav.tech",
      "strict_min_version": "120.0"
    }
  }
}
```

Firefox 120+ has Wasm GC enabled by default — which is required for Kotlin/Wasm output.

#### 2.2 Cross-browser compatibility strategy

| Feature | Chrome | Firefox | Strategy |
|---|---|---|---|
| MV3 manifest | ✅ | ✅ (v109+) | Single manifest, add `browser_specific_settings` |
| `wasm-unsafe-eval` | ✅ | ✅ | Same CSP directive |
| `chrome.sidePanel` | ✅ (v114+) | ❌ | Graceful degradation: popup-only on Firefox |
| `chrome.storage` | ✅ | ✅ (`browser.storage`) | Use `globalThis.chrome?.storage \|\| globalThis.browser?.storage` |
| Wasm GC | Chrome 119+ | Firefox 120+ | Both support Kotlin/Wasm output |

#### 2.3 Polyfill approach

Use Mozilla's [`webextension-polyfill`](https://github.com/mozilla/webextension-polyfill) if the extension needs to call browser extension APIs from Kotlin/Wasm via JS interop. This normalizes `chrome.*` and `browser.*` APIs.

For v1, since the Compose UI only uses `localStorage` (via kstore), no extension API calls are needed and no polyfill is required.

---

### Phase 3 — Build pipeline and packaging

#### 3.1 Gradle-driven build

The existing `./gradlew :composeApp:wasmJsBrowserProductionWebpack` produces the Wasm build output. The extension packaging wraps this output.

Add a custom Gradle task that:
1. Runs `wasmJsBrowserProductionWebpack`
2. Copies the production output to a staging directory
3. Adds extension-specific files (`manifest.json`, `popup.html`, `background.js`, icons)
4. Produces a `.zip` ready for Chrome Web Store / Firefox Add-ons upload

```kotlin
// composeApp/build.gradle.kts — proposed extension packaging task

tasks.register<Zip>("packageChromeExtension") {
    dependsOn("wasmJsBrowserProductionWebpack")
    from("build/dist/wasmJs/productionExecutable")
    from("src/wasmJsMain/resources/extension") {
        // extension-specific overrides: manifest.json, popup.html, background.js
    }
    archiveFileName.set("twofac-chrome-extension.zip")
    destinationDirectory.set(layout.buildDirectory.dir("extension"))
}

tasks.register<Zip>("packageFirefoxExtension") {
    dependsOn("wasmJsBrowserProductionWebpack")
    from("build/dist/wasmJs/productionExecutable")
    from("src/wasmJsMain/resources/extension-firefox") {
        // Firefox-specific manifest with browser_specific_settings
    }
    archiveFileName.set("twofac-firefox-extension.zip")
    destinationDirectory.set(layout.buildDirectory.dir("extension"))
}
```

#### 3.2 Resource directory layout

```
composeApp/src/wasmJsMain/resources/
  index.html                    ← PWA entry
  styles.css                    ← shared
  favicon.png                   ← shared
  manifest.webmanifest          ← PWA manifest
  service-worker.js             ← PWA offline worker
  icons/
    icon-16.png
    icon-32.png
    icon-48.png
    icon-128.png
    icon-192.png
    icon-512.png
  extension/                    ← Chrome extension overlay
    manifest.json
    popup.html
    sidepanel.html
    background.js
  extension-firefox/            ← Firefox extension overlay
    manifest.json               ← same as Chrome + gecko block
    popup.html
    background.js
```

#### 3.3 CI/CD workflow

Add GitHub Actions jobs:
- `build-web-pwa` — builds Wasm and deploys to hosting (existing or new)
- `build-chrome-extension` — runs `packageChromeExtension`, uploads artifact
- `build-firefox-extension` — runs `packageFirefoxExtension`, uploads artifact
- Optional: use `web-ext lint` to validate the Firefox extension before packaging

#### 3.4 Version synchronization

The extension `manifest.json` version field should be kept in sync with the app version defined in the root `build.gradle.kts` (`appVersionName`). The Gradle packaging task can template-replace the version string.

---

### Phase 4 — Storage considerations

#### 4.1 Current: `localStorage` via kstore-storage

The wasmJs target already uses browser `localStorage` for account storage. This works identically in:
- Standalone web page / PWA
- Extension popup pages
- Extension side panels

`localStorage` in an extension context is scoped to the extension's origin (`chrome-extension://<id>/`), which is isolated from web pages. This is fine for a v1 extension.

#### 4.2 Future: `chrome.storage.local` migration

For a more robust extension, consider migrating to `chrome.storage.local`:
- Persists across extension updates
- Can sync across devices via `chrome.storage.sync`
- Has higher storage limits (10 MB local vs. 5 MB localStorage)

This would require a Kotlin external declaration for the Chrome storage API in `wasmJsMain`:

```kotlin
// wasmJsMain — chrome.storage external declarations
@JsName("chrome")
external object ChromeApi {
    val storage: ChromeStorage
}

external interface ChromeStorage {
    val local: StorageArea
}

external interface StorageArea {
    fun get(keys: JsAny?, callback: (JsAny) -> Unit)
    fun set(items: JsAny, callback: (() -> Unit)?)
}
```

A `KStore` adapter wrapping `chrome.storage.local` would let the existing storage layer work without changing any UI or business logic code.

#### 4.3 Storage decision matrix

| Storage backend | PWA | Extension | Sync | Limit | Migration needed |
|---|:---:|:---:|:---:|---|---|
| `localStorage` | ✅ | ✅ | ❌ | ~5 MB | None (current) |
| `chrome.storage.local` | ❌ | ✅ | ❌ | 10 MB | New KStore adapter |
| `chrome.storage.sync` | ❌ | ✅ | ✅ | 100 KB | New KStore adapter |

**Recommendation**: Start with `localStorage` for both PWA and extension. Add `chrome.storage.local` as a follow-up once extension is validated.

---

### Phase 5 — Extension-specific enhancements (future)

These features are **not** part of the initial extension release but are worth planning for:

| Feature | Description | Complexity |
|---|---|---|
| **Badge countdown** | Show TOTP seconds remaining on the toolbar icon | Low — JS `setInterval` in `background.js` calling `chrome.action.setBadgeText` |
| **Clipboard copy** | One-click copy OTP to clipboard from popup | Low — Compose UI already supports this |
| **Context menu** | Right-click → "Paste OTP for site X" | Medium — requires `contextMenus` permission + site matching |
| **Autofill** | Detect 2FA input fields and offer to fill | High — requires `content_scripts` and DOM inspection |
| **Side panel** | Persistent sidebar with full app | Low — already planned (`sidepanel.html`) |
| **Keyboard shortcut** | `Ctrl+Shift+2` to open popup | Low — `commands` in manifest |

---

## Performance considerations

### Wasm cold start in extension popup

| Concern | Detail | Mitigation |
|---|---|---|
| **Bundle size** | Kotlin/Wasm + Compose + Skiko output is typically 3–7 MB | Chrome caches extension resources locally; cold start only on install/update |
| **First paint latency** | Wasm compilation adds 200–800 ms on first open | Acceptable for a 2FA popup; subsequent opens are faster due to compiled code cache |
| **Memory** | Skiko canvas rendering uses more memory than DOM-based UIs | Extension popups are short-lived; memory is freed on close |

### Optimization opportunities

- Use `wasmJsBrowserProductionWebpack` (not dev) for smaller output
- Kotlin 2.x brings ~30% Wasm binary size reduction
- Consider lazy-loading non-critical UI screens
- If startup is too slow, explore pre-compiling Wasm in the background service worker

---

## Research: existing Kotlin/Compose browser extension experiments

### 1. Kromex (DatL4g)
- **What**: Kotlin/JS-based Chrome extension template with Manifest V3 support
- **Approach**: Uses Kotlin/JS (not Wasm) to compile Kotlin to JS for popup, background, and content scripts
- **Relevance**: Proves Kotlin can be used for extension development. However, it targets Kotlin/JS, not Kotlin/Wasm with Compose UI
- **Takeaway**: The extension scaffolding patterns (manifest, background worker, popup) are directly applicable; the UI rendering approach differs

### 2. Kotlin/Wasm examples (JetBrains)
- **What**: Official JetBrains repository of Kotlin/Wasm browser examples including Compose Multiplatform demos
- **Approach**: Targets web pages, not extensions specifically; demonstrates Wasm interop and Compose rendering in browser
- **Relevance**: Validates that Kotlin/Wasm + Compose output works in modern Chrome/Firefox. The same output can be loaded in an extension popup
- **Takeaway**: The `productionExecutable` output from these examples is structurally identical to what we'd bundle in an extension

### 3. ComposePWA Gradle plugin (yu-ko-ba)
- **What**: Gradle plugin that auto-generates PWA manifest, service worker, and Workbox config for Compose Multiplatform web apps
- **Approach**: Hooks into the Wasm build pipeline, adds PWA assets to the production output
- **Relevance**: Directly applicable for our PWA goal. Can be used alongside extension packaging
- **Takeaway**: Consider adopting this plugin for Phase 0 to avoid hand-maintaining PWA assets

### 4. theberrigan/rust-wasm-chrome-ext
- **What**: Chrome extension using Rust-compiled Wasm with Manifest V3
- **Approach**: Same architectural pattern — Wasm binary loaded in extension popup via JS glue
- **Relevance**: Validates that Wasm in MV3 extensions works with `wasm-unsafe-eval` CSP directive
- **Takeaway**: The CSP and manifest patterns are directly transferable to Kotlin/Wasm

### 5. Authenticator.cc / 2FAS
- **What**: Popular 2FA authenticator Chrome extensions
- **Approach**: Pure JavaScript/TypeScript implementations
- **Relevance**: UX reference for what a 2FA extension should look and behave like — popup dimensions, code display layout, QR scanning
- **Takeaway**: Target ~360×500 px popup; show 4–6 codes at once; one-click copy; countdown timer

### 6. JetBrains Compose for Web status
- **What**: Compose Multiplatform for Web reached Beta status in v1.9.0
- **Approach**: Uses Skiko for rendering to HTML Canvas, compiled via Kotlin/Wasm
- **Relevance**: Confirms the rendering engine works in all modern browsers including inside extension popups
- **Takeaway**: Browser support requires Chrome 119+ / Firefox 120+ for Wasm GC

---

## Step-by-step implementation order

### Slice A — PWA offline support (foundation)
1. Generate icon assets from `logo/`
2. Add `manifest.webmanifest` to `wasmJsMain/resources/`
3. Add `service-worker.js` for offline caching (or integrate ComposePWA plugin)
4. Update `index.html` with manifest link and service worker registration
5. Test PWA install and offline behavior in Chrome/Firefox

### Slice B — Chrome extension (MVP)
1. Create `extension/` overlay directory with `manifest.json`, `popup.html`, `background.js`
2. Add `packageChromeExtension` Gradle task
3. Build and test: load unpacked in `chrome://extensions`
4. Validate: popup renders, OTP codes display, localStorage works
5. Fix any CSP or sizing issues

### Slice C — Firefox extension
1. Create `extension-firefox/` overlay with Firefox-specific manifest
2. Add `packageFirefoxExtension` Gradle task
3. Test in Firefox with `about:debugging#/runtime/this-firefox`
4. Validate Wasm GC support and rendering
5. Run `web-ext lint` for compliance

### Slice D — CI/CD and distribution
1. Add GitHub Actions workflow for extension builds
2. Automate version sync from root build config
3. Upload artifacts to Chrome Web Store / Firefox Add-ons (manual first, automated later)
4. Add extension-specific README section

### Slice E — Enhancements (future)
1. `chrome.storage.local` migration with KStore adapter
2. Badge countdown timer
3. Side panel support
4. Keyboard shortcut
5. Cross-device sync via `chrome.storage.sync`

---

## Risk assessment

| Risk | Impact | Likelihood | Mitigation |
|---|---|---|---|
| Wasm GC not supported in older browsers | Extension won't load | Low (Chrome 119+/Firefox 120+ already widespread) | Declare minimum browser version in manifest |
| Popup cold start too slow (>1s) | Poor UX on first click | Medium | Wasm code cache after first load; optimize bundle size |
| Chrome Web Store review rejects `wasm-unsafe-eval` | Can't publish | Low (Google explicitly supports this directive) | Document justification; it's the standard approach |
| Wasm bundle size too large for extension | Slow install, large download | Low (3–7 MB is within norms) | Monitor size; Kotlin 2.x improvements will help |
| localStorage isolation breaks across update | Data loss | Very Low | localStorage persists within extension origin across updates |

---

## Research links

### Chrome extension development
- [Chrome MV3 overview](https://developer.chrome.com/docs/extensions/develop/migrate/what-is-mv3)
- [Chrome extension CSP (wasm-unsafe-eval)](https://developer.chrome.com/docs/extensions/reference/manifest/content-security-policy)
- [Chrome sidePanel API](https://developer.chrome.com/docs/extensions/reference/api/sidePanel)
- [Using Wasm in MV3 — GitHub issue #775](https://github.com/GoogleChrome/chrome-extensions-samples/issues/775)
- [Wasm in MV3 — Google Groups discussion](https://groups.google.com/a/chromium.org/g/chromium-extensions/c/sJiaTnFMLHQ)
- [Chrome extension popup size limits](https://groups.google.com/a/chromium.org/g/chromium-extensions/c/3A8d3oiOV_E)

### Firefox extension development
- [Firefox MV3 migration guide](https://extensionworkshop.com/documentation/develop/manifest-v3-migration-guide/)
- [MDN: extension manifest.json reference](https://developer.mozilla.org/en-US/docs/Mozilla/Add-ons/WebExtensions/manifest.json)
- [MDN: extension CSP](https://developer.mozilla.org/en-US/docs/Mozilla/Add-ons/WebExtensions/Content_Security_Policy)
- [MDN: web_accessible_resources](https://developer.mozilla.org/en-US/docs/Mozilla/Add-ons/WebExtensions/manifest.json/web_accessible_resources)
- [MDN: cross-browser extensions](https://developer.mozilla.org/en-US/docs/Mozilla/Add-ons/WebExtensions/Build_a_cross_browser_extension)

### Cross-browser tooling
- [web-ext (Mozilla CLI tool)](https://extensionworkshop.com/documentation/develop/getting-started-with-web-ext/)
- [Extension.js — cross-browser framework](https://extension.js.org/)
- [webextension-polyfill (Mozilla)](https://github.com/mozilla/webextension-polyfill)

### Kotlin/Wasm and Compose Multiplatform
- [Kotlin/Wasm overview](https://kotlinlang.org/docs/wasm-overview.html)
- [Get started with Kotlin/Wasm + Compose](https://kotlinlang.org/docs/wasm-get-started.html)
- [Kotlin/Wasm JS interop](https://kotlinlang.org/docs/wasm-js-interop.html)
- [Compose Multiplatform 1.9.0 release (Web Beta)](https://blog.jetbrains.com/kotlin/2025/09/compose-multiplatform-1-9-0-compose-for-web-beta/)
- [JetBrains Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform)
- [Kotlin/Wasm examples](https://github.com/Kotlin/kotlin-wasm-examples)
- [Kotlin Wasm Compose template](https://github.com/Kotlin/kotlin-wasm-compose-template)
- [KotlinConf 2025 — State of Kotlin/Wasm and Compose for Web](https://resources.jetbrains.com/storage/products/kotlinconf-2025/may-22/State%20of%20Kotlin_Wasm%20and%20Compose%20Multiplatform%20for%20Web%20on%20Modern%20Browsers%20_%20Pamela%20Hill.pdf)
- [WebAssembly GC support in Chrome](https://developer.chrome.com/blog/wasmgc/)

### Kotlin browser extension projects
- [Kromex — Kotlin/JS Chrome extension template (DatL4g)](https://github.com/DATL4G/Kromex)
- [ComposePWA Gradle plugin](https://github.com/yu-ko-ba/ComposePWA)
- [K/JS and K/Wasm interop discussion (Kotlin forums)](https://discuss.kotlinlang.org/t/how-to-achieve-type-safe-k-js-and-k-wasm-interoperability-call-browser-apis-in-a-shared-kmp-web-sourceset/29506)

### Wasm in browser extensions (other languages)
- [Rust + Wasm Chrome extension example (theberrigan)](https://github.com/theberrigan/rust-wasm-chrome-ext)
- [Chrome extension with Rust and Wasm guide (DEV Community)](https://dev.to/rimutaka/chrome-extension-with-rust-and-wasm-by-example-5cbh)
- [Optimizing Wasm module size](https://tty4.dev/development/optimizing-wasm-js-size/)

### 2FA authenticator extension references
- [Authenticator by authenticator.cc (Chrome Web Store)](https://chromewebstore.google.com/detail/authenticator/bhghoamapcdpbohphigoooaddinpkbai)
- [2FA Authenticator (Chrome Web Store)](https://chromewebstore.google.com/detail/2fa-authenticator/lihconfopkpbjpkbbcpofjofmpaopgol)

### PWA development
- [MDN: Web application manifest](https://developer.mozilla.org/en-US/docs/Web/Progressive_web_apps/Manifest)
- [MDN: Service workers / offline](https://developer.mozilla.org/en-US/docs/Web/Progressive_web_apps/Guides/Offline_and_background_operation)
- [Compose for Web (WASM) — What and Why? (Handstand Sam)](https://handstandsam.com/2023/09/05/compose-for-web-wasm/)
- [Deploying Kotlin/Wasm to a web server (Stack Overflow)](https://stackoverflow.com/questions/78669456/how-to-deploy-a-kotlin-multiplatform-webassembly-project-on-a-web-server)
