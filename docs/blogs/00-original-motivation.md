---
title: "My Next Project: Building the Open-Source, Cross-Platform Authenticator I Always Wanted"
datePublished: Sat Jun 21 2025 23:19:59 GMT+0000 (Coordinated Universal Time)
cuid: cmc6v2xf8000k02l2fd3q6x66
slug: my-next-project-building-the-open-source-cross-platform-authenticator-i-always-wanted
cover: https://cdn.hashnode.com/res/hashnode/image/upload/v1750547881820/742b92c6-38ae-4dcd-bad4-b5f156b2edd5.png
tags: authentication, opensource, authorization

---

Like many of you, I have a workflow. It’s a finely tuned set of tools and habits that lets me move between my different machines—a Mac for primary development, a Windows machine for gaming and testing—without missing a beat. For years, a key piece of that workflow was the Authy desktop app. It was simple, it was reliable, and it was *there*. Need a 2FA code on my desktop? Click, copy, paste. Done. It was a perfect little piece of utility software.

And then, the email landed. Twilio announced they were sunsetting the Authy desktop app.

Initially, the end-of-life was set for August 2024, which was disappointing but manageable. But then, in a move that felt like a slap in the face to its most loyal users, the date was abruptly pulled forward to March 19, 2024.1 The developer community's reaction was, to put it mildly, not positive. One user summed it up perfectly: "This is an excellent way to piss off thousands of developers and make sure we never touch any of your products again".1

The real salt in the wound, however, wasn't just the loss of the app. It was the realization of the digital cage we'd been living in. Authy provided no way to export your 2FA secrets.1 The only way to migrate to a new authenticator was to painstakingly go through every single one of your accounts, disable 2FA, and then re-enable it with a new app. For anyone with more than a handful of accounts, this was a nightmare.

This experience was a catalyst. It crystallized a problem that had been nagging at me for a while: our reliance on proprietary, centrally-controlled services for our most critical security functions. The moment a company decides to change its strategy, pivot, or simply cut costs, we, the users, are left stranded. This project, therefore, is more than just an attempt to build a replacement for a defunct app. It’s a philosophical stance. It's about reclaiming our digital sovereignty and building a tool based on principles of openness, user control, and data ownership. It’s about building the authenticator I, and many others, always wanted.

> NOTE: Many parts of this article is paraphrased using Google Gemini Deep Research based on my rough notes for this project.

## Surveying the Landscape: A Crowded Field with Room for One More

Before diving into a new project, it’s always wise to survey the existing landscape. I’m not the first person to have this idea, and there are some truly excellent open-source authenticator apps out there. The goal isn't to reinvent the wheel, but to see if there's a specific niche—my niche—that isn't being perfectly served.

### The Contenders: 2FAS and Ente

Two projects immediately stand out: 2FAS and Ente Auth.

**2FAS** is fantastic and has a massive following for good reason. It's fully open-source, doesn't require an account to use, has a beautiful and simple user interface, and even provides browser extensions for one-tap authentication.4 It hits many of the right notes on privacy and usability. However, it falls short of my personal "be everywhere" requirement. It lacks official desktop clients, and while there are browser extensions, it's not the same as a native application. Furthermore, there's no support for wearables like the Apple Watch, a feature some users have explicitly wished for.5

**Ente Auth** gets even closer to my ideal. It is also open-source and has been praised by the likes of Linus Tech Tips. Crucially, it offers a full suite of native clients for Android, iOS, Windows, macOS, and Linux, plus a web app.9 This is a huge win for cross-platform users. It provides end-to-end encrypted cloud backup and sync, which is a great feature for most people.

But here’s the rub. Ente's sync is tied to *their* cloud service.8 While it is end-to-end encrypted and the company has a strong privacy focus, it doesn't satisfy the power-user desire for complete and total control over the backup location. What if I want to store my encrypted secrets in my own S3 bucket? Or on my personal Google Drive? Or sync them to a local server via an`rsync` endpoint? This is where I see the gap.

My project aims to fill the niche for the user who wants the comprehensive, multi-platform presence of Ente but with the absolute data sovereignty of being able to choose their own backup provider. It's for the developer who demands a first-class, scriptable Command-Line Interface (CLI) not just as an afterthought, but as a core part of the experience.

## The Vision: A Manifesto for the Perfect Authenticator

This project is guided by a set of core principles that address the shortcomings of existing solutions and embody the spirit of user empowerment.

1. **Platform Ubiquity:** The app must be available and feel native on every platform I use: iOS, Android, watchOS, Wear OS, Windows, macOS, Linux, the Web (as a PWA and browser extension), and the command line.
    
2. **User Sovereignty:** Your secrets are yours. Period. The application will **never** require you to use a hosted cloud service. Backups will be fully under your control.
    
3. **Interoperability:** The core logic should be a self-contained, distributable library that other developers can use in their own projects.
    
4. **Openness:** Everything, from the cryptographic core to the UI, will be open-source, auditable, and community-driven.
    
5. **Intelligence:** The app should do more than just display six-digit codes. It should provide context and act as a security advisor to help users make smarter decisions.
    

### Feature Breakdown

With those principles in mind, here is the planned feature set:

* **Baseline Requirements (v1.0):**
    
    * Support for TOTP (Time-based) and HOTP (HMAC-based) one-time passwords.
        
    * Ability to add accounts by scanning a QR code or manually entering the secret string.
        
    * GUI apps for iOS, Android, watchOS, and Wear OS.
        
    * GUI apps for desktop: Windows, macOS, and Linux.
        
    * A fully-featured web app (PWA) and browser extensions for Chrome and Firefox.
        
    * A first-class, scriptable CLI for power users and automation.
        
    * Ability to create encrypted backups locally, without involving any cloud service.
        
* **Future Features (v2.0 and beyond):**
    
    * A simple, secure password manager for storing private passwords alongside 2FA secrets.
        
    * Multiple backup "providers," allowing you to "bring your own cloud." This includes options like a personal S3-compatible bucket, a Google Drive or Apple iCloud folder, or even an `rsync`\-compatible endpoint.
        

To make the vision clearer, here’s how my proposed app (let's tentatively call it **AuthZen**) stacks up against the competition:

| Feature | AuthZen (Proposed) | Authy (Legacy) | 2FAS | Ente |
| --- | --- | --- | --- | --- |
| **Platforms** |  |  |  |  |
| Desktop (Win/Mac/Linux) | ✅ | ❌ (EOL) | ❌ | ✅ |
| Browser Extension | ✅ | ❌ (EOL) | ✅ | ✅ |
| CLI | ✅ (First-Class) | ❌ | ❌ | ✅ |
| Wearable (watchOS/WearOS) | ✅ | ❌ | ❌ | ❌ |
| **Backup & Sync** |  |  |  |  |
| Local/Offline Export | ✅ | ❌ | ✅ | ✅ |
| Bring-Your-Own Cloud (S3, etc.) | ✅ (v2) | ❌ | ❌ | ❌ |
| Vendor-Managed Cloud Sync | ❌ (by design) | ✅ | ✅ | ✅ |

This table shows the clear value proposition. AuthZen is designed for the user who wants it all: every platform, complete control over their data, and a powerful CLI, all wrapped in an open-source package.

## The Architectural Blueprint: Kotlin, One Codebase to Rule Them All

So, how is it possible for a solo developer to build and maintain an application across such a vast array of platforms? The answer lies in a carefully chosen, modern tech stack that maximizes code reuse without sacrificing performance or the native feel of each platform. The cornerstone of this entire project is **Kotlin**.

### Why Kotlin Multiplatform is the Only Sane Choice

Building native apps for iOS (Swift/SwiftUI), Android (Kotlin/Compose), and Desktop (C++/Qt, C#/WPF, Electron/JS, etc.) would require three or more separate codebases. This is simply not feasible. This is where **Kotlin Multiplatform (KMP)** comes in. KMP is a technology from JetBrains that allows you to share code between different platforms while retaining full access to native APIs.12

This isn't like older cross-platform frameworks that give you a one-size-fits-none solution. With KMP, you decide what to share. For this project, the plan is to share almost everything: the business logic, the data models, the network code, and even the UI. The fact that Google now officially supports KMP for its Jetpack libraries gives this approach immense credibility and ensures its longevity.14

### The Core Engine: `lib2fa`

The heart of the application will be a shared Kotlin module, tentatively named `lib2fa`. This library will contain all the critical, non-UI logic:

* The implementation of the TOTP and HOTP algorithms.
    
* Logic for parsing `otpauth://` URIs.
    
* Encryption and decryption routines for backups.
    
* State management for the list of accounts.
    
* (In v2.0) The integration logic for the various backup providers (S3, Google Drive, etc.).
    

The beauty of KMP is how this single library can be compiled for different targets:

* **For JVM Targets (Android, Desktop):** The `lib2fa` code will be compiled into a standard Java `.jar` file, which can be easily consumed by the Android and Desktop JVM applications.12
    
* **For Native Targets (iOS, CLI):** Using **Kotlin/Native**, the *exact same* Kotlin code is compiled down to native machine code.15 For iOS and watchOS, this produces a
    
    `.xcframework` that can be imported directly into an Xcode project. For the desktop and CLI apps, it produces standalone executables or shared libraries (`.so` on Linux, `.dll` on Windows).15 This directly fulfills the goal of having a core engine that is truly interoperable.
    

### Painting the Pixels with Compose Multiplatform

For all the graphical user interfaces, I'll be using **Compose Multiplatform**. This is a declarative UI framework from JetBrains, based on Android's popular Jetpack Compose, that allows you to share your UI code across Android, iOS, desktop, and even the web.13

This means I can write the UI for a feature *once* in Kotlin, and it will run everywhere.

* On **Desktop**, Compose uses the powerful Skia 2D graphics library for rendering, giving it native performance.17
    
* On **Android**, it's the native Jetpack Compose framework.
    
* On **iOS**, it renders its UI into a native `UIViewController`, allowing for seamless integration with the rest of the iOS ecosystem.18
    
* On the **Web**, it compiles to WebAssembly (WASM), offering near-native performance in the browser.20
    

This unification of logic and UI is the project's strategic enabler. When I add the v2.0 password manager feature, I'll add the logic to `lib2fa` and the UI screens to the shared Compose module. With one push, that feature will be available consistently and simultaneously across every single platform. This is a massive advantage in development speed and ensures a cohesive user experience that many cross-platform projects lack.

### The CLI: A Purely Native Experience

The Command-Line Interface won't be a second-class citizen. It will be a dedicated Kotlin/Native application that depends directly on the native-compiled `lib2fa`. I'll use a robust library like `kotlinx-cli` to build a powerful and user-friendly interface with commands, subcommands, and flags, making it perfect for scripting and automation.22

Here’s a clear breakdown of the architecture:

| Target Platform | Core Logic (lib2fa) | UI Framework | Final Artifact(s) |
| --- | --- | --- | --- |
| Android / Wear OS | Kotlin/JVM | Jetpack Compose | `.jar -> .apk` |
| iOS / watchOS | Kotlin/Native | Compose Multiplatform | `.xcframework -> .ipa` |
| Desktop (Win, Mac, Linux) | Kotlin/JVM | Compose for Desktop (Skia) | `.exe, .dmg, .deb` |
| CLI (Win, Mac, Linux) | Kotlin/Native | None (e.g., kotlinx-cli) | `.so/.dll -> binary` |
| Web (PWA / Extension) | Kotlin/WASM | Compose for Web (Alpha) | `wasm + js` |

## The Road Ahead: Open Source, Open for Contribution

This is an ambitious project, but it's one I'm incredibly passionate about. The entire codebase, from `lib2fa` to every UI component, will be open-source under a permissive license. Trust is paramount for a security tool, and the only way to earn it is through transparency.

This is just the beginning of the journey. I'm starting this project not just for myself, but for everyone who felt the sting of the Authy shutdown and wished for a better, more open alternative. I'll be setting up a GitHub repository soon where you can follow the progress, star the project, contribute ideas, and eventually, submit code.

Let's build the tool we all deserve.

#### **Works Referenced**

1. Twilio reminds users that Authy Desktop apps die in March – not in August - The Register, accessed on June 21, 2025, [https://www.theregister.com/2024/02/15/twilio\_authy\_eol/](https://www.theregister.com/2024/02/15/twilio_authy_eol/)
    
2. Topic: Authy for Desktop End of Life (EOL) @ AskWoody, accessed on June 21, 2025, [https://www.askwoody.com/forums/topic/authy-for-desktop-end-of-life-eol/](https://www.askwoody.com/forums/topic/authy-for-desktop-end-of-life-eol/)
    
3. help.twilio.com, accessed on June 21, 2025, [https://help.twilio.com/articles/22771146070299-User-guide-End-of-Life-EOL-for-Twilio-Authy-Desktop-app](https://help.twilio.com/articles/22771146070299-User-guide-End-of-Life-EOL-for-Twilio-Authy-Desktop-app)
    
4. 2FA Authenticator (2FAS) on the App Store, accessed on June 21, 2025, [https://apps.apple.com/us/app/2fa-authenticator-2fas/id1217793794](https://apps.apple.com/us/app/2fa-authenticator-2fas/id1217793794)
    
5. 2FAS Review - PCMag, accessed on June 21, 2025, [https://www.pcmag.com/reviews/2fas](https://www.pcmag.com/reviews/2fas)
    
6. 2FAS - 2FA Authentication App Reviews (2025) - Product Hunt, accessed on June 21, 2025, [https://www.producthunt.com/products/2fas-2fa-authentication-app/reviews](https://www.producthunt.com/products/2fas-2fa-authentication-app/reviews)
    
7. Ente Auth - 2FA Authenticator – Apps on Google Play, accessed on June 21, 2025, [https://play.google.com/store/apps/details/ente\_Authenticator?id=io.ente.auth&hl=en\_AU](https://play.google.com/store/apps/details/ente_Authenticator?id=io.ente.auth&hl=en_AU)
    
8. What is Ente Auth? - WorkOS, accessed on June 21, 2025, [https://workos.com/blog/what-is-ente-auth](https://workos.com/blog/what-is-ente-auth)
    
9. Ente Auth - 2FA Authenticator - Apps on Google Play, accessed on June 21, 2025, [https://play.google.com/store/apps/details?id=io.ente.auth](https://play.google.com/store/apps/details?id=io.ente.auth)
    
10. Ente Auth - Open source 2FA authenticator, with E2EE backups, accessed on June 21, 2025, [https://ente.io/auth/](https://ente.io/auth/)
    
11. Ente Auth - 2FA Authenticator on the App Store, accessed on June 21, 2025, [https://apps.apple.com/us/app/ente-auth-2fa-authenticator/id6444121398](https://apps.apple.com/us/app/ente-auth-2fa-authenticator/id6444121398)
    
12. Kotlin Multiplatform Development: A Comprehensive Guide - Riseup Labs, accessed on June 21, 2025, [https://riseuplabs.com/kotlin-multiplatform-development-comprehensive-guide/](https://riseuplabs.com/kotlin-multiplatform-development-comprehensive-guide/)
    
13. Kotlin Multiplatform – Build Cross-Platform Apps - JetBrains, accessed on June 21, 2025, [https://www.jetbrains.com/kotlin-multiplatform/](https://www.jetbrains.com/kotlin-multiplatform/)
    
14. Kotlin Multiplatform Overview - Android Developers, accessed on June 21, 2025, [https://developer.android.com/kotlin/multiplatform](https://developer.android.com/kotlin/multiplatform)
    
15. Kotlin/Native | Kotlin Documentation, accessed on June 21, 2025, [https://kotlinlang.org/docs/native-overview.html](https://kotlinlang.org/docs/native-overview.html)
    
16. Kotlin Multiplatform | Kotlin Documentation, accessed on June 21, 2025, [https://kotlinlang.org/docs/multiplatform.html](https://kotlinlang.org/docs/multiplatform.html)
    
17. Compose Multiplatform – Beautiful UIs Everywhere - JetBrains, accessed on June 21, 2025, [https://www.jetbrains.com/compose-multiplatform/](https://www.jetbrains.com/compose-multiplatform/)
    
18. Has anyone used Compose Multiplatform? : r/FlutterDev - Reddit, accessed on June 21, 2025, [https://www.reddit.com/r/FlutterDev/comments/1afeo2r/has\_anyone\_used\_compose\_multiplatform/](https://www.reddit.com/r/FlutterDev/comments/1afeo2r/has_anyone_used_compose_multiplatform/)
    
19. Building a subscription tracker Desktop and iOS app with compose multiplatform - Setup, accessed on June 21, 2025, [https://dev.to/kuroski/building-a-subscription-tracker-desktop-and-ios-app-with-compose-multiplatform-5feg](https://dev.to/kuroski/building-a-subscription-tracker-desktop-and-ios-app-with-compose-multiplatform-5feg)
    
20. Create your Compose Multiplatform app - JetBrains, accessed on June 21, 2025, [https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-multiplatform-create-first-app.html](https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-multiplatform-create-first-app.html)
    
21. Compose Multiplatform, a modern UI framework for Kotlin that makes building performant and beautiful user interfaces easy and enjoyable. - GitHub, accessed on June 21, 2025, [https://github.com/JetBrains/compose-multiplatform](https://github.com/JetBrains/compose-multiplatform)
    
22. Kotlin/kotlinx-cli: Pure Kotlin implementation of a generic CLI parser. - GitHub, accessed on June 21, 2025, [https://github.com/Kotlin/kotlinx-cli](https://github.com/Kotlin/kotlinx-cli)