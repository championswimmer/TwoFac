<script setup lang="ts">
import { RouterLink } from 'vue-router'
import MainLayout from '../../layouts/MainLayout.vue'
import ComparisonTable from '../../components/ComparisonTable.vue'
import { useSEO } from '../../composables/useSEO'

useSEO({
  title: 'TwoFac vs Microsoft Authenticator — Open Source Privacy-First Alternative (2025)',
  description: 'Detailed comparison of TwoFac and Microsoft Authenticator. TwoFac is the open-source, privacy-focused, lightweight 2FA alternative with desktop, Linux, CLI, and wearable support — no Microsoft account required.',
  canonicalPath: '/compare/microsoft-authenticator',
})

const features = [
  { name: 'Open Source', twofac: true, competitor: false },
  { name: 'Android App', twofac: true, competitor: true },
  { name: 'iOS App', twofac: true, competitor: true },
  { name: 'Desktop App (macOS/Windows/Linux)', twofac: true, competitor: false },
  { name: 'CLI Tool', twofac: true, competitor: false },
  { name: 'Web App (PWA)', twofac: true, competitor: false },
  { name: 'Browser Extension', twofac: true, competitor: false },
  { name: 'Linux Support', twofac: true, competitor: false },
  { name: 'Wear OS', twofac: true, competitor: false },
  { name: 'watchOS', twofac: true, competitor: false },
  { name: 'TOTP Support', twofac: true, competitor: true },
  { name: 'HOTP Support', twofac: true, competitor: false },
  { name: 'Push Notification MFA', twofac: false, competitor: 'Yes (Microsoft only)' },
  { name: 'Passwordless Sign-In', twofac: false, competitor: 'Yes (Microsoft only)' },
  { name: 'E2E Encrypted Sync', twofac: true, competitor: false },
  { name: 'Privacy Focused', twofac: 'Yes (no telemetry)', competitor: 'No (telemetry)' },
  { name: 'Biometric Lock', twofac: true, competitor: true },
  { name: 'Lightweight', twofac: true, competitor: false },
  { name: 'No Account Required', twofac: true, competitor: 'No (Microsoft account)' },
  { name: 'Vendor Independent', twofac: true, competitor: 'No (Microsoft ecosystem)' },
]
</script>

<template>
  <MainLayout>
    <!-- Hero -->
    <section class="bg-gradient-to-br from-primary-50 to-white dark:from-secondary-900 dark:to-secondary-950 py-16 sm:py-24">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
        <h1 class="text-3xl sm:text-4xl lg:text-5xl font-bold text-secondary-900 dark:text-white">
          TwoFac vs <span class="text-primary-600 dark:text-primary-400">Microsoft Authenticator</span>
        </h1>
        <p class="mt-6 text-lg sm:text-xl text-secondary-600 dark:text-secondary-400 max-w-3xl mx-auto leading-relaxed">
          Microsoft Authenticator is deeply integrated into the Microsoft enterprise ecosystem, but it's closed-source, collects
          telemetry, only runs on mobile phones, and requires a Microsoft account for most features. TwoFac is the open-source,
          privacy-first alternative that works on 11+ platforms — including Linux, desktop, CLI, and smartwatches — with no vendor
          lock-in and no tracking.
        </p>
        <div class="mt-8 flex flex-col sm:flex-row gap-4 justify-center">
          <RouterLink
            to="/download"
            class="inline-flex items-center justify-center px-6 py-3 rounded-lg bg-primary-600 text-white font-medium hover:bg-primary-700 transition-colors shadow-sm"
          >
            Download TwoFac Free
          </RouterLink>
          <a
            href="#comparison-table"
            class="inline-flex items-center justify-center px-6 py-3 rounded-lg border border-secondary-300 dark:border-secondary-600 text-secondary-700 dark:text-secondary-300 font-medium hover:bg-secondary-50 dark:hover:bg-secondary-800 transition-colors"
          >
            See Full Comparison <i class="fa-solid fa-arrow-down ml-1" aria-hidden="true" />
          </a>
        </div>
      </div>
    </section>

    <!-- At a Glance -->
    <section class="py-12 sm:py-16">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-8 text-center">TwoFac vs Microsoft Authenticator at a Glance</h2>
        <div class="grid gap-6 sm:grid-cols-3">
          <div class="rounded-xl bg-primary-50 dark:bg-primary-900/20 border border-primary-200 dark:border-primary-800 p-6 text-center">
            <div class="text-3xl font-bold text-primary-600 dark:text-primary-400">11+</div>
            <div class="mt-2 text-sm font-medium text-secondary-700 dark:text-secondary-300">Platforms TwoFac supports</div>
            <div class="mt-1 text-xs text-secondary-500 dark:text-secondary-400">Microsoft Authenticator: mobile only</div>
          </div>
          <div class="rounded-xl bg-primary-50 dark:bg-primary-900/20 border border-primary-200 dark:border-primary-800 p-6 text-center">
            <div class="text-3xl font-bold text-primary-600 dark:text-primary-400">Zero</div>
            <div class="mt-2 text-sm font-medium text-secondary-700 dark:text-secondary-300">Telemetry & data collection</div>
            <div class="mt-1 text-xs text-secondary-500 dark:text-secondary-400">Microsoft collects usage telemetry</div>
          </div>
          <div class="rounded-xl bg-primary-50 dark:bg-primary-900/20 border border-primary-200 dark:border-primary-800 p-6 text-center">
            <div class="text-3xl font-bold text-primary-600 dark:text-primary-400">100%</div>
            <div class="mt-2 text-sm font-medium text-secondary-700 dark:text-secondary-300">Open Source</div>
            <div class="mt-1 text-xs text-secondary-500 dark:text-secondary-400">Microsoft Authenticator is proprietary</div>
          </div>
        </div>
      </div>
    </section>

    <!-- What is Microsoft Authenticator -->
    <section class="bg-secondary-50 dark:bg-secondary-900 py-12 sm:py-16">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-4">What Is Microsoft Authenticator?</h2>
        <div class="space-y-4">
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            Microsoft Authenticator is a free mobile app for iOS and Android that provides multi-factor authentication, passwordless
            sign-in, and passkey support, primarily designed for the Microsoft enterprise ecosystem (Entra ID, formerly Azure AD).
            Beyond Microsoft-specific features, it also generates standard OATH TOTP codes for third-party services like GitHub,
            AWS, and Google.
          </p>
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            The app offers push notification-based MFA for Microsoft accounts — you receive a notification and tap to approve or
            deny a sign-in. It supports passwordless sign-in via biometrics, PIN, or FIDO2 passkeys. Enterprise features include
            Conditional Access policies, GPS-based named locations, number matching to prevent accidental approvals, and admin-managed
            registration campaigns.
          </p>
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            However, Microsoft Authenticator has notable limitations. It's mobile-only with no desktop, web, or CLI app. It collects
            telemetry data. It only supports TOTP (not HOTP). Its cloud backup for iOS uses iCloud and for Android uses the device's
            cloud sync, with no end-to-end encryption. The password manager feature is being fully retired by August 2025. And it's
            deeply tied to the Microsoft ecosystem — requiring a Microsoft account for most advanced features.
          </p>
        </div>
      </div>
    </section>

    <!-- Comparison Table -->
    <section id="comparison-table" class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-12 sm:py-16">
      <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-2">Feature-by-Feature Comparison</h2>
      <p class="text-secondary-600 dark:text-secondary-400 mb-6">Microsoft Authenticator excels in enterprise Microsoft integration. TwoFac excels everywhere else.</p>
      <ComparisonTable :features="features" competitor-name="Microsoft Authenticator" />
    </section>

    <!-- Platform Availability -->
    <section class="bg-secondary-50 dark:bg-secondary-900 py-12 sm:py-16">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-4">Platform Availability: Mobile-Only vs Universal</h2>
        <div class="space-y-4">
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            Microsoft Authenticator is strictly a mobile app — Android and iOS only. There is no desktop application, no web interface,
            no CLI tool, no browser extension, and no smartwatch companion. For an app made by one of the world's largest technology
            companies, the platform coverage is surprisingly narrow.
          </p>
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            This mobile-only limitation is especially frustrating for professionals who work at a desk all day. Every time you need a
            2FA code to log into a service on your computer, you must pick up your phone, unlock it, open the app, find the right
            account, and manually type the code. For Linux users, the situation is even worse — there's no Microsoft-supported path at all.
          </p>
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            TwoFac runs natively on <strong class="text-secondary-900 dark:text-white">11+ platforms</strong>: Android, iOS, macOS,
            Windows, Linux, web (PWA), CLI, browser extensions, Wear OS, and Apple Watch. Whether you're on a Linux workstation, a
            Windows laptop, a Mac, or SSH'd into a remote server, your 2FA codes are always accessible — no phone required.
          </p>
        </div>
      </div>
    </section>

    <!-- Privacy & Telemetry -->
    <section class="py-12 sm:py-16">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-4">Privacy and Telemetry: A Fundamental Difference</h2>
        <div class="space-y-4">
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            Microsoft Authenticator collects telemetry data as part of the broader Microsoft ecosystem. While Microsoft states this
            data is used to improve the product, it means your app usage patterns, device information, and interaction data are
            transmitted to Microsoft's servers. For privacy-conscious users, this is a dealbreaker.
          </p>
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            TwoFac takes the opposite approach: <strong class="text-secondary-900 dark:text-white">zero telemetry, zero tracking, zero
            data collection</strong>. No analytics SDKs, no usage reports, no device fingerprinting. Your 2FA secrets and your usage
            patterns stay entirely private. Combined with TwoFac's open-source codebase, you can verify these claims yourself —
            something impossible with Microsoft's proprietary app.
          </p>
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            Microsoft Authenticator's cloud backup also lacks end-to-end encryption. Backups are encrypted in transit and at rest, but
            Microsoft holds the decryption keys. TwoFac uses true end-to-end encryption — your secrets are encrypted on your device
            with keys only you control.
          </p>
        </div>
      </div>
    </section>

    <!-- Enterprise vs Personal -->
    <section class="bg-secondary-50 dark:bg-secondary-900 py-12 sm:py-16">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-4">Enterprise Integration vs Independence</h2>
        <div class="space-y-4">
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            Microsoft Authenticator's biggest strength is its deep integration with Microsoft Entra ID (Azure AD). If your
            organization uses Microsoft 365, push notification MFA, Conditional Access policies, and passwordless sign-in are
            compelling features that TwoFac doesn't replicate. For enterprise Microsoft environments, Microsoft Authenticator serves
            a specific role that goes beyond standard TOTP codes.
          </p>
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            However, most individuals and many organizations use 2FA across dozens of non-Microsoft services: GitHub, AWS, Cloudflare,
            DigitalOcean, Slack, Discord, cryptocurrency exchanges, banking apps, and more. For these standard TOTP-based services,
            Microsoft Authenticator offers no advantage over TwoFac — and TwoFac offers a dramatically better experience with desktop
            apps, CLI tools, browser autofill, and cross-platform sync.
          </p>
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            <strong class="text-secondary-900 dark:text-white">You don't have to choose one.</strong> Many users keep Microsoft
            Authenticator for their work Microsoft account's push MFA while using TwoFac for all standard TOTP accounts. TwoFac is
            designed to complement — or replace — Microsoft Authenticator depending on your needs.
          </p>
        </div>
      </div>
    </section>

    <!-- Lightweight -->
    <section class="py-12 sm:py-16">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-4">Lightweight vs Feature-Bloated</h2>
        <div class="space-y-4">
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            Over the years, Microsoft has added passwords, payment cards, addresses, and other features to Microsoft Authenticator,
            turning it into a multi-purpose app. The password manager feature is being retired by August 2025, but the app still
            carries significant complexity. Users report that the app can be sluggish, that backup/sync occasionally glitches, and
            that the interface feels cluttered compared to dedicated authenticator apps.
          </p>
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            TwoFac is purpose-built for 2FA. It does one thing and does it well — generating and managing TOTP/HOTP codes across
            all your platforms. No password manager, no payment cards, no address book. This focused approach means faster startup,
            cleaner interface, smaller app size, and fewer potential failure points.
          </p>
        </div>
      </div>
    </section>

    <!-- Why Choose TwoFac -->
    <section class="bg-secondary-50 dark:bg-secondary-900 py-12 sm:py-16">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-8">Why Choose TwoFac Over Microsoft Authenticator?</h2>
        <div class="grid gap-6 sm:grid-cols-2">
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm border border-secondary-200 dark:border-secondary-700">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2"><i class="fa-solid fa-user-shield mr-2" aria-hidden="true" />Privacy First</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              Zero telemetry, zero tracking. TwoFac is open source and collects no data. Microsoft Authenticator collects telemetry
              as part of the Microsoft ecosystem.
            </p>
          </div>
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm border border-secondary-200 dark:border-secondary-700">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2"><i class="fa-brands fa-linux mr-2" aria-hidden="true" />Linux, Desktop & CLI</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              Native apps for macOS, Windows, and Linux plus a CLI tool. Microsoft Authenticator is mobile-only with no desktop or
              terminal support.
            </p>
          </div>
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm border border-secondary-200 dark:border-secondary-700">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2"><i class="fa-solid fa-code-branch mr-2" aria-hidden="true" />Open Source</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              Every line of code is on GitHub, auditable by anyone. Microsoft Authenticator is proprietary — you can't verify what
              it does with your data.
            </p>
          </div>
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm border border-secondary-200 dark:border-secondary-700">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2"><i class="fa-solid fa-feather-pointed mr-2" aria-hidden="true" />Lightweight & Focused</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              Purpose-built for 2FA only. No password manager, no payment cards, no bloat. Fast, clean, and focused on doing one
              thing exceptionally well.
            </p>
          </div>
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm border border-secondary-200 dark:border-secondary-700">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2"><i class="fa-solid fa-link mr-2" aria-hidden="true" />No Vendor Lock-In</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              No Microsoft account required. No ecosystem dependency. Export your data anytime in standard formats. TwoFac works
              with any service that supports TOTP.
            </p>
          </div>
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm border border-secondary-200 dark:border-secondary-700">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2">⌚ Wearable Support</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              View 2FA codes on your Wear OS or Apple Watch. Microsoft Authenticator offers no wearable app — you always need your
              phone.
            </p>
          </div>
        </div>
      </div>
    </section>

    <!-- FAQ -->
    <section class="py-12 sm:py-16">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-8">Frequently Asked Questions</h2>
        <div class="space-y-6">
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm border border-secondary-200 dark:border-secondary-700">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2">Can TwoFac replace Microsoft Authenticator for push MFA?</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              TwoFac generates standard TOTP codes but does not support Microsoft's proprietary push notification MFA or passwordless
              sign-in features. If your organization requires push-based MFA for Entra ID, you'll still need Microsoft Authenticator
              for that specific account. However, you can use TwoFac for all other TOTP-based services alongside it.
            </p>
          </div>
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm border border-secondary-200 dark:border-secondary-700">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2">Does Microsoft Authenticator work on Linux?</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              No. Microsoft Authenticator is only available on Android and iOS. There is no desktop app for any platform — not Windows,
              not macOS, and not Linux. TwoFac provides native desktop apps for all three operating systems plus a CLI tool.
            </p>
          </div>
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm border border-secondary-200 dark:border-secondary-700">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2">Does Microsoft Authenticator support HOTP?</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              No. Microsoft Authenticator only supports OATH TOTP (time-based codes). HOTP (counter-based codes) is not supported.
              TwoFac supports both TOTP and HOTP standards, providing broader compatibility with services that use counter-based tokens.
            </p>
          </div>
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm border border-secondary-200 dark:border-secondary-700">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2">What happened to Microsoft Authenticator's password manager?</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              Microsoft announced that password management features (autofill, saved passwords, and credit cards) are being fully
              retired by August 2025. Autofill ends in July 2025, and remaining data is not auto-transferred. TwoFac has never
              bundled a password manager — it's purpose-built for 2FA only.
            </p>
          </div>
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm border border-secondary-200 dark:border-secondary-700">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2">Is Microsoft Authenticator free?</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              The core app is free. However, advanced enterprise features like Conditional Access, risk-based authentication, and FIDO2
              passkey management require Entra ID P1/P2 licenses (part of Microsoft 365 business plans). TwoFac is completely free with
              no paid tiers.
            </p>
          </div>
        </div>
      </div>
    </section>

    <!-- CTA -->
    <section class="bg-secondary-50 dark:bg-secondary-900 py-12 sm:py-16">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
        <h2 class="text-2xl sm:text-3xl font-bold text-secondary-900 dark:text-white mb-4">Take Back Control of Your 2FA</h2>
        <p class="text-secondary-600 dark:text-secondary-400 mb-8 max-w-2xl mx-auto">
          Open source, no telemetry, no vendor lock-in, and available on every platform — including the desktop and Linux that
          Microsoft forgot. Switch to TwoFac and experience 2FA done right.
        </p>
        <div class="flex flex-col sm:flex-row gap-4 justify-center">
          <RouterLink
            to="/download"
            class="inline-flex items-center justify-center px-8 py-3 rounded-lg bg-primary-600 text-white font-medium hover:bg-primary-700 transition-colors shadow-sm"
          >
            Download TwoFac Free
          </RouterLink>
          <RouterLink
            to="/features"
            class="inline-flex items-center justify-center px-8 py-3 rounded-lg border border-secondary-300 dark:border-secondary-600 text-secondary-700 dark:text-secondary-300 font-medium hover:bg-secondary-50 dark:hover:bg-secondary-800 transition-colors"
          >
            Explore All Features <i class="fa-solid fa-arrow-right-long ml-1" aria-hidden="true" />
          </RouterLink>
        </div>
      </div>
    </section>
  </MainLayout>
</template>
