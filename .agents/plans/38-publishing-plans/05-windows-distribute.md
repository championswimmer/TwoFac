# Windows Distribution: TwoFac Desktop App

Step-by-step guide for distributing the TwoFac Compose Desktop app on Windows via direct download, Microsoft Store, and winget.

## App Details

- **App Name**: TwoFac
- **Package Name**: TwoFac
- **Version**: 1.6.0
- **Runtime**: JVM (Compose Desktop, Kotlin Multiplatform)
- **Installer**: MSI (built via `./gradlew packageMsi`)
- **Icon**: `composeApp/src/desktopMain/resources/icons/windows.ico`
- **Category**: Security / Utilities (2FA Authenticator)

## Prerequisites

- Windows 10/11 build machine (or CI with Windows runner)
- Windows SDK installed (provides `signtool.exe`)
- JDK 21+ and Gradle configured (see project root)

---

## 1. Simple Distributable (Direct Download)

### 1.1 Obtain a Code Signing Certificate

Microsoft requires signed executables for SmartScreen trust. As of 2025, Microsoft enforces **EV certificates** or **Azure Trusted Signing** for immediate SmartScreen reputation.

| Option | Description | Cost (approx.) |
|--------|-------------|-----------------|
| **Standard OV cert** | Organization Validation; builds SmartScreen reputation over time | $200–500/yr |
| **EV cert** | Extended Validation; immediate SmartScreen reputation, hardware token required | $300–800/yr |
| **Azure Trusted Signing** | Microsoft's cloud HSM signing service; EV-equivalent trust, no physical token | Azure subscription pricing |

**Where to buy:**
- [DigiCert](https://www.digicert.com/signing/code-signing-certificates) — OV & EV
- [GlobalSign](https://www.globalsign.com/en/code-signing-certificate) — OV & EV
- [Sectigo](https://sectigo.com/code-signing-certificates) — OV & EV

**Azure Trusted Signing** (recommended for CI/CD):
- Docs: [Azure Trusted Signing overview](https://learn.microsoft.com/en-us/azure/trusted-signing/)
- Blog: [Signing with Azure Trusted Signing and GitHub Actions](https://www.hanselman.com/blog/automatically-signing-a-windows-exe-with-azure-trusted-signing-dotnet-sign-and-github-actions)

### 1.2 Build the MSI

```bash
./gradlew packageMsi
```

Output: `composeApp/build/compose/binaries/main/msi/TwoFac-1.6.0.msi`

### 1.3 Sign the MSI with `signtool.exe`

`signtool.exe` is in the Windows SDK, typically at:
`C:\Program Files (x86)\Windows Kits\10\bin\<version>\x64\signtool.exe`

**Sign with certificate from store (EV/OV on hardware token):**
```cmd
signtool sign /a /fd sha256 /tr http://timestamp.digicert.com /td sha256 "TwoFac-1.6.0.msi"
```

**Sign with PFX file:**
```cmd
signtool sign /f cert.pfx /p <password> /fd sha256 /tr http://timestamp.digicert.com /td sha256 "TwoFac-1.6.0.msi"
```

**Sign with Azure Trusted Signing:**
```cmd
signtool sign /v /dlib "Azure.CodeSigning.Dlib.dll" /dmdf "metadata.json" /fd sha256 /tr http://timestamp.acs.microsoft.com /td sha256 "TwoFac-1.6.0.msi"
```

**Verify:**
```cmd
signtool verify /v /pa "TwoFac-1.6.0.msi"
```

- Docs: [SignTool overview](https://learn.microsoft.com/en-us/windows/win32/seccrypto/signtool)
- Docs: [Using SignTool to sign a file](https://learn.microsoft.com/en-us/windows/win32/seccrypto/using-signtool-to-sign-a-file)
- Docs: [Verify a file signature](https://learn.microsoft.com/en-us/windows/win32/seccrypto/using-signtool-to-verify-a-file-signature)

### 1.4 Host for Download

- Upload the signed MSI to **GitHub Releases** on the TwoFac repository
- Include SHA-256 checksum in the release notes:
  ```powershell
  Get-FileHash "TwoFac-1.6.0.msi" -Algorithm SHA256
  ```
- Link from project website/README

---

## 2. Microsoft Store

### 2.1 Register a Partner Center Account

1. Go to [Microsoft Partner Center](https://partner.microsoft.com/dashboard)
2. Sign in with a Microsoft account
3. Register as a developer — **$19 USD one-time fee**
4. Complete profile: contact info, payment/banking, tax info

- Docs: [Create a Partner Center account](https://learn.microsoft.com/en-us/windows/apps/publish/pcgs-create-partner-account)
- Docs: [Manage your account](https://learn.microsoft.com/en-us/windows/apps/publish/pcgs-account-management)

### 2.2 Package MSI as MSIX

The Microsoft Store requires **MSIX** format for Win32 desktop apps. You must convert the MSI to MSIX.

**Option A: MSIX Packaging Tool (GUI)**
1. Install [MSIX Packaging Tool](https://learn.microsoft.com/en-us/windows/msix/packaging-tool/tool-overview) from the Microsoft Store
2. Launch → "Create new package" → point to `TwoFac-1.6.0.msi`
3. The tool captures the installation and produces an `.msix` file
4. Edit the `AppxManifest.xml` (set publisher, display name, capabilities)

**Option B: Manual MSIX Packaging Project (Visual Studio)**
1. Open Visual Studio → File → New → "Windows Application Packaging Project"
2. Add the TwoFac app output as a reference
3. Configure `Package.appxmanifest` with identity, icons, capabilities
4. Build to produce `.msix`

- Docs: [MSIX Packaging Tool overview](https://learn.microsoft.com/en-us/windows/msix/packaging-tool/tool-overview)
- Docs: [Package a desktop app](https://learn.microsoft.com/en-us/windows/msix/desktop/desktop-to-uwp-packaging-dot-net)
- Docs: [Convert MSI to MSIX](https://learn.microsoft.com/en-us/windows/msix/migrate-from-msi/msi-to-msix)

### 2.3 Sign the MSIX

```cmd
signtool sign /fd SHA256 /a /f cert.pfx /p <password> "TwoFac.msix"
```

- Docs: [Sign an MSIX package](https://learn.microsoft.com/en-us/windows/msix/package/sign-appx)

### 2.4 Validate with Windows App Certification Kit (WACK)

Run WACK on the MSIX before submission to catch issues early:

- Docs: [Validate your app](https://learn.microsoft.com/en-us/windows/msix/validate/validate-your-app)

### 2.5 Create Store Listing

In Partner Center, fill out the submission form:

| Section | TwoFac Values |
|---------|---------------|
| **Display name** | TwoFac |
| **Description** | Two-factor authentication code generator. Secure, open-source, cross-platform. |
| **Category** | Security / Utilities |
| **Icons** | 300×300 and 512×512 PNG (derive from `windows.ico`) |
| **Screenshots** | Min 3 screenshots at 1920×1080 |
| **Privacy policy URL** | _(your privacy policy URL)_ |
| **Support URL** | _(your support/GitHub URL)_ |
| **Age rating** | Complete IARC questionnaire (likely rated for all ages) |
| **Pricing** | Free |
| **Markets** | All available (190+ countries) |

- Docs: [Create store listings](https://learn.microsoft.com/en-us/windows/apps/publish/pcgs-create-store-listings)
- Docs: [Store policies](https://learn.microsoft.com/en-us/legal/windows/developer-policies)

### 2.6 Submit for Review

1. Upload signed MSIX under "Packages"
2. Click "Send for certification"
3. **Timeline:**
   - Automated validation (WACK + malware scan): minutes
   - Manual review: 1–3 business days (typically 24–48 hours)
   - Total: up to 5 business days
4. If rejected — fix issues noted in Partner Center and resubmit (unlimited, free)

- Docs: [Publish your app](https://learn.microsoft.com/en-us/windows/apps/publish/pcgs-publish-app-to-store)
- Docs: [Store certification policies](https://learn.microsoft.com/en-us/windows/apps/publish/store-certification-policies)

---

## 3. Winget (Windows Package Manager)

### 3.1 Choose a Package Identifier

Convention: `Publisher.ProductName` (reverse-DNS style)

Recommended: **`Arnav.TwoFac`** or **`tech.arnav.TwoFac`**

Check for conflicts: <https://github.com/microsoft/winget-pkgs/tree/master/manifests>

### 3.2 Generate the Manifest

Install `wingetcreate`:
```cmd
winget install Microsoft.WingetCreate
```

Create the manifest interactively:
```cmd
wingetcreate new https://github.com/<owner>/TwoFac/releases/download/v1.6.0/TwoFac-1.6.0.msi
```

Or create manually. The manifest lives at:
```
manifests/a/Arnav/TwoFac/1.6.0/
├── Arnav.TwoFac.installer.yaml
├── Arnav.TwoFac.locale.en-US.yaml
└── Arnav.TwoFac.yaml
```

**`Arnav.TwoFac.yaml`** (version manifest):
```yaml
PackageIdentifier: Arnav.TwoFac
PackageVersion: 1.6.0
DefaultLocale: en-US
ManifestType: version
ManifestVersion: 1.9.0
```

**`Arnav.TwoFac.installer.yaml`**:
```yaml
PackageIdentifier: Arnav.TwoFac
PackageVersion: 1.6.0
InstallerType: msi
Installers:
  - Architecture: x64
    InstallerUrl: https://github.com/<owner>/TwoFac/releases/download/v1.6.0/TwoFac-1.6.0.msi
    InstallerSha256: <SHA256_HASH>
ManifestType: installer
ManifestVersion: 1.9.0
```

**`Arnav.TwoFac.locale.en-US.yaml`**:
```yaml
PackageIdentifier: Arnav.TwoFac
PackageVersion: 1.6.0
PackageLocale: en-US
Publisher: Arnav
PackageName: TwoFac
License: <your license>
ShortDescription: Two-factor authentication code generator
Tags:
  - 2fa
  - totp
  - authenticator
  - security
ManifestType: defaultLocale
ManifestVersion: 1.9.0
```

Generate the SHA-256 hash:
```cmd
winget hash "TwoFac-1.6.0.msi"
```

### 3.3 Validate Locally

```cmd
winget validate --manifest manifests/a/Arnav/TwoFac/1.6.0/
```

### 3.4 Submit a Pull Request

**Manual method:**
1. Fork [microsoft/winget-pkgs](https://github.com/microsoft/winget-pkgs)
2. Add manifest files under `manifests/a/Arnav/TwoFac/1.6.0/`
3. Commit and push to your fork
4. Open a PR targeting `main` on `microsoft/winget-pkgs`
5. Automated CI runs: schema validation, SHA-256 check, AV scan, installer verification
6. Microsoft maintainers review and merge (typically a few days)

**Automated method (recommended for updates):**
```cmd
wingetcreate submit --token <GITHUB_PAT> manifests/a/Arnav/TwoFac/1.6.0/
```

Or create + submit in one step:
```cmd
wingetcreate new --submit --token <GITHUB_PAT> https://github.com/<owner>/TwoFac/releases/download/v1.6.0/TwoFac-1.6.0.msi
```

### 3.5 Automate with GitHub Actions

Add a workflow that runs on new GitHub Releases to auto-submit winget updates:

```yaml
# .github/workflows/winget-publish.yml
name: Publish to Winget
on:
  release:
    types: [published]
jobs:
  winget:
    runs-on: windows-latest
    steps:
      - uses: vedantmgoyal9/winget-releaser@main
        with:
          identifier: Arnav.TwoFac
          token: ${{ secrets.WINGET_TOKEN }}
```

Alternatively, use `wingetcreate update` in a custom step.

- Docs: [Windows Package Manager overview](https://learn.microsoft.com/en-us/windows/package-manager/winget/)
- Docs: [winget validate](https://learn.microsoft.com/en-us/windows/package-manager/winget/validate)
- Repo: [microsoft/winget-pkgs](https://github.com/microsoft/winget-pkgs)
- Repo: [microsoft/wingetcreate](https://github.com/microsoft/wingetcreate)
- Action: [vedantmgoyal9/winget-releaser](https://github.com/vedantmgoyal9/winget-releaser)

---

## Quick Reference Links

| Resource | URL |
|----------|-----|
| SignTool overview | https://learn.microsoft.com/en-us/windows/win32/seccrypto/signtool |
| Azure Trusted Signing | https://learn.microsoft.com/en-us/azure/trusted-signing/ |
| Partner Center | https://partner.microsoft.com/dashboard |
| MSIX Packaging Tool | https://learn.microsoft.com/en-us/windows/msix/packaging-tool/tool-overview |
| MSI to MSIX conversion | https://learn.microsoft.com/en-us/windows/msix/migrate-from-msi/msi-to-msix |
| Store certification policies | https://learn.microsoft.com/en-us/windows/apps/publish/store-certification-policies |
| winget docs | https://learn.microsoft.com/en-us/windows/package-manager/winget/ |
| winget-pkgs repo | https://github.com/microsoft/winget-pkgs |
| wingetcreate repo | https://github.com/microsoft/wingetcreate |

---

## Checklist

### Code Signing
- [ ] Choose certificate type: OV / EV / Azure Trusted Signing
- [ ] Purchase or set up certificate
- [ ] Install Windows SDK (for `signtool.exe`)
- [ ] Build MSI: `./gradlew packageMsi`
- [ ] Sign MSI with `signtool`
- [ ] Verify signature: `signtool verify /v /pa`
- [ ] Upload signed MSI to GitHub Releases with SHA-256 checksum

### Microsoft Store
- [ ] Register Partner Center account ($19 USD)
- [ ] Convert MSI → MSIX (MSIX Packaging Tool or Visual Studio)
- [ ] Sign the MSIX package
- [ ] Validate with Windows App Certification Kit (WACK)
- [ ] Prepare store assets: icons (300×300, 512×512), screenshots (1920×1080 ×3+), description
- [ ] Complete IARC age rating questionnaire
- [ ] Set privacy policy and support URLs
- [ ] Submit for certification and await review (1–5 business days)

### Winget
- [ ] Decide on Package ID (e.g., `Arnav.TwoFac`)
- [ ] Install `wingetcreate`
- [ ] Generate manifest files (version + installer + locale YAML)
- [ ] Compute SHA-256 hash of the MSI
- [ ] Validate manifest locally: `winget validate`
- [ ] Fork `microsoft/winget-pkgs` and submit PR
- [ ] Pass automated CI checks
- [ ] (Optional) Set up GitHub Actions for automated winget updates on release
