---
name: Browser Extension Desktop Companion Mode
status: Planned
progress:
  - "[ ] Phase 0 — Native Messaging Host in Desktop App"
  - "[ ] Phase 1 — Extension-side Native Messaging Client (TypeScript)"
  - "[ ] Phase 2 — Companion-aware Account Storage Switching in Wasm/Kotlin"
  - "[ ] Phase 3 — Extension UI: Companion Status Indicator & Settings"
  - "[ ] Phase 4 — Desktop App Installer Registers Native Messaging Host"
  - "[ ] Phase 5 — Security Hardening & Review"
  - "[ ] Phase 6 — Testing, Documentation & Release"
---

# Browser Extension Desktop Companion Mode

## Goal

Enable the TwoFac browser extension (Chrome & Firefox) to operate in **companion mode** when the TwoFac desktop app is installed and running on the same machine — sharing the desktop app's account storage instead of maintaining a separate copy in browser `localStorage`.

When the desktop app is **not** detected, the extension continues to work **standalone** using its existing `localStorage`-backed storage, exactly as it does today.

This is the same pattern used by:
- **ExpressVPN**: extension acts as a "remote control" for the desktop app when installed, falls back to proxy-only mode otherwise.
- **1Password**: extension connects to the desktop app via native messaging for shared vault access and mutual unlock/lock.
- **NordVPN**: extension can communicate with the desktop app for system-wide VPN control.

---

## Prior Art in This Repository

### Watch Companion Pattern (WearOS + watchOS)

This project already implements a **companion mode** for watch apps:

| Component | Android (WearOS) | iOS (watchOS) |
|---|---|---|
| Common interface | `CompanionSyncCoordinator` (commonMain) | Same |
| Phone-side impl | `AndroidWatchSyncCoordinator` (androidMain) | `IosCompanionSyncCoordinator` (iosMain) |
| Transport | Google Play Wearable Data Layer API | Apple `WCSession` (WatchConnectivity) |
| Payload | `WatchSyncSnapshot` → JSON via `WatchSyncSnapshotCodec` | Same model via `IosWatchSyncHelper` |
| Watch-side cache | `WatchSyncSnapshotRepository` (KStore file) | `Documents/watch-sync-payload.json` |
| Trigger | Account changes, unlock, periodic `WorkManager` | Account changes, unlock, app lifecycle |

**Key design principles carried forward:**
1. **Common interface** (`CompanionSyncCoordinator`) abstracts platform-specific discovery and sync.
2. **Snapshot model** (`WatchSyncSnapshot`) serialized as JSON is the interchange format.
3. **Graceful degradation**: watch works offline from cached data; extension works standalone from localStorage.
4. **Detection before sync**: `isCompanionActive()` checks companion availability before attempting communication.

### Existing Browser Extension Architecture

The browser extension (plan 03) packages the same Kotlin/Wasm Compose UI as the PWA:
- **Entry points**: `popup.html`, `sidepanel.html` load `composeApp.js` + `.wasm`
- **Storage**: `localStorage` via KStore (`key: "twofac_accounts"`)
- **Manifest**: Chrome MV3 with `wasm-unsafe-eval` CSP
- **Background**: minimal `background.js` service worker (side panel behavior only)
- **TypeScript interop**: `composeApp/src/wasmJsMain/typescript/src/` contains `.mts` modules for storage, crypto, WebAuthn, QR, backup, time

### Desktop App Storage

The desktop app (Compose Desktop / JVM) stores accounts at platform-specific paths via the `AppDirs` library:

| OS | Path |
|---|---|
| Windows | `%APPDATA%\tech.arnav\TwoFac\accounts.json` |
| macOS | `~/Library/Application Support/TwoFac/accounts.json` |
| Linux | `~/.local/share/TwoFac/accounts.json` |

The desktop app also supports a **system tray** mode (plan 09) where it runs persistently in the background — a key enabler for companion mode since the extension needs the desktop app to be running.

---

## How Browser Extension ↔ Desktop App Communication Works

### The Native Messaging API

Chrome, Firefox, and Edge all support the [Native Messaging API](https://developer.chrome.com/docs/extensions/develop/concepts/native-messaging) which allows browser extensions to communicate with native desktop applications via stdin/stdout JSON messages.

#### Architecture Overview

```
┌─────────────────────┐       Native Messaging (stdio)       ┌──────────────────────┐
│  Browser Extension   │ ◄──────────────────────────────────► │   Native Messaging   │
│  (popup / sidepanel) │    4-byte length + UTF-8 JSON        │   Host (Desktop App) │
│                      │                                       │                      │
│  background.js       │                                       │  Reads accounts.json │
│  ↕ chrome.runtime    │                                       │  from AppDirs path   │
│    .connectNative()  │                                       │                      │
└─────────────────────┘                                       └──────────────────────┘
```

#### Protocol Details

1. **Extension → Host**: Extension calls `chrome.runtime.connectNative("tech.arnav.twofac")` or `chrome.runtime.sendNativeMessage(...)`.
2. **Message format**: Each message is a 4-byte little-endian unsigned integer (message length) followed by UTF-8-encoded JSON of exactly that many bytes.
3. **Host → Extension**: Host writes response in same format to stdout.
4. **Lifecycle**: Browser launches the host process when connection is opened; process terminates when port is disconnected.

#### Host Manifest Registration

The desktop app installer must register a **native messaging host manifest** so the browser can find and launch the host:

**Manifest file** (`tech.arnav.twofac.json`):
```json
{
  "name": "tech.arnav.twofac",
  "description": "TwoFac Desktop Companion for Browser Extension",
  "path": "/path/to/twofac-nmh",
  "type": "stdio",
  "allowed_origins": [
    "chrome-extension://<CHROME_EXTENSION_ID>/"
  ],
  "allowed_extensions": [
    "twofac@tech.arnav"
  ]
}
```

**Registration locations**:

| OS | Chrome | Firefox |
|---|---|---|
| **Windows** | Registry: `HKCU\Software\Google\Chrome\NativeMessagingHosts\tech.arnav.twofac` → manifest path | Registry: `HKCU\Software\Mozilla\NativeMessagingHosts\tech.arnav.twofac` → manifest path |
| **macOS** | `~/Library/Application Support/Google/Chrome/NativeMessagingHosts/tech.arnav.twofac.json` | `~/Library/Application Support/Mozilla/NativeMessagingHosts/tech.arnav.twofac.json` |
| **Linux** | `~/.config/google-chrome/NativeMessagingHosts/tech.arnav.twofac.json` | `~/.mozilla/native-messaging-hosts/tech.arnav.twofac.json` |

---

## Companion Mode Design

### Operating Modes

The extension operates in one of two modes, determined at startup and re-evaluated periodically:

```
┌─────────────────────────────────────────────────────────┐
│              Extension Startup / Periodic Check          │
│                                                          │
│  1. Try chrome.runtime.sendNativeMessage("tech.arnav.   │
│     twofac", { type: "ping" })                           │
│                                                          │
│  ┌──────────────┐              ┌───────────────────┐     │
│  │  Response OK  │──────────►  │  COMPANION MODE   │     │
│  │  (host found) │              │  Read/write via   │     │
│  └──────────────┘              │  native messaging  │     │
│                                 └───────────────────┘     │
│  ┌──────────────┐              ┌───────────────────┐     │
│  │  Error/timeout│──────────►  │  STANDALONE MODE  │     │
│  │  (no host)    │              │  Use localStorage  │     │
│  └──────────────┘              │  (existing behavior)│    │
│                                 └───────────────────┘     │
└─────────────────────────────────────────────────────────┘
```

### Message Protocol (Extension ↔ Desktop Host)

All messages are JSON objects with a `type` field. The host reads from the desktop app's `accounts.json` file directly.

#### Request Messages (Extension → Host)

| Type | Description | Payload |
|---|---|---|
| `ping` | Check if desktop app is alive | `{}` |
| `get_accounts` | Read all accounts from desktop storage | `{}` |
| `get_storage_path` | Get the desktop storage file path | `{}` |
| `watch_accounts` | Subscribe to account change notifications | `{}` |

#### Response Messages (Host → Extension)

| Type | Description | Payload |
|---|---|---|
| `pong` | Heartbeat response | `{ "version": "1.0.0", "storagePath": "..." }` |
| `accounts` | Account data | `{ "accounts": [...], "generatedAtEpochSec": ... }` |
| `storage_path` | Desktop file path | `{ "path": "..." }` |
| `accounts_changed` | Push notification when file changes | `{ "accounts": [...], "generatedAtEpochSec": ... }` |
| `error` | Error response | `{ "code": "...", "message": "..." }` |

### Storage Layer Switching

The key architectural change is making the extension's account storage **pluggable** — selecting between localStorage (standalone) and native messaging (companion) based on desktop app availability.

```
┌──────────────────────────────────────────────┐
│           CompanionAwareAccountStore          │
│                                               │
│  isCompanionAvailable()                       │
│    ├─ true  → NativeMessagingAccountStore     │
│    │           (reads/writes via host)         │
│    └─ false → LocalStorageAccountStore        │
│               (existing KStore behavior)       │
└──────────────────────────────────────────────┘
```

### File Watching for Live Sync

When in companion mode, the native messaging host watches the `accounts.json` file for changes (using Java NIO `WatchService` / `FileChannel`). When the desktop app modifies accounts (add/delete/import), the host pushes an `accounts_changed` message to the extension, keeping the UI in sync without polling.

---

## Step-by-step Implementation Plan

### Phase 0 — Native Messaging Host in Desktop App

The native messaging host is a small standalone JVM executable (or a mode of the existing desktop app) that the browser launches via stdio.

#### 0.1 Create Native Messaging Host module or entry point

**Option A — Separate thin JVM module** (recommended):
- Create a `nmhApp/` module (or similar) that depends on `sharedLib` and reads the same `accounts.json` file.
- Pro: Small binary, fast startup, no Compose UI dependency.
- Con: Separate build artifact to distribute.

**Option B — Desktop app `--nmh` flag**:
- Add a `--native-messaging-host` CLI flag to the desktop app's `main()` that switches to stdio mode.
- Pro: Single binary to distribute.
- Con: Larger process for a simple stdio bridge; Compose Desktop runtime loaded unnecessarily.

**Recommendation**: Option A for production, but Option B is acceptable for v1 if packaging is simpler.

#### 0.2 Implement stdio JSON message loop

```kotlin
// Pseudocode for the NMH entry point
fun main() {
    val input = System.`in`.buffered()
    val output = System.out.buffered()

    while (true) {
        val message = readNativeMessage(input) ?: break
        val response = handleMessage(message)
        writeNativeMessage(output, response)
    }
}

fun readNativeMessage(input: BufferedInputStream): JsonObject? {
    val lengthBytes = ByteArray(4)
    if (input.read(lengthBytes) != 4) return null
    val length = ByteBuffer.wrap(lengthBytes).order(ByteOrder.LITTLE_ENDIAN).getInt()
    val messageBytes = ByteArray(length)
    input.readFully(messageBytes)
    return Json.parseToJsonElement(messageBytes.decodeToString()).jsonObject
}

fun writeNativeMessage(output: BufferedOutputStream, message: JsonObject) {
    val bytes = message.toString().toByteArray()
    val lengthBytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(bytes.size).array()
    output.write(lengthBytes)
    output.write(bytes)
    output.flush()
}
```

#### 0.3 Implement message handlers

- `ping` → return app version + storage path
- `get_accounts` → read `accounts.json` from AppDirs path, return parsed accounts
- `get_storage_path` → return the AppDirs path string
- `watch_accounts` → start file watcher on `accounts.json`, push `accounts_changed` on modifications

#### 0.4 File watching for live sync

Use Java NIO `WatchService` to monitor the AppDirs directory for `accounts.json` changes:

```kotlin
fun watchAccountsFile(storagePath: Path, onChanged: (List<StoredAccount>) -> Unit) {
    val watchService = FileSystems.getDefault().newWatchService()
    storagePath.parent.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY)
    // ... poll for events, re-read file, invoke callback
}
```

### Phase 1 — Extension-side Native Messaging Client (TypeScript)

#### 1.1 Add TypeScript native messaging module

Create `composeApp/src/wasmJsMain/typescript/src/native-messaging.mts`:

```typescript
const NMH_NAME = "tech.arnav.twofac";

export interface NmhResponse {
  type: string;
  [key: string]: unknown;
}

export const isNativeMessagingAvailable = (): boolean => {
  return typeof globalThis.chrome?.runtime?.sendNativeMessage === "function";
};

export const sendNativeMessage = (
  message: Record<string, unknown>
): Promise<NmhResponse | null> => {
  return new Promise((resolve) => {
    if (!isNativeMessagingAvailable()) {
      resolve(null);
      return;
    }
    chrome.runtime.sendNativeMessage(NMH_NAME, message, (response) => {
      if (chrome.runtime.lastError) {
        console.warn("NMH error:", chrome.runtime.lastError.message);
        resolve(null);
      } else {
        resolve(response as NmhResponse);
      }
    });
  });
};

export const connectNativePort = (): chrome.runtime.Port | null => {
  if (!isNativeMessagingAvailable()) return null;
  try {
    return chrome.runtime.connectNative(NMH_NAME);
  } catch {
    return null;
  }
};

export const pingDesktopApp = async (): Promise<boolean> => {
  const response = await sendNativeMessage({ type: "ping" });
  return response?.type === "pong";
};

export const getAccountsFromDesktop = async (): Promise<string | null> => {
  const response = await sendNativeMessage({ type: "get_accounts" });
  if (response?.type === "accounts") {
    return JSON.stringify(response.accounts);
  }
  return null;
};
```

#### 1.2 Add `nativeMessaging` permission to manifest

Update `composeApp/extension/manifest.base.json`:

```json
{
  "permissions": ["storage", "sidePanel", "nativeMessaging"],
  ...
}
```

#### 1.3 Create Kotlin/Wasm external declarations

Create `composeApp/src/wasmJsMain/kotlin/tech/arnav/twofac/companion/NativeMessagingInterop.kt`:

```kotlin
@JsModule("./native-messaging.mjs")
external object NativeMessagingInterop {
    fun isNativeMessagingAvailable(): Boolean
    fun pingDesktopApp(): JsPromise<JsBoolean>
    fun getAccountsFromDesktop(): JsPromise<JsString?>
}
```

### Phase 2 — Companion-aware Account Storage Switching in Wasm/Kotlin

#### 2.1 Create `DesktopCompanionClient` in wasmJsMain

A Kotlin wrapper around the TypeScript native messaging interop:

```kotlin
// composeApp/src/wasmJsMain/kotlin/tech/arnav/twofac/companion/DesktopCompanionClient.kt
class DesktopCompanionClient {
    suspend fun isDesktopAppAvailable(): Boolean { ... }
    suspend fun getAccounts(): List<StoredAccount>? { ... }
    fun connectForUpdates(onAccountsChanged: (List<StoredAccount>) -> Unit): Closeable { ... }
}
```

#### 2.2 Create `CompanionAwareAccountStore`

A KStore-compatible wrapper that delegates to either native messaging or localStorage:

```kotlin
// composeApp/src/wasmJsMain/kotlin/tech/arnav/twofac/companion/CompanionAwareAccountStore.kt
class CompanionAwareAccountStore(
    private val localStore: KStore<List<StoredAccount>>,
    private val companionClient: DesktopCompanionClient,
) {
    enum class Mode { STANDALONE, COMPANION }

    val currentMode: StateFlow<Mode>

    suspend fun detectMode(): Mode {
        return if (companionClient.isDesktopAppAvailable()) Mode.COMPANION else Mode.STANDALONE
    }

    // Delegates get/set to appropriate backing store based on mode
    suspend fun get(): List<StoredAccount>? { ... }
    suspend fun set(accounts: List<StoredAccount>) { ... }
}
```

#### 2.3 Update DI module (`WasmModules.kt`)

Wire the `CompanionAwareAccountStore` into the existing Koin DI graph so the rest of the app is unaware of the mode switching:

```kotlin
val wasmCompanionModule = module {
    single { DesktopCompanionClient() }
    single { CompanionAwareAccountStore(get(), get()) }
}
```

#### 2.4 Modify `AppDirUtils.wasmJs.kt` to support companion mode

The `createAccountsStore()` function needs to be aware of companion mode. Two approaches:

**Option A** (Recommended): Keep `createAccountsStore()` returning the local KStore as-is. The `CompanionAwareAccountStore` sits above it and conditionally bypasses it. This minimizes changes to existing code.

**Option B**: Make `createAccountsStore()` return a wrapper store. More invasive.

### Phase 3 — Extension UI: Companion Status Indicator & Settings

#### 3.1 Companion status indicator

Add a small indicator in the extension UI (Settings screen or app bar) showing current mode:

- 🟢 **Connected to Desktop App** — companion mode active
- 🔴 **Standalone Mode** — using local browser storage
- 🔄 **Detecting...** — checking for desktop app

#### 3.2 Manual mode override in Settings

Allow users to force standalone mode even when the desktop app is available (useful for debugging or preference):

- Toggle: "Use Desktop App Storage" (auto-detected, manually overridable)
- Info text explaining what companion mode means

#### 3.3 One-time migration prompt

When the extension first detects a desktop app and has existing localStorage accounts:
- Show a prompt: "Desktop app detected. Merge your browser accounts into the desktop app, or switch to using desktop accounts only?"
- Options: "Merge & Switch", "Switch (discard browser accounts)", "Stay Standalone"

### Phase 4 — Desktop App Installer Registers Native Messaging Host

#### 4.1 Generate host manifest during build

Add a Gradle task that generates `tech.arnav.twofac.json` with the correct binary path for each platform.

#### 4.2 Register manifest during installation

**Windows (.msi via WiX):**
- Add registry keys during install for Chrome and Firefox native messaging host paths.
- Point to the NMH executable path in the install directory.

**macOS (.dmg):**
- Post-install script copies manifest to:
  - `~/Library/Application Support/Google/Chrome/NativeMessagingHosts/`
  - `~/Library/Application Support/Mozilla/NativeMessagingHosts/`
  - (and Chromium/Brave variants)

**Linux (.deb):**
- Post-install script copies manifest to:
  - `~/.config/google-chrome/NativeMessagingHosts/`
  - `~/.mozilla/native-messaging-hosts/`
  - (and Chromium variants)

#### 4.3 Unregistration on uninstall

Reverse the registration steps during uninstall.

### Phase 5 — Security Hardening & Review

#### 5.1 Extension ID pinning

The host manifest `allowed_origins` / `allowed_extensions` must list only the published TwoFac extension IDs. During development, use a wildcard or dev ID.

#### 5.2 Message validation

- Host validates all incoming messages against a schema before processing.
- Extension validates all responses from the host.
- Reject unknown message types.

#### 5.3 Passkey / encryption alignment

When companion mode is active, the extension should **not** independently encrypt accounts (since the desktop app handles its own passkey flow). The extension's WebAuthn-based passkey session management should be:
- **Companion mode**: Bypassed — accounts come pre-decrypted from the desktop app (which has its own unlock flow).
- **Standalone mode**: Unchanged — existing WebAuthn flow applies.

#### 5.4 Rate limiting and timeout

- Host should timeout if no messages received within 60 seconds (configurable).
- Extension should timeout native messaging calls after 5 seconds and fall back to standalone.

#### 5.5 Code signing verification

On macOS and Windows, the extension should ideally verify that the native messaging host binary is signed by the expected publisher before trusting it. This is handled at the OS/browser level for native messaging, but additional verification can be added.

### Phase 6 — Testing, Documentation & Release

#### 6.1 Unit tests

- NMH message parsing and serialization (JVM tests in the host module)
- `CompanionAwareAccountStore` mode detection and delegation
- Message protocol round-trip tests

#### 6.2 Integration tests

- End-to-end test: extension ↔ NMH ↔ accounts.json file
- Mode switching test: start standalone, install desktop app, detect transition
- File watcher test: modify accounts.json externally, verify extension updates

#### 6.3 Manual testing matrix

| Scenario | Chrome | Firefox | Edge |
|---|---|---|---|
| Desktop app installed, extension installed → companion mode | ✅ | ✅ | ✅ |
| Desktop app NOT installed → standalone mode | ✅ | ✅ | ✅ |
| Desktop app installed then closed → graceful fallback | ✅ | ✅ | ✅ |
| Desktop app opened after extension → mode switch | ✅ | ✅ | ✅ |
| Accounts added on desktop → extension updates | ✅ | ✅ | ✅ |
| Multiple browser windows open simultaneously | ✅ | ✅ | ✅ |

#### 6.4 Documentation

- Update `composeApp/AGENTS.md` with companion mode architecture
- Add user-facing docs on how to enable/troubleshoot companion mode
- Document the native messaging protocol for future extension developers

---

## File-level Change Map (Planned)

### New files

| File | Description |
|---|---|
| `nmhApp/` (or `composeApp/src/desktopMain/.../nmh/`) | Native Messaging Host module/package |
| `nmhApp/src/main/kotlin/.../NativeMessagingHost.kt` | Stdio message loop entry point |
| `nmhApp/src/main/kotlin/.../NmhMessageHandler.kt` | Message routing and account reading |
| `nmhApp/src/main/kotlin/.../NmhFileWatcher.kt` | File watcher for accounts.json changes |
| `nmhApp/src/main/kotlin/.../NmhProtocol.kt` | Message types and serialization |
| `composeApp/src/wasmJsMain/typescript/src/native-messaging.mts` | TypeScript native messaging client |
| `composeApp/src/wasmJsMain/kotlin/.../companion/NativeMessagingInterop.kt` | Kotlin external declarations |
| `composeApp/src/wasmJsMain/kotlin/.../companion/DesktopCompanionClient.kt` | Kotlin companion client |
| `composeApp/src/wasmJsMain/kotlin/.../companion/CompanionAwareAccountStore.kt` | Mode-switching store wrapper |
| `composeApp/extension/tech.arnav.twofac.json` | Template native messaging host manifest |

### Modified files

| File | Change |
|---|---|
| `composeApp/extension/manifest.base.json` | Add `nativeMessaging` permission |
| `composeApp/extension/background.js` | Add companion detection + heartbeat logic |
| `composeApp/src/wasmJsMain/kotlin/.../di/WasmModules.kt` | Wire companion DI |
| `composeApp/src/wasmJsMain/kotlin/.../screens/PlatformSettings.wasmJs.kt` | Companion status UI |
| `settings.gradle.kts` | Include `nmhApp` module (if separate module) |
| `composeApp/build.gradle.kts` | NMH manifest generation task |
| `.github/workflows/release.yml` | Package NMH binary + register manifests in installers |

---

## Comparison with Watch Companion Approach

| Aspect | Watch Companion | Browser Extension Companion |
|---|---|---|
| **Transport** | Wearable Data Layer / WCSession | Chrome/Firefox Native Messaging API |
| **Discovery** | `CapabilityClient` / `WCSession.isSupported()` | `chrome.runtime.sendNativeMessage("ping")` |
| **Data flow** | Phone → Watch (one-way push) | Desktop ↔ Extension (bidirectional) |
| **Payload** | `WatchSyncSnapshot` JSON | Same `StoredAccount` JSON as `accounts.json` |
| **Offline/fallback** | Watch uses cached snapshot | Extension uses localStorage |
| **Common interface** | `CompanionSyncCoordinator` | New: `DesktopCompanionClient` (could implement `CompanionSyncCoordinator` if generalized) |
| **Trigger** | Account changes + periodic WorkManager | File watcher push + on-demand pull |

---

## Open Questions

1. **Separate NMH module vs. desktop app flag**: Which approach is preferred for v1?
2. **Extension ID**: Is the Chrome Web Store extension ID already known, or will it be determined at publish time?
3. **Bidirectional writes**: Should the extension be able to add/edit/delete accounts that are written back to the desktop app's `accounts.json`, or is it read-only from desktop?
4. **Multiple browsers**: If the user has both Chrome and Firefox with the extension, both would connect to the same NMH — any concerns?
5. **Tray mode dependency**: Should companion mode require the desktop app to be running in tray mode, or should the NMH be independently launchable by the browser?
