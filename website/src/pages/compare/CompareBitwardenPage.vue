<script setup lang="ts">
import { RouterLink } from 'vue-router'
import MainLayout from '../../layouts/MainLayout.vue'
import ComparisonTable from '../../components/ComparisonTable.vue'
import { useSEO } from '../../composables/useSEO'

useSEO({
  title: 'TwoFac vs Bitwarden Authenticator — Dedicated 2FA vs Password Manager Add-On (2025)',
  description: 'Detailed comparison of TwoFac and Bitwarden Authenticator. TwoFac is a purpose-built, lightweight 2FA app with desktop, CLI, and wearable support — no account or subscription required.',
  canonicalPath: '/compare/bitwarden',
})

const features = [
  { name: 'Standalone 2FA App', twofac: true, competitor: 'Partial (standalone + PM add-on)' },
  { name: 'Open Source', twofac: true, competitor: true },
  { name: 'Android App', twofac: true, competitor: true },
  { name: 'iOS App', twofac: true, competitor: true },
  { name: 'Desktop App (macOS/Windows/Linux)', twofac: true, competitor: 'No (standalone) / Yes (via PM)' },
  { name: 'CLI Tool', twofac: true, competitor: false },
  { name: 'Web App (PWA)', twofac: true, competitor: 'No (standalone) / Yes (via PM)' },
  { name: 'Browser Extension', twofac: true, competitor: 'No (standalone) / Yes (via PM)' },
  { name: 'Wear OS', twofac: true, competitor: false },
  { name: 'watchOS', twofac: true, competitor: false },
  { name: 'TOTP Support', twofac: true, competitor: true },
  { name: 'HOTP Support', twofac: true, competitor: false },
  { name: 'E2E Encrypted Sync', twofac: true, competitor: 'Yes (via PM vault)' },
  { name: 'Biometric Lock', twofac: true, competitor: false },
  { name: 'No Account Required', twofac: true, competitor: 'Yes (standalone) / No (PM)' },
  { name: 'Completely Free', twofac: true, competitor: 'Standalone free / PM TOTP: $10/yr' },
  { name: 'Lightweight', twofac: true, competitor: 'No (PM is heavy)' },
  { name: 'Kotlin Multiplatform (Native)', twofac: true, competitor: false },
]
</script>

<template>
  <MainLayout>
    <!-- Hero -->
    <section class="border-b border-secondary-200 bg-secondary-100 dark:border-secondary-800 dark:bg-secondary-900 py-16 sm:py-24">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
        <h1 class="text-3xl sm:text-4xl lg:text-5xl font-bold text-secondary-900 dark:text-white">
          TwoFac vs <span class="text-primary-600 dark:text-primary-400">Bitwarden Authenticator</span>
        </h1>
        <p class="mt-6 text-lg sm:text-xl text-secondary-600 dark:text-secondary-400 max-w-3xl mx-auto leading-relaxed">
          Bitwarden offers 2FA through two paths: a free standalone mobile app and an integrated authenticator in the Bitwarden
          Password Manager (premium feature). TwoFac is a purpose-built, dedicated 2FA app that works on 11+ platforms —
          including desktop, CLI, browser extensions, and smartwatches — completely free with no account required.
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
        <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-8 text-center">TwoFac vs Bitwarden Authenticator at a Glance</h2>
        <div class="grid gap-6 sm:grid-cols-3">
          <div class="rounded-xl bg-primary-50 dark:bg-primary-900/20 border border-primary-200 dark:border-primary-800 p-6 text-center">
            <div class="text-3xl font-bold text-primary-600 dark:text-primary-400">$0</div>
            <div class="mt-2 text-sm font-medium text-secondary-700 dark:text-secondary-300">All features, forever free</div>
            <div class="mt-1 text-xs text-secondary-500 dark:text-secondary-400">Bitwarden PM TOTP needs $10/yr premium</div>
          </div>
          <div class="rounded-xl bg-primary-50 dark:bg-primary-900/20 border border-primary-200 dark:border-primary-800 p-6 text-center">
            <div class="text-3xl font-bold text-primary-600 dark:text-primary-400">11+</div>
            <div class="mt-2 text-sm font-medium text-secondary-700 dark:text-secondary-300">Platforms supported</div>
            <div class="mt-1 text-xs text-secondary-500 dark:text-secondary-400">Bitwarden standalone: mobile only</div>
          </div>
          <div class="rounded-xl bg-primary-50 dark:bg-primary-900/20 border border-primary-200 dark:border-primary-800 p-6 text-center">
            <div class="text-3xl font-bold text-primary-600 dark:text-primary-400"><i class="fa-solid fa-bullseye" aria-hidden="true" /></div>
            <div class="mt-2 text-sm font-medium text-secondary-700 dark:text-secondary-300">Purpose-built for 2FA</div>
            <div class="mt-1 text-xs text-secondary-500 dark:text-secondary-400">Not a password manager add-on</div>
          </div>
        </div>
      </div>
    </section>

    <!-- What is Bitwarden Authenticator -->
    <section class="bg-secondary-50 dark:bg-secondary-900 py-12 sm:py-16">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-4">What Is Bitwarden Authenticator?</h2>
        <div class="space-y-4">
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            Bitwarden offers two distinct 2FA solutions, which can be confusing. The <strong class="text-secondary-900 dark:text-white">Bitwarden
            Authenticator</strong> is a free, standalone mobile app (Android and iOS) that generates TOTP codes independently —
            no Bitwarden account required. It stores codes locally on your device or can optionally sync with a Bitwarden vault.
          </p>
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            The <strong class="text-secondary-900 dark:text-white">integrated authenticator</strong> lives inside the Bitwarden Password Manager
            and attaches TOTP codes directly to vault login items. This enables auto-copy of codes after password autofill and is
            available across all Bitwarden platforms (desktop, web, extensions). However, TOTP code generation in the password manager
            requires a Premium subscription ($10/year) or a Families/Teams/Enterprise plan.
          </p>
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            Both Bitwarden products are open source, which is commendable. However, the standalone authenticator is mobile-only with no
            native desktop app, no CLI, no web interface, and no wearable support. Users have reported issues with the standalone app
            including missing sorting/renaming features, occasional crashes on iOS, and JSON export bugs. The integrated authenticator
            is more fully featured but comes at a subscription cost and requires a Bitwarden account.
          </p>
        </div>
      </div>
    </section>

    <!-- Comparison Table -->
    <section id="comparison-table" class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-12 sm:py-16">
      <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-2">Feature-by-Feature Comparison</h2>
      <p class="text-secondary-600 dark:text-secondary-400 mb-6">Comparing TwoFac against the Bitwarden standalone authenticator app and noting where the PM integration differs.</p>
      <ComparisonTable :features="features" competitor-name="Bitwarden" />
    </section>

    <!-- Standalone vs Dedicated -->
    <section class="bg-secondary-50 dark:bg-secondary-900 py-12 sm:py-16">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-4">Purpose-Built 2FA vs Password Manager Add-On</h2>
        <div class="space-y-4">
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            The fundamental question is whether your 2FA should live inside your password manager or in a separate, dedicated app.
            Security best practice generally recommends keeping them separate — if your password manager is compromised, your 2FA
            codes shouldn't go down with it. This "separation of concerns" principle is why dedicated authenticator apps exist.
          </p>
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            Bitwarden's standalone authenticator follows this principle but is limited to mobile devices with basic functionality.
            The more feature-rich integrated authenticator goes against this principle by bundling 2FA inside the password manager.
          </p>
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            <strong class="text-secondary-900 dark:text-white">TwoFac is purpose-built for 2FA.</strong> It's not a side feature
            of a password manager — it's a fully-featured, cross-platform authenticator that treats every platform as a first-class
            citizen. You get the security benefit of keeping 2FA separate from passwords, combined with the convenience of desktop,
            CLI, browser, and wearable access.
          </p>
        </div>
      </div>
    </section>

    <!-- Platform Comparison -->
    <section class="py-12 sm:py-16">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-4">Platform Coverage: The Bitwarden Confusion</h2>
        <div class="space-y-4">
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            The platform story with Bitwarden is confusing because the standalone authenticator and the password manager have
            different platform support. The <strong class="text-secondary-900 dark:text-white">standalone Bitwarden Authenticator</strong>
            only runs on Android and iOS — no desktop, no web, no CLI, no browser extension, no wearable. If you want to view
            TOTP codes on your computer, you need the full Bitwarden Password Manager with a premium subscription.
          </p>
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            With TwoFac, there's no confusion. <strong class="text-secondary-900 dark:text-white">One app, 11+ platforms, all free.</strong>
            Android, iOS, macOS, Windows, Linux, web PWA, CLI, browser extension, Wear OS, and Apple Watch — every platform has a
            first-class, native TwoFac experience. No separate products, no premium gates, no subscription tiers.
          </p>
        </div>
      </div>
    </section>

    <!-- Security -->
    <section class="bg-secondary-50 dark:bg-secondary-900 py-12 sm:py-16">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-4">Security: Both Open Source, Different Approaches</h2>
        <div class="space-y-4">
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            Both TwoFac and Bitwarden are fully open source, which is excellent for security transparency. Bitwarden has undergone
            multiple third-party security audits (Cure53, HackerOne, Insight Risk) and holds SOC 2, GDPR, CCPA, and HIPAA compliance
            certifications — impressive credentials for a password manager.
          </p>
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            The Bitwarden Password Manager uses AES-256 end-to-end encryption for vault data, and TOTP secrets stored in the vault
            inherit this encryption. The standalone authenticator, however, stores codes locally on your device without vault
            encryption unless you opt into vault sync (which requires a Bitwarden account).
          </p>
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            TwoFac uses end-to-end encryption for all synced and backed-up data. The standalone app also supports biometric locking
            (fingerprint and face recognition) to prevent unauthorized access — a feature the standalone Bitwarden Authenticator
            doesn't currently offer.
          </p>
        </div>
      </div>
    </section>

    <!-- Pricing -->
    <section class="py-12 sm:py-16">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-4">Pricing: Free vs Freemium</h2>
        <div class="space-y-4">
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            The Bitwarden standalone authenticator is free for basic TOTP generation on mobile. However, the more feature-rich
            integrated authenticator (with desktop access, browser extension, autofill, and vault sync) requires a Bitwarden Premium
            subscription at $10/year. Families plans are $40/year, and business plans start at $4/user/month.
          </p>
          <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
            TwoFac is <strong class="text-secondary-900 dark:text-white">completely free with no tiers</strong>. Every feature —
            desktop apps, CLI, browser extensions, wearable apps, encrypted sync, and biometric lock — is available at no cost.
            There are no premium upgrades, no per-user fees, and no annual subscriptions. TwoFac will never charge for core
            functionality.
          </p>
        </div>
      </div>
    </section>

    <!-- Why Choose TwoFac -->
    <section class="bg-secondary-50 dark:bg-secondary-900 py-12 sm:py-16">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-8">Why Choose TwoFac Over Bitwarden Authenticator?</h2>
        <div class="grid gap-6 sm:grid-cols-2">
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm border border-secondary-200 dark:border-secondary-700">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2"><i class="fa-solid fa-bullseye mr-2" aria-hidden="true" />Purpose-Built for 2FA</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              TwoFac is a dedicated 2FA app — not a side feature of a password manager. It does one thing and does it
              exceptionally well across every platform.
            </p>
          </div>
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm border border-secondary-200 dark:border-secondary-700">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2"><i class="fa-solid fa-gift mr-2" aria-hidden="true" />100% Free, All Platforms</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              No premium tier, no subscription. Desktop, CLI, browser extension, and wearable access are free. Bitwarden charges
              $10/year for TOTP in the password manager.
            </p>
          </div>
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm border border-secondary-200 dark:border-secondary-700">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2"><i class="fa-solid fa-desktop mr-2" aria-hidden="true" />Native Desktop & CLI</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              Standalone desktop apps for macOS, Windows, and Linux, plus a CLI tool for terminal workflows. Bitwarden's standalone
              authenticator is mobile-only.
            </p>
          </div>
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm border border-secondary-200 dark:border-secondary-700">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2"><i class="fa-solid fa-clock mr-2" aria-hidden="true" />Wearable Apps</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              View codes on your Wear OS or Apple Watch. Neither Bitwarden product offers wearable companion apps.
            </p>
          </div>
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm border border-secondary-200 dark:border-secondary-700">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2"><i class="fa-solid fa-feather-pointed mr-2" aria-hidden="true" />Lightweight & Fast</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              No password vault, no credential management, no bloat. TwoFac starts fast, runs lean, and focuses entirely on
              generating and managing 2FA codes.
            </p>
          </div>
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm border border-secondary-200 dark:border-secondary-700">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2"><i class="fa-solid fa-user-check mr-2" aria-hidden="true" />No Account Required</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              Start using TwoFac immediately — no sign-up, no email verification. Bitwarden's full-featured authenticator
              requires a Bitwarden account and Premium subscription.
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
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2">Should I keep my 2FA separate from my password manager?</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              Security experts generally recommend separation. If your password manager is compromised, having 2FA in a separate app
              means your second factor is still secure. TwoFac follows this principle — it's a dedicated 2FA app that complements any
              password manager (including Bitwarden).
            </p>
          </div>
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm border border-secondary-200 dark:border-secondary-700">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2">What's the difference between Bitwarden Authenticator and Bitwarden's built-in TOTP?</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              Bitwarden Authenticator is a separate free mobile app that generates TOTP codes independently. Bitwarden's built-in TOTP
              is a Premium feature ($10/year) inside the Password Manager that attaches codes to vault items for auto-copy after autofill.
              They can sync with each other, but serve different use cases.
            </p>
          </div>
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm border border-secondary-200 dark:border-secondary-700">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2">Can I use TwoFac alongside Bitwarden Password Manager?</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              Absolutely. TwoFac and Bitwarden complement each other perfectly — Bitwarden manages your passwords, TwoFac manages your
              2FA codes. This is actually the most secure setup: keeping your first factor (passwords) and second factor (TOTP codes)
              in separate applications.
            </p>
          </div>
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm border border-secondary-200 dark:border-secondary-700">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2">Does the Bitwarden standalone authenticator have a desktop app?</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              No. The Bitwarden Authenticator standalone app is mobile-only (Android and iOS). To access TOTP codes on desktop, you
              need the full Bitwarden Password Manager with a Premium subscription. TwoFac offers native desktop apps for macOS,
              Windows, and Linux at no cost.
            </p>
          </div>
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm border border-secondary-200 dark:border-secondary-700">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2">Is the Bitwarden standalone authenticator actually free?</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              The standalone mobile app is free for basic TOTP generation. However, it's limited to mobile with no desktop or CLI
              support. For the full experience (desktop, browser, autofill), you need Bitwarden Premium at $10/year. TwoFac provides
              the full experience across all platforms for free.
            </p>
          </div>
        </div>
      </div>
    </section>

    <!-- CTA -->
    <section class="bg-secondary-50 dark:bg-secondary-900 py-12 sm:py-16">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
        <h2 class="text-2xl sm:text-3xl font-bold text-secondary-900 dark:text-white mb-4">Try the Dedicated 2FA Experience</h2>
        <p class="text-secondary-600 dark:text-secondary-400 mb-8 max-w-2xl mx-auto">
          Purpose-built for 2FA. Free on every platform. No account, no subscription, no compromise.
          TwoFac pairs perfectly with any password manager — including Bitwarden.
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
