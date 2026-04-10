# Chrome Web Store: Publishing TwoFac Extension

Step-by-step guide for publishing the TwoFac browser extension to the Chrome Web Store.

## Prerequisites

- Google account (use a dedicated one for publishing)
- Extension ZIP built via: `./gradlew packageChromeExtension`
- A hosted privacy policy page (URL required during submission)

## 1. Register a Developer Account

- Go to [Chrome Web Store Developer Dashboard](https://chrome.google.com/webstore/devconsole)
- Pay the **one-time $5 USD** registration fee
- Complete your developer profile (display name, verified email)
- Docs: [Register your developer account](https://developer.chrome.com/docs/webstore/register)

## 2. Prepare the Extension Package

- Build the ZIP: `./gradlew packageChromeExtension`
- Test locally first:
  1. Open `chrome://extensions`
  2. Enable **Developer mode**
  3. Click **Load unpacked** → select the unzipped extension folder
  4. Verify popup, side panel, and background service worker all function
- Ensure `manifest.json` includes:
  - `"manifest_version": 3`
  - `"name": "TwoFac"`
  - `"description": "Two-factor authentication codes in your browser"`
  - `"permissions": ["storage", "sidePanel"]`
  - `"content_security_policy": { "extension_pages": "script-src 'self' 'wasm-unsafe-eval'; object-src 'self';" }`

## 3. Upload to Chrome Web Store

- In Developer Dashboard → **Add new item** → upload the `.zip`
- Docs: [Publish in the Chrome Web Store](https://developer.chrome.com/docs/webstore/publish)

## 4. Fill in Store Listing

In the **Store Listing** tab, provide:

| Field | Value |
|-------|-------|
| Extension name | TwoFac |
| Summary | Two-factor authentication codes in your browser |
| Description | Detailed description of features (popup, side panel, OTP generation) |
| Category | Productivity (or Developer Tools) |
| Language | English |
| Icon | 128×128 PNG |
| Screenshots | At least 1 screenshot (1280×800 or 640×400) |
| Promotional images | Small tile (440×280) recommended |

- Docs: [Prepare your listing](https://developer.chrome.com/docs/webstore/cws-dashboard-listing)

## 5. Privacy Tab

- **Single purpose description**: "Generate and display two-factor authentication (TOTP/HOTP) codes"
- **Privacy policy URL**: Link to your hosted privacy policy (required)
- **Data usage disclosures**: Declare what data is collected/stored
  - TwoFac stores OTP secrets locally via `chrome.storage` — declare this
  - If no data leaves the device, state "No data transmitted to external servers"
- Docs: [Privacy practices](https://developer.chrome.com/docs/webstore/cws-dashboard-privacy)

## 6. Permission Justifications

You **must** justify each permission in the Developer Dashboard:

| Permission | Justification |
|------------|---------------|
| `storage` | "Used to persist user's 2FA account secrets and app preferences locally via `chrome.storage.local`. No data is sent externally." |
| `sidePanel` | "Provides a persistent side panel view for quick access to 2FA codes while browsing, without blocking the current page." |

- Keep justifications specific, mentioning the exact API used and why
- Docs: [Declare permissions](https://developer.chrome.com/docs/extensions/develop/concepts/declare-permissions)

## 7. WebAssembly / `wasm-unsafe-eval` Considerations

TwoFac uses Kotlin/Wasm compiled to WebAssembly, requiring `wasm-unsafe-eval` in the CSP.

- **Why it's needed**: The `wasm-unsafe-eval` directive allows `WebAssembly.instantiate()` and `WebAssembly.compile()` calls in extension pages. Without it, Wasm modules cannot load.
- **Review impact**: This CSP directive may trigger additional scrutiny during review. Be prepared to:
  - Explain that Wasm is used for the core OTP generation logic (compiled from Kotlin)
  - Clarify that no `eval()` or dynamic code execution is used — only pre-compiled `.wasm` binaries
  - Note: `wasm-unsafe-eval` is the approved Manifest V3 approach (available since Chrome 103); `unsafe-eval` is **not** allowed in MV3
- Docs: [Content Security Policy for extensions](https://developer.chrome.com/docs/extensions/reference/manifest/content-security-policy)

## 8. Submit for Review

- Choose **publishing visibility**: Public, Unlisted, or Private
- Choose **publish timing**: Publish immediately after approval, or defer
- Click **Submit for review**
- Review typically takes **1–3 business days** (may be longer for Wasm extensions)
- If rejected: read the rejection reason, fix the issue, and resubmit
- Docs: [Review process](https://developer.chrome.com/docs/webstore/review-process)

## 9. Post-Publish

- Monitor the Developer Dashboard for review status and user feedback
- Use **deferred publishing** for future updates to control release timing
- Update the extension by uploading a new ZIP with an incremented `version`
- Docs: [Update your extension](https://developer.chrome.com/docs/webstore/update)

## Quick Reference Links

| Resource | URL |
|----------|-----|
| Developer Dashboard | https://chrome.google.com/webstore/devconsole |
| Registration docs | https://developer.chrome.com/docs/webstore/register |
| Publishing guide | https://developer.chrome.com/docs/webstore/publish |
| MV3 CSP reference | https://developer.chrome.com/docs/extensions/reference/manifest/content-security-policy |
| storage API | https://developer.chrome.com/docs/extensions/reference/api/storage |
| sidePanel API | https://developer.chrome.com/docs/extensions/reference/api/sidePanel |
| Program policies | https://developer.chrome.com/docs/webstore/program-policies |
