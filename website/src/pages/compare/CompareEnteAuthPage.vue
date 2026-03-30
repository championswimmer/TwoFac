<script setup lang="ts">
import { RouterLink } from 'vue-router'
import MainLayout from '../../layouts/MainLayout.vue'
import ComparisonTable from '../../components/ComparisonTable.vue'
import { useSEO } from '../../composables/useSEO'

useSEO({
  title: 'TwoFac vs Ente Auth — Privacy-First 2FA Apps Compared (2025)',
  description: 'In-depth comparison of TwoFac and Ente Auth. Both are open source with E2E encryption, but TwoFac adds a CLI tool, browser extensions, Wear OS, and watchOS — built natively with Kotlin Multiplatform.',
  canonicalPath: '/compare/ente-auth',
})

const features = [
  { name: 'Open Source', twofac: '✓', competitor: '✓' },
  { name: 'Android App', twofac: '✓', competitor: '✓' },
  { name: 'iOS App', twofac: '✓', competitor: '✓' },
  { name: 'Desktop App (macOS/Windows/Linux)', twofac: '✓', competitor: '✓' },
  { name: 'CLI Tool', twofac: '✓', competitor: '✗' },
  { name: 'Web App', twofac: '✓', competitor: '✓' },
  { name: 'Browser Extension', twofac: '✓', competitor: '✗' },
  { name: 'Wear OS', twofac: '✓', competitor: '✗' },
  { name: 'watchOS', twofac: '✓', competitor: '✗' },
  { name: 'TOTP Support', twofac: '✓', competitor: '✓' },
  { name: 'HOTP Support', twofac: '✓', competitor: '✓' },
  { name: 'E2E Encrypted Sync', twofac: '✓', competitor: '✓' },
  { name: 'Encrypted Backups', twofac: '✓', competitor: '✓' },
  { name: 'Biometric Lock', twofac: '✓', competitor: '✗' },
  { name: 'No Account Required', twofac: '✓', competitor: '✓ (local) / ✗ (sync)' },
  { name: 'Kotlin Multiplatform (Native)', twofac: '✓', competitor: '✗ (Flutter)' },
  { name: 'Completely Free', twofac: '✓', competitor: '✓ (5 GB free tier)' },
]
</script>

<template>
  <MainLayout>
    <!-- Hero -->
    <section class="bg-gradient-to-br from-primary-50 to-white dark:from-secondary-900 dark:to-secondary-950 py-16 sm:py-24">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
        <h1 class="text-3xl sm:text-4xl lg:text-5xl font-bold text-secondary-900 dark:text-white">
          TwoFac vs <span class="text-primary-600 dark:text-primary-400">Ente Auth</span>
        </h1>
        <p class="mt-6 text-lg sm:text-xl text-secondary-600 dark:text-secondary-400 max-w-3xl mx-auto leading-relaxed">
          TwoFac and Ente Auth are both open-source, privacy-focused authenticator apps with end-to-end encrypted sync, desktop
          support, and web access. The difference? TwoFac goes further with a dedicated CLI tool, browser extensions for autofill,
          wearable apps for Wear OS and Apple Watch, and true native performance via Kotlin Multiplatform instead of Flutter.
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
            See Full Comparison ↓
          </a>
        </div>
      </div>
    </section>

    <!-- At a Glance -->
    <section class="py-12 sm:py-16">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-8 text-center">TwoFac vs Ente Auth at a Glance</h2>
        <div class="grid gap-6 sm:grid-cols-3">
          <div class="rounded-xl bg-primary-50 dark:bg-primary-900/20 border border-primary-200 dark:border-primary-800 p-6 text-center">
            <div class="text-3xl font-bold text-primary-600 dark:text-primary-400">CLI</div>
            <div class="mt-2 text-sm font-medium text-secondary-700 dark:text-secondary-300">Terminal-based 2FA codes</div>
            <div class="mt-1 text-xs text-secondary-500 dark:text-secondary-400">Ente Auth has no CLI tool</div>
          </div>
          <div class="rounded-xl bg-primary-50 dark:bg-primary-900/20 border border-primary-200 dark:border-primary-800 p-6 text-center">
            <div class="text-3xl font-bold text-primary-600 dark:text-primary-400">Native</div>
            <div class="mt-2 text-sm font-medium text-secondary-700 dark:text-secondary-300">Kotlin Multiplatform</div>
            <div class="mt-1 text-xs text-secondary-500 dark:text-secondary-400">Ente Auth uses Flutter</div>
          </div>
          <div class="rounded-xl bg-primary-50 dark:bg-primary-900/20 border border-primary-200 dark:border-primary-800 p-6 text-center">
            <div class="text-3xl font-bold text-primary-600 dark:text-primary-400">⌚</div>
            <div class="mt-2 text-sm font-medium text-secondary-700 dark:text-secondary-300">Wear OS + watchOS</div>
            <div class="mt-1 text-xs text-secondary-500 dark:text-secondary-400">Ente Auth has no wearable apps</div>
          </div>
        </div>
      </div>
    </section>

    <!-- What is Ente Auth -->
    <section class="bg-secondary-50 dark:bg-secondary-900 py-12 sm:py-16">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-4">What Is Ente Auth?</h2>
        <div class="space-y-4">
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            Ente Auth is a free, open-source 2FA authenticator from Ente, the company known for its encrypted photo storage service.
            Built with Flutter, it's available on Android, iOS, macOS, Windows, Linux, and the web. Ente Auth is widely recommended
            in privacy-focused communities like Privacy Guides for its end-to-end encrypted cloud backup and zero-knowledge architecture.
          </p>
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            The app supports TOTP and HOTP standards, offers cross-device sync encrypted with your master key, and can be used
            without an account for local-only storage. Cloud backup requires a free Ente account (5 GB forever-free storage plan).
            Ente Auth also provides features like tap-to-copy, next code preview, pinning, tagging, and search for organizing
            your accounts.
          </p>
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            Ente Auth is an excellent authenticator, and it shares many strengths with TwoFac. Where TwoFac differentiates itself
            is in the periphery: CLI access for developers, browser extensions for seamless autofill, wearable apps for convenience,
            and native performance through Kotlin Multiplatform instead of Flutter's cross-compiled approach.
          </p>
        </div>
      </div>
    </section>

    <!-- Comparison Table -->
    <section id="comparison-table" class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-12 sm:py-16">
      <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-2">Feature-by-Feature Comparison</h2>
      <p class="text-secondary-600 dark:text-secondary-400 mb-6">Both apps share a strong foundation — here's where they diverge.</p>
      <ComparisonTable :features="features" competitor-name="Ente Auth" />
    </section>

    <!-- Platform & Architecture -->
    <section class="bg-secondary-50 dark:bg-secondary-900 py-12 sm:py-16">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-4">Architecture: Kotlin Multiplatform vs Flutter</h2>
        <div class="space-y-4">
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            Ente Auth is built with Flutter, Google's cross-platform UI framework. Flutter compiles to native ARM code on mobile
            but uses its own rendering engine (Skia/Impeller) rather than native platform UI widgets. This means the app looks
            identical on every platform but doesn't always feel native — scrolling physics, text selection, and accessibility can
            diverge from platform conventions.
          </p>
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            TwoFac is built with <strong class="text-secondary-900 dark:text-white">Kotlin Multiplatform</strong>, which compiles shared
            business logic to truly native code on each platform: JVM on Android and desktop, native binary on iOS, and WebAssembly
            on the web. The UI layer uses each platform's native framework — Jetpack Compose on Android, SwiftUI on iOS, and Compose
            Multiplatform on desktop. The result is an app that feels genuinely native on every platform while sharing a single
            codebase for core logic.
          </p>
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            For most users, the difference is subtle — both apps work well. But if you value native platform behavior, smooth
            integration with system features like biometrics and accessibility, and optimal performance without a runtime layer,
            TwoFac's architecture has a clear advantage.
          </p>
        </div>
      </div>
    </section>

    <!-- Security & Encryption -->
    <section class="py-12 sm:py-16">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-4">Security and Encryption: Strong on Both Sides</h2>
        <div class="space-y-4">
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            Both TwoFac and Ente Auth are open source, both offer end-to-end encryption, and both generate codes locally without
            network requests. This is the strongest security posture you can find in a 2FA authenticator, and both apps deserve praise
            for it.
          </p>
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            Ente Auth derives its encryption key from your master password using a zero-knowledge architecture. Your secrets are
            encrypted client-side before being sent to Ente's servers. Even Ente themselves cannot read your data. The app has been
            independently audited, adding further confidence.
          </p>
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            TwoFac follows the same zero-knowledge principle with end-to-end encryption for synced data. Where TwoFac adds security
            value is with <strong class="text-secondary-900 dark:text-white">biometric locking</strong> (fingerprint and face recognition)
            to prevent unauthorized access to the app itself. Ente Auth has been noted for lacking biometric lock options, which means
            anyone with access to your unlocked phone can open the app and view your codes.
          </p>
        </div>
      </div>
    </section>

    <!-- Developer & Power User Features -->
    <section class="bg-secondary-50 dark:bg-secondary-900 py-12 sm:py-16">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-4">Developer and Power User Features</h2>
        <div class="space-y-4">
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            If you're a developer, system administrator, or power user, the tooling around your 2FA app matters just as much as
            the app itself. This is where TwoFac and Ente Auth diverge most significantly.
          </p>
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            <strong class="text-secondary-900 dark:text-white">TwoFac's CLI tool</strong> is a native command-line application that
            generates 2FA codes directly in your terminal. It supports auto-refreshing code display, account management, backup workflows, and
            platform info commands. You can pipe codes into scripts, use them in CI/CD pipelines, or generate codes over SSH on remote
            servers. Ente Auth has a CLI for exporting data but not for generating codes interactively.
          </p>
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            <strong class="text-secondary-900 dark:text-white">TwoFac's browser extension</strong> integrates with Chrome and Firefox
            to autofill 2FA codes directly in login forms. No more switching to another app, copying a code, and pasting it manually.
            Ente Auth does not offer a browser extension — you need to switch to the desktop app or phone to view codes.
          </p>
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            <strong class="text-secondary-900 dark:text-white">Wearable apps</strong> round out TwoFac's advantage. Native apps for
            both Wear OS and Apple Watch let you view codes on your wrist — perfect for quick authentications when your phone or
            computer isn't in reach. Ente Auth offers no wearable support.
          </p>
        </div>
      </div>
    </section>

    <!-- Pricing -->
    <section class="py-12 sm:py-16">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-4">Pricing and Account Requirements</h2>
        <div class="space-y-4">
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            Ente Auth is free to use for both local-only and cloud-synced modes. Cloud sync requires a free Ente account with a
            5 GB forever-free storage plan. You need to use the account at least once per year to keep it active. Ente also offers
            paid tiers for additional storage, though the free plan is more than sufficient for 2FA data.
          </p>
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            TwoFac is completely free with no account requirement for local use. All features — desktop, CLI, browser extensions,
            wearables, and encrypted backups — are available at no cost. There are no storage limits, no paid tiers, and no annual
            activity requirements.
          </p>
        </div>
      </div>
    </section>

    <!-- Why Choose TwoFac -->
    <section class="bg-secondary-50 dark:bg-secondary-900 py-12 sm:py-16">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-8">Why Choose TwoFac Over Ente Auth?</h2>
        <div class="grid gap-6 sm:grid-cols-2">
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm border border-secondary-200 dark:border-secondary-700">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2">💻 CLI for Developers</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              Generate 2FA codes from your terminal with auto-refresh, account management, and scriptable output. Ente Auth offers
              no interactive CLI.
            </p>
          </div>
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm border border-secondary-200 dark:border-secondary-700">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2">🧩 Browser Extension</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              Autofill 2FA codes directly in your browser login forms. No app-switching, no manual code entry. Ente Auth doesn't
              have a browser extension.
            </p>
          </div>
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm border border-secondary-200 dark:border-secondary-700">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2">⌚ Wearable Apps</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              View 2FA codes on your Wear OS or Apple Watch. Quick glance at your wrist beats reaching for your phone every time.
              Ente Auth has no wearable support.
            </p>
          </div>
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm border border-secondary-200 dark:border-secondary-700">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2">🔧 True Native Performance</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              Kotlin Multiplatform compiles to native code with platform-native UI frameworks — no Flutter rendering layer. Faster
              startup, better animations, and genuine platform behavior.
            </p>
          </div>
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm border border-secondary-200 dark:border-secondary-700">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2">🔒 Biometric Lock</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              Protect your 2FA codes with fingerprint or face recognition. Ente Auth lacks biometric lock, leaving codes accessible
              to anyone with device access.
            </p>
          </div>
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm border border-secondary-200 dark:border-secondary-700">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2">🆓 No Account Required</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              Use every TwoFac feature without creating any account. Ente Auth requires an Ente account for cloud sync and backup.
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
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2">Both have E2E encryption — what's different about TwoFac?</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              The encryption models are similar — both use zero-knowledge E2E encryption for synced data. The differences are in the
              surrounding features: TwoFac adds a CLI, browser extensions, wearable apps, biometric lock, and native performance via
              Kotlin Multiplatform. Ente Auth is slightly more limited in tooling around the core authenticator.
            </p>
          </div>
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm border border-secondary-200 dark:border-secondary-700">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2">Is Ente Auth really built with Flutter?</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              Yes. Ente Auth is built with Flutter, which uses its own rendering engine rather than native platform UI widgets.
              TwoFac uses Kotlin Multiplatform with Compose Multiplatform, which compiles to native code and uses platform-native
              UI frameworks for a more authentic feel on each platform.
            </p>
          </div>
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm border border-secondary-200 dark:border-secondary-700">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2">Does Ente Auth have biometric lock?</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              As of 2025, Ente Auth has been noted for lacking biometric lock options. This means anyone with access to your unlocked
              phone can open Ente Auth and view all your codes. TwoFac supports fingerprint and face recognition to protect app access.
            </p>
          </div>
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm border border-secondary-200 dark:border-secondary-700">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2">Can I use Ente Auth without an account?</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              You can use Ente Auth locally without an account, but cloud sync and backup require a free Ente account (5 GB
              forever-free plan, used once per year). TwoFac doesn't require any account for local use or encrypted backups.
            </p>
          </div>
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm border border-secondary-200 dark:border-secondary-700">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2">Which app is better for developers?</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              TwoFac. Its CLI tool generates codes interactively in the terminal with auto-refresh, and the browser extension autofills
              codes in login forms. These two features alone save significant time in a developer's daily workflow. Ente Auth has
              a CLI for data export but not for interactive code generation.
            </p>
          </div>
        </div>
      </div>
    </section>

    <!-- CTA -->
    <section class="bg-secondary-50 dark:bg-secondary-900 py-12 sm:py-16">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
        <h2 class="text-2xl sm:text-3xl font-bold text-secondary-900 dark:text-white mb-4">Try TwoFac — The Complete 2FA Experience</h2>
        <p class="text-secondary-600 dark:text-secondary-400 mb-8 max-w-2xl mx-auto">
          If you love Ente Auth's privacy-first approach but want CLI access, browser autofill, wearable apps, and native
          performance, TwoFac is the next step. Available on 11+ platforms, completely free.
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
            Explore All Features →
          </RouterLink>
        </div>
      </div>
    </section>
  </MainLayout>
</template>
