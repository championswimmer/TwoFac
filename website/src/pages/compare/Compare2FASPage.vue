<script setup lang="ts">
import { RouterLink } from 'vue-router'
import MainLayout from '../../layouts/MainLayout.vue'
import ComparisonTable from '../../components/ComparisonTable.vue'
import { useSEO } from '../../composables/useSEO'

useSEO({
  title: 'TwoFac vs 2FAS — Open Source 2FA Apps Compared (2025)',
  description: 'In-depth comparison of TwoFac and 2FAS authenticator apps. Both are open source, but TwoFac adds native desktop apps, CLI tools, Wear OS, watchOS, and a web PWA that 2FAS lacks.',
  canonicalPath: '/compare/2fas',
})

const features = [
  { name: 'Open Source', twofac: true, competitor: true },
  { name: 'Android App', twofac: true, competitor: true },
  { name: 'iOS App', twofac: true, competitor: true },
  { name: 'Desktop App (macOS/Windows/Linux)', twofac: true, competitor: false },
  { name: 'CLI Tool', twofac: true, competitor: false },
  { name: 'Web App (PWA)', twofac: true, competitor: false },
  { name: 'Browser Extension', twofac: true, competitor: true },
  { name: 'Wear OS', twofac: true, competitor: false },
  { name: 'watchOS', twofac: true, competitor: 'Yes (Apple Watch)' },
  { name: 'TOTP Support', twofac: true, competitor: true },
  { name: 'HOTP Support', twofac: true, competitor: true },
  { name: 'Encrypted Backups', twofac: true, competitor: true },
  { name: 'Biometric Lock', twofac: true, competitor: true },
  { name: 'Cross-Platform Sync', twofac: true, competitor: 'Ecosystem-locked' },
  { name: 'No Account Required', twofac: true, competitor: true },
  { name: 'Kotlin Multiplatform (Native)', twofac: true, competitor: false },
  { name: 'Completely Free', twofac: true, competitor: 'Free core / $9.99/yr Pass' },
]
</script>

<template>
  <MainLayout>
    <!-- Hero -->
    <section class="border-b border-secondary-200 bg-secondary-100 dark:border-secondary-800 dark:bg-secondary-900 py-16 sm:py-24">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
        <h1 class="text-3xl sm:text-4xl lg:text-5xl font-bold text-secondary-900 dark:text-white">
          TwoFac vs <span class="text-primary-600 dark:text-primary-400">2FAS</span>
        </h1>
        <p class="mt-6 text-lg sm:text-xl text-secondary-600 dark:text-secondary-400 max-w-3xl mx-auto leading-relaxed">
          TwoFac and 2FAS are both free, open-source authenticator apps with strong privacy credentials. The key difference?
          TwoFac goes beyond mobile with native desktop apps for macOS, Windows, and Linux, a powerful CLI tool, a web PWA,
          and Wear OS companion support — while 2FAS remains mobile-first with no standalone desktop application.
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
        <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-8 text-center">TwoFac vs 2FAS at a Glance</h2>
        <div class="grid gap-6 sm:grid-cols-3">
          <div class="rounded-xl bg-primary-50 dark:bg-primary-900/20 border border-primary-200 dark:border-primary-800 p-6 text-center">
            <div class="text-3xl font-bold text-primary-600 dark:text-primary-400">11+</div>
            <div class="mt-2 text-sm font-medium text-secondary-700 dark:text-secondary-300">Platforms TwoFac supports</div>
            <div class="mt-1 text-xs text-secondary-500 dark:text-secondary-400">vs mobile + browser for 2FAS</div>
          </div>
          <div class="rounded-xl bg-primary-50 dark:bg-primary-900/20 border border-primary-200 dark:border-primary-800 p-6 text-center">
            <div class="text-3xl font-bold text-primary-600 dark:text-primary-400">$0</div>
            <div class="mt-2 text-sm font-medium text-secondary-700 dark:text-secondary-300">TwoFac — all features free</div>
            <div class="mt-1 text-xs text-secondary-500 dark:text-secondary-400">2FAS charges $9.99/yr for Pass</div>
          </div>
          <div class="rounded-xl bg-primary-50 dark:bg-primary-900/20 border border-primary-200 dark:border-primary-800 p-6 text-center">
            <div class="text-3xl font-bold text-primary-600 dark:text-primary-400">Native</div>
            <div class="mt-2 text-sm font-medium text-secondary-700 dark:text-secondary-300">Kotlin Multiplatform</div>
            <div class="mt-1 text-xs text-secondary-500 dark:text-secondary-400">True native code on every platform</div>
          </div>
        </div>
      </div>
    </section>

    <!-- What is 2FAS -->
    <section class="bg-secondary-50 dark:bg-secondary-900 py-12 sm:py-16">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-4">What Is 2FAS?</h2>
        <div class="space-y-4">
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            2FAS (Two Factor Authentication Service) is a free, open-source authenticator app known for its clean interface and
            privacy-first philosophy. Available on Android and iOS with a browser extension for one-tap code autofill, 2FAS has
            earned a loyal following among privacy-conscious users. It supports TOTP, HOTP, and Steam Guard tokens, and offers
            biometric app locking and encrypted backups via Google Drive or iCloud.
          </p>
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            With over 5 million downloads on Google Play and a 4.3-star rating, 2FAS is a solid mobile-first authenticator. However,
            its ecosystem limitations become evident for users who work across multiple device types. There is no standalone desktop
            application — 2FAS relies entirely on its browser extension paired with the mobile app for computer-based workflows. The
            free tier limits you to a single browser extension connection, with the $9.99/year 2FAS Pass upgrade required for unlimited
            extensions, sync, and autofill.
          </p>
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            Cross-platform sync is locked to your mobile ecosystem: Android users sync via Google Drive, and iOS users sync via iCloud.
            There is no universal cross-platform cloud sync between Android and iOS without the paid tier. 2FAS also lacks a web app,
            CLI tool, or Wear OS support.
          </p>
        </div>
      </div>
    </section>

    <!-- Comparison Table -->
    <section id="comparison-table" class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-12 sm:py-16">
      <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-2">Feature-by-Feature Comparison</h2>
      <p class="text-secondary-600 dark:text-secondary-400 mb-6">Both apps are open source, but they differ significantly in platform coverage and feature depth.</p>
      <ComparisonTable :features="features" competitor-name="2FAS" />
    </section>

    <!-- Platform Availability -->
    <section class="bg-secondary-50 dark:bg-secondary-900 py-12 sm:py-16">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-4">Platform Availability: Mobile-First vs Truly Cross-Platform</h2>
        <div class="space-y-4">
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            2FAS takes a mobile-first approach: the core experience lives on your phone, and the browser extension serves as a bridge
            to get codes on your computer. This works well for basic use cases, but it means you always need your phone nearby when
            working on a desktop or laptop. If your phone is charging in another room, you're locked out.
          </p>
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            TwoFac takes a different philosophy — <strong class="text-secondary-900 dark:text-white">every platform is a first-class citizen</strong>.
            Native desktop apps for macOS, Windows, and Linux run independently, with their own local code storage and sync capabilities.
            The CLI tool generates codes from any terminal. The progressive web app works in any modern browser. And companion apps for
            Wear OS and Apple Watch let you view codes on your wrist.
          </p>
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            Both apps offer Apple Watch support — 2FAS has a watchOS companion, and so does TwoFac. But TwoFac adds Wear OS support
            for Android smartwatch users, which 2FAS lacks. Combined with the CLI, web PWA, and standalone desktop apps, TwoFac covers
            significantly more ground.
          </p>
        </div>
      </div>
    </section>

    <!-- Security & Privacy -->
    <section class="py-12 sm:py-16">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-4">Security and Privacy: Two Open-Source Champions</h2>
        <div class="space-y-4">
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            Both TwoFac and 2FAS take security seriously. Both are fully open source with code on GitHub, both offer biometric
            app locking, and both generate codes locally without any server interaction. Neither app collects personal data, displays
            ads, or requires an account to function.
          </p>
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            Where they diverge is in backup encryption and sync strategy. 2FAS encrypts backups with a user-set password and stores
            them on Google Drive (Android) or iCloud (iOS). This ties your backup security to your Google or Apple account security.
            TwoFac uses end-to-end encryption for sync and backup, with keys derived from your passphrase that never leave your devices.
          </p>
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            Both apps are excellent choices from a privacy perspective. If you're already comfortable with the Google/Apple ecosystem
            for backups, 2FAS works well. If you prefer independent, end-to-end encrypted sync that doesn't rely on Big Tech cloud
            services, TwoFac has the edge.
          </p>
        </div>
      </div>
    </section>

    <!-- Backup & Sync -->
    <section class="bg-secondary-50 dark:bg-secondary-900 py-12 sm:py-16">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-4">Backup, Sync, and Cross-Device Access</h2>
        <div class="space-y-4">
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            2FAS offers ecosystem-specific sync: Android devices sync through Google Drive, and iOS devices through iCloud. This means
            if you use both an Android phone and an iPad, your 2FA codes won't sync between them on the free tier. The 2FAS Pass
            upgrade ($9.99/year) unlocks cross-platform sync, unlimited browser extensions, and autofill capabilities.
          </p>
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            TwoFac provides end-to-end encrypted sync across all your devices regardless of platform — Android, iOS, desktop, web,
            and wearables all stay in sync automatically. There is no premium tier or paid upgrade. Every sync and backup feature is
            free and available on day one.
          </p>
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            For migration, TwoFac supports importing accounts from 2FAS via file import, making it easy to switch if you're looking
            for broader platform support without sacrificing the open-source ethos you value.
          </p>
        </div>
      </div>
    </section>

    <!-- Pricing -->
    <section class="py-12 sm:py-16">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-4">Pricing: Free Core vs Completely Free</h2>
        <div class="space-y-4">
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            2FAS's core authenticator app is free with unlimited token storage. However, the free tier limits you to a single browser
            extension connection and ecosystem-only sync. The 2FAS Pass upgrade at $9.99/year unlocks full sync, autofill, and unlimited
            browser extensions.
          </p>
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            TwoFac is completely free with no tiers, no subscriptions, and no feature gates. Every feature — desktop apps, CLI, browser
            extensions, wearable apps, sync, and encrypted backups — is included at no cost. TwoFac is funded as an open-source
            community project, not a subscription service.
          </p>
        </div>
      </div>
    </section>

    <!-- Why Choose TwoFac -->
    <section class="bg-secondary-50 dark:bg-secondary-900 py-12 sm:py-16">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-8">Why Choose TwoFac Over 2FAS?</h2>
        <div class="grid gap-6 sm:grid-cols-2">
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm border border-secondary-200 dark:border-secondary-700">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2"><i class="fa-solid fa-desktop mr-2" aria-hidden="true" />Native Desktop Apps</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              Standalone desktop apps for macOS, Windows, and Linux that work independently — no phone-to-browser bridge required.
              2FAS has no desktop application at all.
            </p>
          </div>
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm border border-secondary-200 dark:border-secondary-700">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2"><i class="fa-solid fa-terminal mr-2" aria-hidden="true" />Powerful CLI Tool</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              Generate 2FA codes from your terminal — perfect for developers and system administrators. Scriptable, pipeable, and
              integrates into any workflow.
            </p>
          </div>
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm border border-secondary-200 dark:border-secondary-700">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2"><i class="fa-solid fa-clock mr-2" aria-hidden="true" />Wear OS + watchOS</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              Both apps support Apple Watch, but only TwoFac adds Wear OS support. View codes on your Android smartwatch without
              reaching for your phone.
            </p>
          </div>
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm border border-secondary-200 dark:border-secondary-700">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2"><i class="fa-solid fa-globe mr-2" aria-hidden="true" />Web PWA</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              Access your codes from any browser without installing an extension. TwoFac's progressive web app works on any device
              with a modern web browser.
            </p>
          </div>
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm border border-secondary-200 dark:border-secondary-700">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2">🆓 100% Free, No Tiers</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              Unlike 2FAS Pass ($9.99/yr), every TwoFac feature is free — unlimited extensions, full sync, all platforms included.
            </p>
          </div>
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm border border-secondary-200 dark:border-secondary-700">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2"><i class="fa-solid fa-code mr-2" aria-hidden="true" />Kotlin Multiplatform</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              Built with Kotlin Multiplatform for true native performance. A single shared codebase compiles to native code on every
              platform — no runtime overhead.
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
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2">Both are open source — what's the real difference?</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              The core difference is platform coverage. 2FAS is mobile-first with a browser extension bridge. TwoFac is truly
              cross-platform with standalone desktop apps, a CLI, a web PWA, and Wear OS support. Both are excellent on mobile; TwoFac
              extends that excellence to every other platform.
            </p>
          </div>
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm border border-secondary-200 dark:border-secondary-700">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2">Can I import my 2FAS accounts into TwoFac?</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              Yes. TwoFac supports importing from 2FAS via file export/import. Export your 2FAS data as an encrypted backup file,
              then import it into TwoFac. The process is straightforward and preserves all your accounts.
            </p>
          </div>
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm border border-secondary-200 dark:border-secondary-700">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2">Does 2FAS have a desktop app?</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              No. 2FAS does not offer a standalone desktop application. It provides a browser extension that communicates with the
              mobile app to autofill codes. You always need your phone nearby. TwoFac offers fully independent desktop apps for
              macOS, Windows, and Linux.
            </p>
          </div>
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm border border-secondary-200 dark:border-secondary-700">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2">Is 2FAS really free?</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              The core 2FAS app is free with unlimited tokens. However, the free tier limits you to one browser extension and
              ecosystem-only sync. Full sync, autofill, and unlimited extensions require 2FAS Pass at $9.99/year. TwoFac has no
              paid tiers — every feature is free.
            </p>
          </div>
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm border border-secondary-200 dark:border-secondary-700">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2">Which app is better for developers?</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              TwoFac, hands down. The CLI tool lets you generate 2FA codes directly in the terminal, integrate with scripts, and
              use in SSH/CI workflows. Combined with the desktop app and browser extension, TwoFac fits naturally into a developer's
              workflow. 2FAS doesn't offer any CLI or desktop tools.
            </p>
          </div>
        </div>
      </div>
    </section>

    <!-- CTA -->
    <section class="bg-secondary-50 dark:bg-secondary-900 py-12 sm:py-16">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
        <h2 class="text-2xl sm:text-3xl font-bold text-secondary-900 dark:text-white mb-4">Ready to Go Beyond Mobile?</h2>
        <p class="text-secondary-600 dark:text-secondary-400 mb-8 max-w-2xl mx-auto">
          If you love 2FAS's open-source ethos but need desktop, CLI, and wearable support, TwoFac is the natural upgrade.
          Import your 2FAS accounts and get started in minutes.
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
            Getting Started Guide <i class="fa-solid fa-arrow-right-long ml-1" aria-hidden="true" />
          </RouterLink>
        </div>
      </div>
    </section>
  </MainLayout>
</template>
