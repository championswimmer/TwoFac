# Publishing TwoFac to the Apple App Store

> Step-by-step guide for a first-time App Store submission.
> Last updated: 2025-07

---

## App Identity

| Field | Value |
|---|---|
| App Name | **TwoFac** |
| Bundle ID (iOS) | `tech.arnav.twofac.app` |
| Bundle ID (watchOS) | `tech.arnav.twofac.app.watchkitapp` |
| Team ID | `9UV5DX2Q64` |
| Version | `1.6.0` |
| Min iOS | 17.6 |
| Category | Utilities |
| SKU (suggestion) | `twofac-ios` |

---

## 1 · Apple Developer Program

- [ ] Enroll at <https://developer.apple.com/programs/enroll/>
- [ ] Cost: **$99 USD/year** (individual or organization)
- [ ] Organization enrollment requires a D-U-N-S number — allow ~2 weeks
- [ ] Accept the latest Developer Program License Agreement

📖 [Enrollment overview](https://developer.apple.com/programs/)

---

## 2 · Register Bundle IDs

In **Certificates, Identifiers & Profiles** → **Identifiers**:

- [ ] Register `tech.arnav.twofac.app` (iOS App)
  - Enable capabilities: Push Notifications (if used), Associated Domains, etc.
- [ ] Register `tech.arnav.twofac.app.watchkitapp` (watchOS companion)
  - Must use the iOS app Bundle ID as prefix

📖 [Register an App ID](https://developer.apple.com/help/account/manage-identifiers/register-an-app-id/)

---

## 3 · Certificates & Provisioning Profiles

### Option A — Automatic Signing (recommended)

- [ ] In Xcode → Signing & Capabilities → check **"Automatically manage signing"**
- [ ] Select team `9UV5DX2Q64`
- [ ] Xcode creates distribution certificate + profiles automatically on archive

### Option B — Manual Signing

- [ ] Create a **Distribution Certificate** (Apple Distribution) via Keychain CSR upload
- [ ] Create an **App Store provisioning profile** for each Bundle ID
- [ ] Download & install both in Xcode

📖 [Create certificates](https://developer.apple.com/help/account/create-certificates/create-a-certificate-signing-request)
📖 [Create provisioning profiles](https://developer.apple.com/help/account/manage-profiles/create-an-app-store-provisioning-profile)

---

## 4 · Create App Record in App Store Connect

Go to <https://appstoreconnect.apple.com> → **My Apps** → **＋** → **New App**:

| Field | What to enter |
|---|---|
| Platforms | ☑ iOS, ☑ watchOS |
| Name | `TwoFac` (max 30 chars) |
| Primary Language | English (U.S.) |
| Bundle ID | `tech.arnav.twofac.app` |
| SKU | `twofac-ios` (internal, immutable) |

Then configure:

- [ ] **Category**: Primary = `Utilities`, Secondary = optional
- [ ] **Pricing**: Free (or set price tier)
- [ ] **Availability**: Select countries/regions

📖 [Add a new app](https://developer.apple.com/help/app-store-connect/create-an-app-record/add-a-new-app)

---

## 5 · Version Metadata

In **App Store** → **App Information** and the version page:

| Field | Limit | Notes |
|---|---|---|
| Subtitle | 30 chars | e.g. "Two-Factor Authenticator" |
| Description | 4,000 chars | What the app does; no HTML |
| Keywords | 100 chars | Comma-separated: `2fa,totp,authenticator,otp,security` |
| What's New | 4,000 chars | Release notes for this version |
| Support URL | required | Link to your support page |
| Privacy Policy URL | **required** | Must be publicly accessible |
| Marketing URL | optional | |

📖 [Required app information](https://developer.apple.com/help/app-store-connect/manage-app-information/required-publishable-and-editable-properties)

---

## 6 · Age Rating

Complete the questionnaire in App Store Connect:

- [ ] Answer **"None"** for all violence/gambling/etc. categories
- [ ] Expected result: **4+** rating (standard for utility apps)

📖 [Age rating questionnaire](https://developer.apple.com/help/app-store-connect/manage-app-information/set-an-age-rating)

---

## 7 · Screenshots & App Preview

### Required iPhone screenshots

| Display Size | Resolution (portrait) | Minimum |
|---|---|---|
| **6.9"** (iPhone 16 Pro Max) — **mandatory** | 1290 × 2796 | 1 (up to 10) |
| **6.7"** (iPhone 15 Pro Max) | 1290 × 2796 | optional (auto-scales from 6.9") |
| **6.5"** (iPhone XS Max) | 1242 × 2688 | optional |
| **5.5"** (iPhone 8 Plus) | 1242 × 2208 | optional |

### iPad (if supporting)
| Display Size | Resolution (portrait) |
|---|---|
| 13" iPad Pro — **mandatory if iPad supported** | 2048 × 2732 |

### Apple Watch
- Required if shipping watchOS companion
- Verify sizes in App Store Connect for current watch models

### App Icon
- [ ] Provide **1024 × 1024 px** PNG (no alpha/transparency)
- [ ] Already embedded in the Xcode asset catalog

### Tips
- Use actual app screenshots (not pure marketing graphics) to avoid rejection
- Format: PNG or JPEG, RGB, no transparency

📖 [Screenshot specifications](https://developer.apple.com/help/app-store-connect/reference/screenshot-specifications)

---

## 8 · Privacy Declarations

### Info.plist usage descriptions (already in Xcode project)

| Key | Value (example) |
|---|---|
| `NSCameraUsageDescription` | "TwoFac uses the camera to scan QR codes for adding accounts" |
| `NSFaceIDUsageDescription` | "TwoFac uses Face ID to protect your 2FA codes" |

### App Privacy "Nutrition Labels" (App Store Connect)

For a local-only 2FA app with no analytics/tracking:

- [ ] **Data Used to Track You** → **None**
- [ ] **Data Linked to You** → **None** (unless you collect accounts/analytics)
- [ ] **Data Not Linked to You** → **None** (or minimal usage data if crash reporting is included)
- [ ] **Data Not Collected** → select this for most categories

If using any analytics SDK (e.g. Firebase Crashlytics), declare accordingly.

**No App Tracking Transparency (ATT) prompt is needed** — the app does not use IDFA or cross-app tracking.

📖 [App privacy details](https://developer.apple.com/app-store/app-privacy-details/)
📖 [Privacy guidelines](https://developer.apple.com/app-store/review/guidelines/#privacy)

---

## 9 · Export Compliance (Encryption)

2FA apps use cryptographic algorithms (TOTP/HOTP with HMAC-SHA). You must declare this:

- [ ] When asked "Does your app use encryption?" → **Yes**
- [ ] Select: "Available on the iOS/macOS App Store only in the U.S. and Canada" **or** file for an encryption exemption
- [ ] Most TOTP apps qualify for the **exempt** classification (authentication, not data encryption)
- [ ] If exempt, set the `ITSEncryptionExportComplianceCode` key in Info.plist or answer in App Store Connect

📖 [Export compliance overview](https://developer.apple.com/help/app-store-connect/manage-builds/manage-export-compliance-for-your-app)
📖 [Determine export requirements](https://developer.apple.com/documentation/security/complying-with-encryption-export-regulations)

---

## 10 · App Review — Things to Watch For

Common rejection reasons relevant to TwoFac:

| Guideline | Watch out for |
|---|---|
| **2.1 Performance** | App must not crash; test on real devices (not just simulator) |
| **2.3 Accurate Metadata** | Description/screenshots must match actual functionality |
| **4.0 Design** | Must follow HIG; no web-view-only apps (Compose Multiplatform is fine) |
| **5.1 Privacy** | All permission prompts must have clear purpose strings |
| **5.1.1 Data Collection** | Privacy nutrition labels must be accurate |
| **2.5.1 APIs** | Only use public APIs |

### Specific to this project
- The app is built with **Kotlin Multiplatform + Compose Multiplatform** with a Swift/Xcode wrapper — this is fully allowed
- Ensure the watchOS companion works independently (reviewers test it)
- Provide a **demo video or notes** in the review notes field explaining how to test 2FA functionality (reviewers may not have TOTP seeds handy)

📖 [App Review Guidelines](https://developer.apple.com/app-store/review/guidelines/)

---

## 11 · Build, Archive & Upload

```bash
# 1. Open the Xcode workspace
open iosApp/iosApp.xcodeproj

# 2. Select "Any iOS Device (arm64)" as destination
# 3. Product → Archive
# 4. In Organizer: Distribute App → App Store Connect → Upload
```

Or via command line:
```bash
xcodebuild -project iosApp/iosApp.xcodeproj \
  -scheme "iosApp" \
  -configuration Release \
  -archivePath build/TwoFac.xcarchive \
  archive

xcodebuild -exportArchive \
  -archivePath build/TwoFac.xcarchive \
  -exportOptionsPlist ExportOptions.plist \
  -exportPath build/export
```

Then upload with **Transporter** (Mac App Store) or `xcrun altool`.

📖 [Distribute an app](https://developer.apple.com/help/xcode/distributing-your-app-for-beta-testing-and-releases)

---

## 12 · Submit for Review

In App Store Connect → your app version page:

- [ ] Select the uploaded build
- [ ] Fill in **Review Notes** (e.g. "No login required. Scan any TOTP QR code to test.")
- [ ] Choose release option: **Manual** or **Automatic after approval**
- [ ] Click **"Add for Review"** → **"Submit to App Review"**

⏱ Review typically takes **24–48 hours** (can be longer for first submission)

📖 [Submit for review](https://developer.apple.com/help/app-store-connect/manage-submissions-to-app-review/submit-for-review)

---

## Quick Checklist

- [ ] Apple Developer Program active & agreement accepted
- [ ] Bundle IDs registered (`tech.arnav.twofac.app` + watchOS)
- [ ] Signing configured (auto or manual)
- [ ] App record created in App Store Connect
- [ ] Description, keywords, category, age rating filled in
- [ ] Privacy policy URL live and linked
- [ ] Screenshots uploaded (6.9" iPhone mandatory + Apple Watch)
- [ ] App icon 1024×1024 in asset catalog
- [ ] Privacy nutrition labels completed
- [ ] Export compliance answered
- [ ] `NSCameraUsageDescription` and `NSFaceIDUsageDescription` in Info.plist
- [ ] Build archived, uploaded, and selected
- [ ] Review notes added
- [ ] Submitted for review ✅
