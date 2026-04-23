<script setup lang="ts">
import { useSEO } from '../../composables/useSEO'
import ComparePageLayout from '../../components/ComparePageLayout.vue'
import type { GlanceStat, WhyChooseCard, FAQEntry, FeatureRow } from '../../components/ComparePageLayout.vue'

useSEO({
  title: 'TwoFac vs Bitwarden Authenticator — Dedicated 2FA vs Password Manager Add-On (2025)',
  description: 'Detailed comparison of TwoFac and Bitwarden Authenticator. TwoFac is a purpose-built, lightweight 2FA app with desktop, CLI, and wearable support — no account or subscription required.',
  canonicalPath: '/compare/bitwarden',
})

const heroDescription = `Bitwarden offers 2FA through two paths: a free standalone mobile app and an integrated authenticator in the Bitwarden
Password Manager (premium feature). TwoFac is a purpose-built, dedicated 2FA app that works on 11+ platforms —
including desktop, CLI, browser extensions, and smartwatches — completely free with no account required.`

const glanceStats: [GlanceStat, GlanceStat, GlanceStat] = [
  { value: '$0', label: 'All features, forever free', note: 'Bitwarden PM TOTP needs $10/yr premium' },
  { value: '11+', label: 'Platforms supported', note: 'Bitwarden standalone: mobile only' },
  { value: '🎯', label: 'Purpose-built for 2FA', note: 'Not a password manager add-on' },
]

const features: FeatureRow[] = [
  { name: 'Standalone 2FA App', twofac: '✓', competitor: 'Partial (standalone + PM add-on)' },
  { name: 'Open Source', twofac: '✓', competitor: '✓' },
  { name: 'Android App', twofac: '✓', competitor: '✓' },
  { name: 'iOS App', twofac: '✓', competitor: '✓' },
  { name: 'Desktop App (macOS/Windows/Linux)', twofac: '✓', competitor: '✗ (standalone) / ✓ (via PM)' },
  { name: 'CLI Tool', twofac: '✓', competitor: '✗' },
  { name: 'Web App (PWA)', twofac: '✓', competitor: '✗ (standalone) / ✓ (via PM)' },
  { name: 'Browser Extension', twofac: '✓', competitor: '✗ (standalone) / ✓ (via PM)' },
  { name: 'Wear OS', twofac: '✓', competitor: '✗' },
  { name: 'watchOS', twofac: '✓', competitor: '✗' },
  { name: 'TOTP Support', twofac: '✓', competitor: '✓' },
  { name: 'HOTP Support', twofac: '✓', competitor: '✗' },
  { name: 'E2E Encrypted Sync', twofac: '✓', competitor: '✓ (via PM vault)' },
  { name: 'Biometric Lock', twofac: '✓', competitor: '✗' },
  { name: 'No Account Required', twofac: '✓', competitor: '✓ (standalone) / ✗ (PM)' },
  { name: 'Completely Free', twofac: '✓', competitor: 'Standalone free / PM TOTP: $10/yr' },
  { name: 'Lightweight', twofac: '✓', competitor: '✗ (PM is heavy)' },
  { name: 'Kotlin Multiplatform (Native)', twofac: '✓', competitor: '✗' },
]

const whyChooseCards: WhyChooseCard[] = [
  { icon: '🎯', title: 'Purpose-Built for 2FA', text: "TwoFac is a dedicated 2FA app — not a side feature of a password manager. It does one thing and does it exceptionally well across every platform." },
  { icon: '🆓', title: '100% Free, All Platforms', text: 'No premium tier, no subscription. Desktop, CLI, browser extension, and wearable access are free. Bitwarden charges $10/year for TOTP in the password manager.' },
  { icon: '🖥️', title: 'Native Desktop & CLI', text: "Standalone desktop apps for macOS, Windows, and Linux, plus a CLI tool for terminal workflows. Bitwarden's standalone authenticator is mobile-only." },
  { icon: '⌚', title: 'Wearable Apps', text: 'View codes on your Wear OS or Apple Watch. Neither Bitwarden product offers wearable companion apps.' },
  { icon: '🪶', title: 'Lightweight & Fast', text: 'No password vault, no credential management, no bloat. TwoFac starts fast, runs lean, and focuses entirely on generating and managing 2FA codes.' },
  { icon: '🔓', title: 'No Account Required', text: "Start using TwoFac immediately — no sign-up, no email verification. Bitwarden's full-featured authenticator requires a Bitwarden account and Premium subscription." },
]

const faqs: FAQEntry[] = [
  { question: 'Should I keep my 2FA separate from my password manager?', answer: 'Security experts generally recommend separation. If your password manager is compromised, having 2FA in a separate app means your second factor is still secure. TwoFac follows this principle — it\'s a dedicated 2FA app that complements any password manager (including Bitwarden).' },
  { question: "What's the difference between Bitwarden Authenticator and Bitwarden's built-in TOTP?", answer: "Bitwarden Authenticator is a separate free mobile app that generates TOTP codes independently. Bitwarden's built-in TOTP is a Premium feature ($10/year) inside the Password Manager that attaches codes to vault items for auto-copy after autofill. They can sync with each other, but serve different use cases." },
  { question: 'Can I use TwoFac alongside Bitwarden Password Manager?', answer: 'Absolutely. TwoFac and Bitwarden complement each other perfectly — Bitwarden manages your passwords, TwoFac manages your 2FA codes. This is actually the most secure setup: keeping your first factor (passwords) and second factor (TOTP codes) in separate applications.' },
  { question: 'Does the Bitwarden standalone authenticator have a desktop app?', answer: 'No. The Bitwarden Authenticator standalone app is mobile-only (Android and iOS). To access TOTP codes on desktop, you need the full Bitwarden Password Manager with a Premium subscription. TwoFac offers native desktop apps for macOS, Windows, and Linux at no cost.' },
  { question: 'Is the Bitwarden standalone authenticator actually free?', answer: 'The standalone mobile app is free for basic TOTP generation. However, it\'s limited to mobile with no desktop or CLI support. For the full experience (desktop, browser, autofill), you need Bitwarden Premium at $10/year. TwoFac provides the full experience across all platforms for free.' },
]
</script>

<template>
  <ComparePageLayout
    competitor-name="Bitwarden Authenticator"
    :hero-description="heroDescription"
    :glance-stats="glanceStats"
    comparison-subtitle="Comparing TwoFac against the Bitwarden standalone authenticator app and noting where the PM integration differs."
    :features="features"
    :why-choose-cards="whyChooseCards"
    :faqs="faqs"
    cta-heading="Try the Dedicated 2FA Experience"
    cta-body="Purpose-built for 2FA. Free on every platform. No account, no subscription, no compromise. TwoFac pairs perfectly with any password manager — including Bitwarden."
    guide-link="/migrate/bitwarden"
    guide-link-text="Migration Guide →"
  >
    <template #intro>
      <section class="bg-secondary-50 dark:bg-secondary-900 py-12 sm:py-16">
        <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
          <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-4">What Is Bitwarden Authenticator?</h2>
          <div class="space-y-4">
            <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
              Bitwarden offers two distinct 2FA solutions. The <strong class="text-secondary-900 dark:text-white">Bitwarden Authenticator</strong>
              is a free, standalone mobile app (Android and iOS) that generates TOTP codes independently — no Bitwarden account required.
            </p>
            <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
              The <strong class="text-secondary-900 dark:text-white">integrated authenticator</strong> lives inside the Bitwarden Password Manager
              and attaches TOTP codes directly to vault login items. This requires a Premium subscription ($10/year) or an organization plan.
            </p>
            <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
              Both Bitwarden products are open source. However, the standalone authenticator is mobile-only — no native desktop app, no CLI,
              no web interface, and no wearable support. The integrated authenticator is more fully featured but comes at a cost and requires a Bitwarden account.
            </p>
          </div>
        </div>
      </section>
    </template>

    <template #details>
      <section class="bg-secondary-50 dark:bg-secondary-900 py-12 sm:py-16">
        <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
          <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-4">Purpose-Built 2FA vs Password Manager Add-On</h2>
          <div class="space-y-4">
            <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
              The fundamental question is whether your 2FA should live inside your password manager or in a separate, dedicated app.
              Security best practice generally recommends keeping them separate — if your password manager is compromised, your 2FA
              codes shouldn't go down with it.
            </p>
            <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
              <strong class="text-secondary-900 dark:text-white">TwoFac is purpose-built for 2FA.</strong> It's not a side feature
              of a password manager — it's a fully-featured, cross-platform authenticator that treats every platform as a first-class citizen.
            </p>
          </div>
        </div>
      </section>

      <section class="py-12 sm:py-16">
        <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
          <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-4">Platform Coverage: The Bitwarden Confusion</h2>
          <div class="space-y-4">
            <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
              The <strong class="text-secondary-900 dark:text-white">standalone Bitwarden Authenticator</strong> only runs on Android and iOS —
              no desktop, no web, no CLI, no browser extension, no wearable. For desktop TOTP access you need Bitwarden Premium.
            </p>
            <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
              With TwoFac, there's no confusion. <strong class="text-secondary-900 dark:text-white">One app, 11+ platforms, all free.</strong>
              Android, iOS, macOS, Windows, Linux, web PWA, CLI, browser extension, Wear OS, and Apple Watch — no premium gates, no subscription tiers.
            </p>
          </div>
        </div>
      </section>

      <section class="bg-secondary-50 dark:bg-secondary-900 py-12 sm:py-16">
        <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
          <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-4">Pricing: Free vs Freemium</h2>
          <div class="space-y-4">
            <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
              The Bitwarden standalone authenticator is free for basic TOTP on mobile. The integrated authenticator with desktop access
              requires a Bitwarden Premium subscription at $10/year. Families plans are $40/year, and business plans start at $4/user/month.
            </p>
            <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
              TwoFac is <strong class="text-secondary-900 dark:text-white">completely free with no tiers</strong>. Every feature —
              desktop apps, CLI, browser extensions, wearable apps, encrypted sync, and biometric lock — is available at no cost.
            </p>
          </div>
        </div>
      </section>
    </template>
  </ComparePageLayout>
</template>
