# sharedLib AGENTS.md

`sharedLib` contains the cross-platform 2FA business logic, cryptography, storage models, and public APIs used by all TwoFac applications.

## What this module does
It acts as the core engine. It manages generating OTPs (TOTP and HOTP), parsing and building `otpauth://` URIs, deriving cryptographic keys, and executing encrypted storage read/writes. It also handles external responsibilities like importing data from other authenticators (Authy, 2FAS, Ente) and orchestrating backup serialization.

## Dependencies
This module is the base library and does not depend on any other internal modules.

## Platforms
The code in this module compiles to:
- **JVM**: For Android and Desktop applications.
- **Native**: For CLI binaries (macOS, Windows, Linux) and Apple platforms (iOS, watchOS framework exports).
- **WasmJs**: For the Web app and browser extensions.

## Libraries Used
- [cryptography-kotlin](https://github.com/whyoleg/cryptography-kotlin) - Multiplatform cryptography (used for AES-GCM, HMAC, PBKDF2).
- [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization) - For JSON encoding/decoding of backups, watch payloads, and imported formats.
- [kotlinx.coroutines](https://github.com/Kotlin/kotlinx.coroutines) - For asynchronous operations and non-blocking cryptography.

## Code Structure
- `src/commonMain/kotlin/tech/arnav/twofac/lib/`:
  - `TwoFacLib.kt`: The main facade exposing business operations to the apps.
  - `otp/`: Implementations of `HOTP` and `TOTP` interfaces based on RFCs.
  - `crypto/`: `CryptoTools` defining hashing, encryption, and Base32 encoding logic.
  - `storage/`: The `Storage` contract and the `StoredAccount` data model.
  - `uri/`: Logic for parsing and generating `otpauth://` strings.
  - `importer/`: Adapters for extracting secrets from 3rd-party exports.
  - `backup/`: Orchestration for encrypting and managing backup files.
  - `watchsync/`: Payloads and codecs for serializing accounts to send to wearable companions.
