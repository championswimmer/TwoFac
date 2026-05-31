# Publishing TwoFac to Google Play Store

> Step-by-step guide for the first release of **TwoFac** (`tech.arnav.twofac.app`)
> on Google Play. Written for a human developer — links to official docs instead
> of reproducing them.

---

## App Snapshot

| Field | Value |
|---|---|
| App Name | TwoFac |
| Application ID | `tech.arnav.twofac.app` |
| Version Name | 1.6.0 |
| Version Code | 260401060 |
| Min SDK | 30 (Android 11) |
| Target / Compile SDK | 36 (Android 16) |
| Category | Tools / Utilities |
| Build type | AAB with R8 (minifyEnabled + shrinkResources) |
| Companion | Wear OS (`tech.arnav.twofac.app` — watchApp module) |

---

## 1 — Google Play Developer Account

- **One-time $25 USD fee.** Register at <https://play.google.com/console/signup>
- You'll need a Google account, legal name, address, and a payment method.
- For organization accounts Google now requires **identity verification** (government ID + proof of address); plan for 2-5 business days.
  - 📖 [Account registration docs](https://support.google.com/googleplay/android-developer/answer/6112435)

---

## 2 — Create the App in Play Console

1. **Play Console → All apps → Create app**
2. Fill in:
   - App name: `TwoFac`
   - Default language: English (US)
   - App or game: **App**
   - Free or paid: **Free**
   - Declarations: confirm Developer Program Policies + US export laws
3. 📖 [Create & set up your app](https://support.google.com/googleplay/android-developer/answer/9859152)

---

## 3 — App Signing (Play App Signing)

Play App Signing is **mandatory** for new apps.

- When you upload your first AAB, Google generates a new **app signing key** it manages.
- You keep your **upload key** (the one in your local keystore) to sign future uploads.
- **Back up your upload keystore** — if lost, you can request a key reset but it's painful.
- If you already have a release keystore, you can **export and upload** the existing signing key instead.
- 📖 [Play App Signing](https://developer.android.com/studio/publish/app-signing#app-signing-google-play)
- 📖 [Manage your app signing key](https://support.google.com/googleplay/android-developer/answer/9842756)

---

## 4 — Store Listing

Fill in under **Grow → Store presence → Main store listing**:

| Asset | Requirement |
|---|---|
| App icon | 512 × 512 px, PNG/JPEG |
| Feature graphic | 1024 × 500 px |
| Phone screenshots | Min 2, up to 8 (16:9 or 9:16) |
| Tablet screenshots | Recommended (7″ + 10″) |
| Wear OS screenshots | At least 1 (circular crop preview) |
| Short description | ≤ 80 chars |
| Full description | ≤ 4000 chars |
| Video (optional) | YouTube URL, 30 s–2 min |

**Also required:**
- Contact email (shown publicly)
- Privacy policy URL (mandatory for apps requesting dangerous permissions like CAMERA)

📖 [Prepare your store listing](https://support.google.com/googleplay/android-developer/answer/9859455)

---

## 5 — App Content (Setup → App content)

Complete **all** of these before you can publish:

### 5a — Content Rating (IARC)

- Fill out the questionnaire (~5 min). For a utility/2FA app with no violence, gambling, or user-generated content the result is typically **PEGI 3 / ESRB Everyone**.
- 📖 [Content ratings](https://support.google.com/googleplay/android-developer/answer/188189)

### 5b — Target Audience

- Select **18+** (or at minimum 13+). A security/2FA app is not designed for children.
- 📖 [Target audience & content](https://support.google.com/googleplay/android-developer/answer/9285070)

### 5c — Data Safety Form

This is critical for a 2FA app. Declare the following:

| Question | Answer for TwoFac |
|---|---|
| Does your app collect or share user data? | **Yes** (camera access) |
| **Camera** | Collected: Yes — for QR code scanning (app functionality). Not shared. Not transferred off device. Processing is ephemeral. |
| **TOTP secrets / tokens** | Stored locally only. Not collected or transmitted. Declare under "App activity" → "Other app activities" if prompted. |
| Data encrypted in transit? | N/A (no network transmission of secrets) or Yes if crash reporting is included |
| Can users request data deletion? | Yes (user can delete accounts in-app) |
| Follows Google Play Families policy? | No (not a children's app) |

- 📖 [Data safety form](https://support.google.com/googleplay/android-developer/answer/10787469)
- 📖 [Data types reference](https://support.google.com/googleplay/android-developer/answer/10787469#types)

### 5d — Government Apps, Financial Features, Health

- Mark as **not applicable** unless the app handles financial login credentials for regulated entities.

### 5e — Ads Declaration

- Declare **No ads** (assuming TwoFac has no ads).

---

## 6 — Build & Upload the AAB

```bash
# From project root
./gradlew androidApp:bundleRelease
```

- Output: `androidApp/build/outputs/bundle/release/androidApp-release.aab`
- This AAB is already R8-optimized (minifyEnabled + shrinkResources are on).
- Make sure the signing config uses your **upload keystore**.

**Upload path:** Release → Production → Create new release → Upload AAB

- 📖 [Build an AAB](https://developer.android.com/build/building-cmdline#build_bundle)
- 📖 [Upload your app bundle](https://support.google.com/googleplay/android-developer/answer/9859348)

---

## 7 — Wear OS Companion App

TwoFac's Wear OS companion shares the same application ID (`tech.arnav.twofac.app`).

**How it works with Google Play:**
- The Wear OS APK/AAB is bundled into the **same listing** as the phone app when using the same package name.
- Google Play delivers the appropriate variant to phone vs. watch automatically via the AAB multi-device support.
- Ensure `watchApp/build.gradle.kts` has the correct `<uses-feature>` for `android.hardware.type.watch`.
- Add **Wear OS screenshots** in your store listing (circular format, ≥ 384 × 384 px).
- Wear OS apps must target **API 30+** (yours targets 36 — ✅).

📖 [Distribute Wear OS apps](https://developer.android.com/training/wearables/apps/creating#distribute)
📖 [Multi-device AAB](https://developer.android.com/guide/app-bundle)

---

## 8 — Target API Level Requirements (2025–2026)

| Deadline | Requirement |
|---|---|
| Aug 2025 (expected) | New apps & updates must target **API 35** (Android 15) |
| ~Aug 2026 | Expected to require **API 36** (Android 16) |
| Existing apps | Must update within ~12 months of new requirement or risk reduced visibility / removal |

- TwoFac currently targets **API 36** — you are ahead of requirements. ✅
- 📖 [Target API level requirements](https://developer.android.com/google/play/requirements/target-sdk)

---

## 9 — Considerations for a 2FA / Security App

- **CAMERA permission**: declared for QR code scanning. Google may flag this during review — have a clear permission rationale in the Data Safety form and in-app (runtime permission dialog).
- **No INTERNET permission?** If the app truly works offline, highlight this as a privacy feature in the description. If you do include INTERNET (e.g., for cloud backup or Google Drive sync), declare it in Data Safety.
- **Biometric / credential APIs**: using `BiometricPrompt` or `KeyguardManager` does not require additional Play Console declarations.
- **ProGuard / R8 mapping files**: upload the `mapping.txt` alongside each release so crash reports in Play Console are deobfuscated.
  - 📖 [Deobfuscate crash reports](https://support.google.com/googleplay/android-developer/answer/9848633)

---

## 10 — Pre-Launch Report & Testing Tracks

Before going to production, consider:

1. **Internal testing** (up to 100 testers, instant approval — no review wait)
2. **Closed testing** (invite-only, reviewed by Google)
3. **Open testing** (public beta, reviewed by Google)
4. **Pre-launch report**: Google automatically runs your app on Firebase Test Lab devices — check for crashes, accessibility issues, security vulnerabilities.

📖 [Testing tracks overview](https://support.google.com/googleplay/android-developer/answer/9845334)

---

## 11 — Submit for Review & Rollout

1. Ensure **all** App Content sections show ✅ in the dashboard.
2. Go to **Release → Production → Create new release**.
3. Upload AAB + release notes.
4. Choose rollout percentage:
   - **Recommended for first release**: 100% (staged rollout is more useful for updates).
   - For updates: start at 5% → 25% → 50% → 100%.
5. **Review timeline**: typically 1–3 days for new apps (can be up to 7 days for first submission).
6. You'll get an email when approved or if changes are required.

📖 [Prepare & roll out a release](https://support.google.com/googleplay/android-developer/answer/9859348)

---

## Quick Checklist

- [ ] Google Play Developer account registered & verified
- [ ] Upload keystore created and backed up securely
- [ ] App created in Play Console with correct package name
- [ ] Play App Signing enrolled
- [ ] Store listing complete (icon, screenshots incl. Wear OS, descriptions)
- [ ] Privacy policy URL live and linked
- [ ] Content rating questionnaire completed
- [ ] Target audience set (not children)
- [ ] Data safety form completed (camera, local storage)
- [ ] Ads declaration (no ads)
- [ ] AAB built and uploaded (`androidApp:bundleRelease`)
- [ ] Wear OS screenshots added
- [ ] ProGuard mapping.txt uploaded
- [ ] Pre-launch report reviewed (no critical issues)
- [ ] Release notes written
- [ ] Submitted for review

---

*Last updated: July 2025. Verify all links and requirements against the current
[Google Play Console](https://play.google.com/console) before publishing.*
