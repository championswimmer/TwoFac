---
name: CLI Installer Script Plan
status: Planned
progress:
  - "[ ] Phase 0 - Confirm release asset contract and hosting path"
  - "[ ] Phase 1 - Add website-hosted installer entrypoint"
  - "[ ] Phase 2 - Implement OS/arch detection and release resolution"
  - "[ ] Phase 3 - Implement download, extract, and install flow"
  - "[ ] Phase 4 - Document installer usage in website and repo docs"
  - "[ ] Phase 5 - Validate static publishing and installer behavior"
---

# CLI Installer Script Plan

## Goal

Add a hosted installer script under `website/` so macOS and Linux users can run:

```bash
curl -fsSL https://twofac.app/install.sh | bash -s --
```

and receive the correct `2fac` CLI binary from the latest GitHub Release for `championswimmer/TwoFac`.

Windows remains supported via the existing `.exe` release asset and is out of scope for this shell installer.

---

## Current State

### Release asset contract today

The current release workflow publishes these CLI assets:

- `2fac-linux-x64.tar.gz`
- `2fac-macos-arm64.tar.gz`
- `2fac-macos-x64.tar.gz`
- `2fac-windows-x64.exe`

Important detail: the Unix archives currently contain **`2fac.kexe`** internally, not `2fac`.

### Website hosting model

The website is built with Vite + `vite-ssg` and deployed as static files only. Files placed in `website/public/` are emitted at the root of the built site, so:

- `website/public/install.sh` -> `https://twofac.app/install.sh`

### Current UX gap

The website `DownloadPage.vue` currently sends CLI users to GitHub Releases manually. There is no one-command installer path yet.

---

## Research Summary

### Installer-script hardening

Research on curl-piped shell installers points to a few baseline expectations:

1. Use `bash` intentionally and start with `set -euo pipefail`.
2. Fail loudly on unsupported OS/arch combinations instead of guessing.
3. Use `mktemp -d` and `trap` cleanup for downloads/extraction.
4. Avoid requiring `sudo` for the default path; prefer a user-writable fallback such as `~/.local/bin`.
5. Download with `curl -fL` so HTTP failures surface immediately.

### Latest-release lookup strategy

There are two reasonable ways to find the latest release:

1. **GitHub REST API** (`/repos/:owner/:repo/releases/latest`)
2. **GitHub latest redirect** (`/releases/latest`)

For this installer, the best fit is:

- **Primary approach:** resolve the latest tag through the GitHub `releases/latest` redirect
- **Reason:** the asset names are deterministic, so the script only needs the tag, not full asset metadata
- **Benefit:** no `jq` or other JSON parser dependency in a bootstrap script

Planned flow:

1. Resolve the effective latest-release tag URL.
2. Derive the tag (`vX.Y.Z`) from that URL.
3. Construct the asset download URL directly:

```text
https://github.com/championswimmer/TwoFac/releases/download/<tag>/<asset-name>
```

If later testing shows the redirect approach is unreliable in practice, the fallback option is to switch to the REST API for tag resolution only.

### Asset-selection implications

Because the release asset names are already normalized, the installer can use a small explicit matrix:

| OS | Arch | Asset |
|---|---|---|
| macOS | arm64 | `2fac-macos-arm64.tar.gz` |
| macOS | x86_64 | `2fac-macos-x64.tar.gz` |
| Linux | x86_64 | `2fac-linux-x64.tar.gz` |

Unsupported combinations should fail clearly:

- Linux arm64 / aarch64
- Windows via this `bash` installer
- Any other unknown `uname -s` / `uname -m` value

---

## Proposed Implementation

## Phase 0 - Confirm release asset contract and hosting path

1. Treat the current release naming as the contract the installer must support first.
2. Do **not** block the installer on changing release packaging.
3. Confirm that `website/public/install.sh` is the simplest path to publish `https://twofac.app/install.sh`.
4. Keep the script independent of the website app bundle so it remains directly fetchable as a plain shell file.

### Scope guardrail

This first implementation should consume the **existing** release assets as-is. Renaming `2fac.kexe` inside the tarballs can be a separate cleanup task later.

---

## Phase 1 - Add website-hosted installer entrypoint

1. Create `website/public/install.sh`.
2. Keep the file self-contained and dependency-light.
3. Ensure the script is served with the exact root path expected by the public install command.
4. Preserve executable readability in git and static output; execution permission on the hosted file is not required because the script is piped to `bash`.

### Recommended script contract

Support a minimal set of overrides for testing and advanced use:

- `TWOFAC_INSTALL_DIR` — explicit install destination
- `TWOFAC_RELEASE_TAG` — install a pinned release instead of latest
- `TWOFAC_REPO` — default `championswimmer/TwoFac`, useful for local forks if needed

These are optional, but they make the installer easier to test without forking the script.

---

## Phase 2 - Implement OS/arch detection and release resolution

1. Detect OS from `uname -s`.
   - `Darwin` -> `macos`
   - `Linux` -> `linux`
2. Detect CPU architecture from `uname -m`.
   - `x86_64` / `amd64` -> `x64`
   - `arm64` / `aarch64` -> `arm64`
3. Map the normalized pair to the release asset name.
4. Reject unsupported pairs with a direct message that points users to GitHub Releases for manual downloads.

### Planned resolver algorithm

1. If `TWOFAC_RELEASE_TAG` is set, use it directly.
2. Otherwise resolve the latest tag from:

```text
https://github.com/championswimmer/TwoFac/releases/latest
```

3. Build the asset URL from `<tag>` + `<asset-name>`.
4. Print the resolved version and asset to stderr before download.

### Failure behavior

If the script cannot resolve a release tag or the expected asset does not exist:

1. Exit non-zero.
2. Print the expected asset name.
3. Print the release page URL.
4. Avoid fallback guesses across architectures.

---

## Phase 3 - Implement download, extract, and install flow

1. Verify required tools early:
   - `curl`
   - `tar`
   - `mktemp`
2. Create a temp directory and register cleanup with `trap`.
3. Download the chosen asset with `curl -fL`.
4. Extract Unix tarballs and locate `2fac.kexe`.
5. Rename the extracted binary to the final installed command name: `2fac`.
6. Mark it executable and install it atomically.

### Install destination policy

Preferred order:

1. `TWOFAC_INSTALL_DIR` if set
2. `/usr/local/bin` if writable
3. `$HOME/.local/bin`

This avoids prompting for `sudo` inside a remote script while still giving a clean global install when permissions already allow it.

### PATH guidance

If the chosen destination is not already on `PATH`, the script should print the exact export line the user needs, for example:

```bash
export PATH="$HOME/.local/bin:$PATH"
```

### Replacement policy

If `2fac` already exists:

1. Overwrite it intentionally with the new binary
2. Make the replacement atomic via temp file + `mv`
3. Print the installed version path clearly

### Future-proofing

Even though current Unix assets are `.tar.gz`, structure the script so handling a future direct binary asset would only require a small branch instead of a rewrite.

---

## Phase 4 - Document installer usage in website and repo docs

1. Update `website/src/pages/DownloadPage.vue` to promote the one-line installer for macOS/Linux CLI users.
2. Add the exact command snippet:

```bash
curl -fsSL https://twofac.app/install.sh | bash -s --
```

3. Keep GitHub Releases linked for:
   - Windows CLI downloads
   - manual installs
   - unsupported architectures
4. Add or update README CLI install guidance so the repo docs and website stay aligned.

### Messaging notes

- Be explicit that the shell installer currently targets **macOS and Linux**
- Keep Windows on the existing direct-download path
- Mention that unsupported architectures should use the releases page directly

---

## Phase 5 - Validate static publishing and installer behavior

### Website validation

1. Build the website:

```bash
cd website
npm run build
```

2. Validate SSG output:

```bash
cd website
npm run validate:ssg
```

3. Confirm `dist/install.sh` exists after the build.

### Installer smoke-test matrix

1. **macOS arm64**
   - resolves `2fac-macos-arm64.tar.gz`
2. **macOS x64**
   - resolves `2fac-macos-x64.tar.gz`
3. **Linux x64**
   - resolves `2fac-linux-x64.tar.gz`
4. **Linux arm64**
   - fails clearly as unsupported

### Behavioral checks

1. Fresh install to `~/.local/bin`
2. Reinstall over an existing `2fac`
3. PATH warning when the target directory is not exported
4. Clear failure if GitHub returns 404 for the resolved asset
5. Clear failure if `curl` or `tar` is missing

---

## Files Likely Touched

- `website/public/install.sh` (new)
- `website/src/pages/DownloadPage.vue`
- `README.md`

Potentially, if follow-up cleanup is desired later:

- `.github/workflows/release.yml` (only if we later decide to rename `2fac.kexe` inside the Unix tarballs)

---

## Out of Scope for This First Pass

- PowerShell installer for Windows
- Homebrew / apt / yum packaging changes
- Release-signature or checksum verification unless release assets begin publishing checksum files
- Reworking the existing CLI release asset naming

---

## Recommended Execution Order

1. Ship `website/public/install.sh` against the current release asset contract.
2. Update website + README docs to surface the installer command.
3. Validate the website build emits `dist/install.sh`.
4. Smoke-test install and reinstall flows on supported platforms.
5. Only after that, consider cleanup work such as renaming `2fac.kexe` in release archives.
