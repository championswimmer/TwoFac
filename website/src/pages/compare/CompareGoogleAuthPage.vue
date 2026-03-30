<script setup lang="ts">
import { RouterLink } from 'vue-router'
import MainLayout from '../../layouts/MainLayout.vue'
import ComparisonTable from '../../components/ComparisonTable.vue'
import { useSEO } from '../../composables/useSEO'

useSEO({
  title: 'TwoFac vs Google Authenticator — Open Source 2FA Alternative (2025)',
  description: 'Detailed comparison of TwoFac and Google Authenticator. Discover why TwoFac is the open-source, cross-platform 2FA alternative with E2E encrypted sync, desktop apps, CLI tools, browser extensions, and wearable support.',
  canonicalPath: '/compare/google-authenticator',
})

const features = [
  { name: 'Open Source', twofac: true, competitor: false },
  { name: 'Android App', twofac: true, competitor: true },
  { name: 'iOS App', twofac: true, competitor: true },
  { name: 'Desktop App (macOS/Windows/Linux)', twofac: true, competitor: false },
  { name: 'CLI Tool', twofac: true, competitor: false },
  { name: 'Web App (PWA)', twofac: true, competitor: false },
  { name: 'Browser Extension', twofac: true, competitor: false },
  { name: 'Wear OS', twofac: true, competitor: false },
  { name: 'watchOS', twofac: true, competitor: false },
  { name: 'TOTP Support', twofac: true, competitor: true },
  { name: 'HOTP Support', twofac: true, competitor: true },
  { name: 'E2E Encrypted Sync', twofac: true, competitor: false },
  { name: 'Encrypted Backups', twofac: true, competitor: false },
  { name: 'Biometric Lock', twofac: true, competitor: false },
  { name: 'Import/Export', twofac: 'Yes (full)', competitor: 'Limited' },
  { name: 'No Account Required', twofac: true, competitor: 'No (Google account for sync)' },
  { name: 'Offline Mode', twofac: true, competitor: true },
  { name: 'Kotlin Multiplatform (Native)', twofac: true, competitor: false },
]
</script>

<template>
  <MainLayout>
    <!-- Hero -->
    <section class="border-b border-secondary-200 bg-secondary-100 dark:border-secondary-800 dark:bg-secondary-900 py-16 sm:py-24">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
        <h1 class="text-3xl sm:text-4xl lg:text-5xl font-bold text-secondary-900 dark:text-white">
          TwoFac vs <span class="text-primary-600 dark:text-primary-400">Google Authenticator</span>
        </h1>
        <p class="mt-6 text-lg sm:text-xl text-secondary-600 dark:text-secondary-400 max-w-3xl mx-auto leading-relaxed">
          Google Authenticator is the world's most widely used two-factor authentication app, but it remains closed-source,
          limited to mobile phones, and lacks end-to-end encryption for cloud sync. TwoFac is the open-source 2FA alternative
          that works on 11+ platforms — including desktop, CLI, browser extensions, and smartwatches — with full end-to-end
          encrypted backups and sync.
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
        <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-8 text-center">TwoFac vs Google Authenticator at a Glance</h2>
        <div class="grid gap-6 sm:grid-cols-3">
          <div class="rounded-xl bg-primary-50 dark:bg-primary-900/20 border border-primary-200 dark:border-primary-800 p-6 text-center">
            <div class="text-3xl font-bold text-primary-600 dark:text-primary-400">11+</div>
            <div class="mt-2 text-sm font-medium text-secondary-700 dark:text-secondary-300">Platforms TwoFac supports</div>
            <div class="mt-1 text-xs text-secondary-500 dark:text-secondary-400">vs 2 for Google Authenticator</div>
          </div>
          <div class="rounded-xl bg-primary-50 dark:bg-primary-900/20 border border-primary-200 dark:border-primary-800 p-6 text-center">
            <div class="text-3xl font-bold text-primary-600 dark:text-primary-400">100%</div>
            <div class="mt-2 text-sm font-medium text-secondary-700 dark:text-secondary-300">Open Source</div>
            <div class="mt-1 text-xs text-secondary-500 dark:text-secondary-400">Google Authenticator is proprietary</div>
          </div>
          <div class="rounded-xl bg-primary-50 dark:bg-primary-900/20 border border-primary-200 dark:border-primary-800 p-6 text-center">
            <div class="text-3xl font-bold text-primary-600 dark:text-primary-400">E2E</div>
            <div class="mt-2 text-sm font-medium text-secondary-700 dark:text-secondary-300">Encrypted Sync</div>
            <div class="mt-1 text-xs text-secondary-500 dark:text-secondary-400">Google's cloud sync lacks E2E encryption</div>
          </div>
        </div>
      </div>
    </section>

    <!-- What is Google Authenticator -->
    <section class="bg-secondary-50 dark:bg-secondary-900 py-12 sm:py-16">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-4">What Is Google Authenticator?</h2>
        <div class="prose prose-secondary dark:prose-invert max-w-none">
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            Google Authenticator is a free mobile application developed by Google that generates time-based one-time passwords (TOTP)
            and HMAC-based one-time passwords (HOTP) for two-factor authentication. Originally launched in 2010, it quickly became
            the de facto standard for 2FA thanks to its simplicity and Google's brand recognition.
          </p>
          <p class="mt-4 text-secondary-600 dark:text-secondary-400 leading-relaxed">
            The app is available on Android and iOS devices. In 2023, Google added cloud sync functionality that allows users to back up
            their 2FA secrets to their Google account. However, this sync feature drew criticism from security researchers because it
            does not use end-to-end encryption — meaning Google could theoretically access your 2FA secrets. The open-source Android
            codebase was archived in 2021 at version 2.21, and all subsequent updates have been proprietary.
          </p>
          <p class="mt-4 text-secondary-600 dark:text-secondary-400 leading-relaxed">
            While Google Authenticator remains a solid choice for basic mobile 2FA, its limitations become apparent for users who need
            desktop access, developer-friendly tools, or privacy-first encryption. That's where TwoFac steps in.
          </p>
        </div>
      </div>
    </section>

    <!-- Comparison Table -->
    <section id="comparison-table" class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-12 sm:py-16">
      <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-2">Feature-by-Feature Comparison</h2>
      <p class="text-secondary-600 dark:text-secondary-400 mb-6">A detailed breakdown of every feature that matters for your 2FA workflow.</p>
      <ComparisonTable :features="features" competitor-name="Google Authenticator" />
    </section>

    <!-- Platform Availability -->
    <section class="bg-secondary-50 dark:bg-secondary-900 py-12 sm:py-16">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-4">Platform Availability: Mobile-Only vs Everywhere</h2>
        <div class="space-y-4">
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            One of the biggest limitations of Google Authenticator is that it only runs on Android and iOS. There is no official desktop
            application, no web interface, no command-line tool, and no browser extension. If you're working at your computer and need a
            2FA code, you must pick up your phone every single time.
          </p>
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            TwoFac takes a fundamentally different approach. Built with Kotlin Multiplatform, TwoFac delivers <strong class="text-secondary-900 dark:text-white">true native applications</strong>
            on every platform: Android, iOS, macOS, Windows, Linux, a progressive web app (PWA), a dedicated CLI tool, a browser extension
            for Chrome and Firefox, and companion apps for Wear OS and Apple Watch. That's 11+ platforms from a single shared codebase,
            each compiled to native code for optimal performance.
          </p>
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            For developers and system administrators, TwoFac's CLI app is a game-changer. Generate 2FA codes directly in your terminal,
            pipe them into scripts, or integrate them into SSH workflows — all without leaving the command line. Google Authenticator
            offers nothing comparable.
          </p>
        </div>
      </div>
    </section>

    <!-- Security & Privacy -->
    <section class="py-12 sm:py-16">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-4">Security and Privacy Compared</h2>
        <div class="space-y-4">
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            Both TwoFac and Google Authenticator generate TOTP and HOTP codes offline using the standard OATH algorithms (RFC 6238 and RFC 4226).
            The core code-generation mechanism is equally secure in both apps. The differences lie in how each app handles your secrets
            beyond code generation.
          </p>
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            <strong class="text-secondary-900 dark:text-white">Google Authenticator's cloud sync is not end-to-end encrypted.</strong>
            When you enable Google account sync, your 2FA secrets are transmitted to Google's servers. While they are encrypted in transit
            and at rest using Google's infrastructure encryption, Google holds the decryption keys. This means your secrets could be
            exposed in the event of a data breach, government request, or internal compromise.
          </p>
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            <strong class="text-secondary-900 dark:text-white">TwoFac uses end-to-end encryption for all synced data.</strong>
            Your 2FA secrets are encrypted on your device before they ever leave it, using keys that only you control. Even if TwoFac's
            servers were compromised, your secrets would remain unreadable. TwoFac also supports biometric locking (fingerprint and
            face recognition) to prevent unauthorized access to the app itself.
          </p>
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            Additionally, TwoFac is <strong class="text-secondary-900 dark:text-white">fully open source</strong> — anyone can audit the
            code to verify that secrets are handled securely. Google Authenticator's codebase has been proprietary since 2021, so
            independent security verification is not possible.
          </p>
        </div>
      </div>
    </section>

    <!-- Backup & Sync -->
    <section class="bg-secondary-50 dark:bg-secondary-900 py-12 sm:py-16">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-4">Backup, Sync, and Migration</h2>
        <div class="space-y-4">
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            For years, Google Authenticator had no backup functionality at all — losing your phone meant losing all your 2FA secrets.
            The 2023 cloud sync update improved this situation, but it introduced new concerns. The sync requires a Google account and
            doesn't offer end-to-end encryption. Export is limited to QR code transfers between devices, with no file-based export option.
          </p>
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            TwoFac provides comprehensive backup and sync options. Encrypted backups can be created and restored at any time.
            Cross-device sync is end-to-end encrypted and works across all platforms — your codes stay in sync between your phone,
            desktop, and watch automatically. No Google account or any third-party account is required.
          </p>
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            <strong class="text-secondary-900 dark:text-white">Migrating from Google Authenticator to TwoFac is straightforward.</strong>
            You can import your existing accounts by scanning a QR code exported from Google Authenticator or by using TwoFac's file
            import feature. The entire migration process takes just a few minutes, and you won't need to reconfigure 2FA on any of your services.
          </p>
        </div>
      </div>
    </section>

    <!-- Open Source -->
    <section class="py-12 sm:py-16">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-4">Open Source vs Closed Source</h2>
        <div class="space-y-4">
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            Google originally released the Google Authenticator source code under the Apache 2.0 license. However, the open-source
            Android repository was archived in April 2021 at version 2.21, and all subsequent development has been proprietary. The
            current Google Authenticator app on the Play Store and App Store is closed-source software.
          </p>
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            TwoFac is and always will be <strong class="text-secondary-900 dark:text-white">fully open source under the MIT License</strong>.
            Every line of code — across all platforms, from the shared core library to each native frontend — is publicly available on
            GitHub. This transparency means the community can verify security claims, contribute improvements, and fork the project if
            needed. For an app that handles your most sensitive authentication secrets, open source isn't just a nice-to-have — it's a
            fundamental trust requirement.
          </p>
        </div>
      </div>
    </section>

    <!-- Pricing -->
    <section class="bg-secondary-50 dark:bg-secondary-900 py-12 sm:py-16">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-4">Pricing: Both Free, But Different</h2>
        <div class="space-y-4">
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            Both TwoFac and Google Authenticator are free to use with no subscription fees or premium tiers. However, Google
            Authenticator's cloud sync requires a Google account, and Google monetizes your data through its advertising ecosystem.
            While Google doesn't directly monetize your 2FA secrets, using a Google account means accepting Google's broader data
            collection practices.
          </p>
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            TwoFac is completely free with no account requirement for local use. All features — including desktop apps, CLI tools,
            browser extensions, and wearable apps — are available at no cost. There are no premium tiers, no ads, and no data collection.
          </p>
        </div>
      </div>
    </section>

    <!-- Why Choose TwoFac -->
    <section class="py-12 sm:py-16">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-8">Why Choose TwoFac Over Google Authenticator?</h2>
        <div class="grid gap-6 sm:grid-cols-2">
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm border border-secondary-200 dark:border-secondary-700">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2"><i class="fa-solid fa-code-branch mr-2" aria-hidden="true" />Fully Open Source</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              Every line of TwoFac's code is publicly auditable on GitHub. Google Authenticator went proprietary in 2021 — you can't
              verify what it does with your secrets.
            </p>
          </div>
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm border border-secondary-200 dark:border-secondary-700">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2"><i class="fa-solid fa-globe mr-2" aria-hidden="true" />11+ Platforms</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              Android, iOS, macOS, Windows, Linux, Web, CLI, browser extension, Wear OS, and watchOS. Google Authenticator only
              supports Android and iOS.
            </p>
          </div>
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm border border-secondary-200 dark:border-secondary-700">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2"><i class="fa-solid fa-shield-halved mr-2" aria-hidden="true" />E2E Encrypted Sync</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              Your 2FA secrets are encrypted on your device before syncing. Google Authenticator's cloud sync does not use end-to-end
              encryption — Google holds the decryption keys.
            </p>
          </div>
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm border border-secondary-200 dark:border-secondary-700">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2"><i class="fa-solid fa-terminal mr-2" aria-hidden="true" />Developer-Friendly CLI</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              Generate 2FA codes from your terminal. Integrate with scripts, SSH workflows, and CI/CD pipelines. Perfect for developers
              and sysadmins.
            </p>
          </div>
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm border border-secondary-200 dark:border-secondary-700">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2">⌚ Wearable Support</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              View 2FA codes on your Wear OS or Apple Watch without reaching for your phone. Google Authenticator doesn't offer any
              smartwatch companion.
            </p>
          </div>
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm border border-secondary-200 dark:border-secondary-700">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2"><i class="fa-solid fa-box-open mr-2" aria-hidden="true" />Easy Migration</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              Import your existing Google Authenticator accounts via QR scan or file import. The switch takes minutes and requires no
              reconfiguration of your online services.
            </p>
          </div>
        </div>
      </div>
    </section>

    <!-- FAQ -->
    <section class="bg-secondary-50 dark:bg-secondary-900 py-12 sm:py-16">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-8">Frequently Asked Questions</h2>
        <div class="space-y-6">
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2">Is TwoFac compatible with services that recommend Google Authenticator?</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              Yes. TwoFac supports the same TOTP and HOTP standards (RFC 6238 and RFC 4226) used by Google Authenticator. Any service
              that works with Google Authenticator — including GitHub, AWS, Dropbox, Gmail, and thousands of others — works with TwoFac.
            </p>
          </div>
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2">Can I import my Google Authenticator accounts into TwoFac?</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              Yes. You can export your accounts from Google Authenticator by generating a QR code transfer, then scan that QR code
              in TwoFac. Alternatively, you can use TwoFac's file import feature. The process takes just a few minutes.
            </p>
          </div>
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2">Is Google Authenticator's cloud sync safe?</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              Google Authenticator's cloud sync encrypts data in transit and at rest using Google's infrastructure, but it does not
              use end-to-end encryption. This means Google holds the keys to decrypt your 2FA secrets. TwoFac uses true end-to-end
              encryption where only you hold the decryption keys.
            </p>
          </div>
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2">Does TwoFac work offline?</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              Yes. Like Google Authenticator, TwoFac generates all codes locally on your device using time-based algorithms. No internet
              connection is needed to generate or view your 2FA codes. Sync only requires connectivity when you want to transfer codes between devices.
            </p>
          </div>
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2">Is Google Authenticator still open source?</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              No. Google archived the open-source Android repository in April 2021 at version 2.21. The current app is proprietary
              freeware. TwoFac, by contrast, is fully open source under the MIT License, with all code publicly available on GitHub.
            </p>
          </div>
        </div>
      </div>
    </section>

    <!-- CTA -->
    <section class="py-12 sm:py-16">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
        <h2 class="text-2xl sm:text-3xl font-bold text-secondary-900 dark:text-white mb-4">Ready to Upgrade from Google Authenticator?</h2>
        <p class="text-secondary-600 dark:text-secondary-400 mb-8 max-w-2xl mx-auto">
          Open source, cross-platform, and end-to-end encrypted. TwoFac is everything Google Authenticator should have been.
          Switch in minutes — your 2FA accounts come with you.
        </p>
        <div class="flex flex-col sm:flex-row gap-4 justify-center">
          <RouterLink
            to="/download"
            class="inline-flex items-center justify-center px-8 py-3 rounded-lg bg-primary-600 text-white font-medium hover:bg-primary-700 transition-colors shadow-sm"
          >
            Download TwoFac Free
          </RouterLink>
          <RouterLink
            to="/getting-started"
            class="inline-flex items-center justify-center px-8 py-3 rounded-lg border border-secondary-300 dark:border-secondary-600 text-secondary-700 dark:text-secondary-300 font-medium hover:bg-secondary-50 dark:hover:bg-secondary-800 transition-colors"
          >
            Migration Guide <i class="fa-solid fa-arrow-right-long ml-1" aria-hidden="true" />
          </RouterLink>
        </div>
      </div>
    </section>
  </MainLayout>
</template>
