<script setup lang="ts">
import { useSEO } from '../../composables/useSEO'
import ComparePageLayout from '../../components/ComparePageLayout.vue'
import type { GlanceStat, WhyChooseCard, FAQEntry, FeatureRow } from '../../components/ComparePageLayout.vue'

useSEO({
  title: 'TwoFac vs Proton Authenticator: Best Cross-Platform 2FA Alternative (2026)',
  description: 'Detailed comparison of TwoFac and Proton Authenticator. Both are open source and end-to-end encrypted, but TwoFac adds a browser extension, web app, CLI, and wearable support that Proton Authenticator lacks.',
  canonicalPath: '/compare/proton-authenticator',
})

const heroDescription = `Proton Authenticator is a strong privacy-focused 2FA app with end-to-end encrypted sync across mobile and desktop.
TwoFac matches that open-source, encrypted foundation while going further with a web PWA, browser extensions, a real CLI,
and smartwatch apps for Wear OS and Apple Watch.`

const glanceStats: [GlanceStat, GlanceStat, GlanceStat] = [
  { value: 'Both', label: 'Open source + encrypted sync', note: 'The core security story is strong on both sides' },
  { value: 'Extra', label: 'Web, browser, CLI, wearables', note: 'TwoFac covers more endpoints than Proton' },
  { value: '11+', label: 'Platforms for TwoFac', note: 'Proton stops at mobile + desktop' },
]

const features: FeatureRow[] = [
  { name: 'Open Source', twofac: '✓', competitor: '✓' },
  { name: 'Android App', twofac: '✓', competitor: '✓' },
  { name: 'iOS App', twofac: '✓', competitor: '✓' },
  { name: 'Desktop App (macOS/Windows/Linux)', twofac: '✓', competitor: '✓' },
  { name: 'Web App (PWA)', twofac: '✓', competitor: '✗' },
  { name: 'Browser Extension', twofac: '✓', competitor: '✗' },
  { name: 'CLI Tool', twofac: '✓', competitor: '✗' },
  { name: 'Wear OS', twofac: '✓', competitor: '✗' },
  { name: 'watchOS', twofac: '✓', competitor: '✗' },
  { name: 'TOTP Support', twofac: '✓', competitor: '✓' },
  { name: 'End-to-End Encrypted Sync', twofac: '✓', competitor: '✓' },
  { name: 'Encrypted Backups', twofac: '✓', competitor: '✓' },
  { name: 'Biometric / PIN Lock', twofac: '✓', competitor: '✓' },
  { name: 'Offline Use', twofac: '✓', competitor: '✓' },
  { name: 'No Account Required', twofac: '✓', competitor: '✓ (local-only mode)' },
  { name: 'Import / Export', twofac: '✓', competitor: '✓' },
  { name: 'Kotlin Multiplatform (Native)', twofac: '✓', competitor: '✗' },
  { name: 'Completely Free', twofac: '✓', competitor: '✓' },
]

const whyChooseCards: WhyChooseCard[] = [
  { icon: 'fa-solid fa-puzzle-piece', title: 'Browser Extension Built In', text: 'Proton Authenticator has no standalone browser extension. TwoFac gives you Chrome and Firefox extensions so codes are right where you log in.' },
  { icon: 'fa-solid fa-globe', title: 'Web PWA Access', text: 'TwoFac works as a progressive web app on any modern browser. Proton Authenticator has desktop apps, but no standalone web client for quick access on unmanaged devices.' },
  { icon: 'fa-solid fa-terminal', title: 'CLI for Developer Workflows', text: 'If you spend time in SSH sessions, terminals, and scripts, TwoFac is in another league. Proton Authenticator has no official CLI.' },
  { icon: 'fa-solid fa-clock', title: 'Wearables Matter', text: 'TwoFac includes both Wear OS and Apple Watch apps. Proton Authenticator currently stops at desktop and mobile.' },
  { icon: 'fa-solid fa-layer-group', title: 'More Ways to Reach the Same Vault', text: 'Both apps sync securely, but TwoFac exposes that vault to more surfaces: browser, web, desktop, mobile, terminal, and watches — all without premium tiers.' },
  { icon: 'fa-solid fa-bolt', title: 'Native Kotlin Multiplatform Stack', text: 'TwoFac is built natively with Kotlin Multiplatform across shared logic and clients, giving it a cohesive app family without sacrificing platform-specific behavior.' },
]

const faqs: FAQEntry[] = [
  { question: 'Is Proton Authenticator a serious competitor?', answer: 'Yes. Proton Authenticator is one of the most credible modern 2FA apps because it is open source, privacy-focused, cross-platform, and offers end-to-end encrypted sync. TwoFac is better positioned when you want those same strengths plus browser, web, CLI, and wearable access.' },
  { question: 'Do I need a Proton account to use Proton Authenticator?', answer: 'No. Proton Authenticator can be used locally and offline without a Proton account. A Proton account is only needed if you want Proton-managed sync and backup features.' },
  { question: 'Which app is better for desktop users?', answer: 'Both have desktop coverage, but TwoFac is more flexible on desktop because it also adds browser extensions, a PWA, and a CLI. Proton Authenticator remains a GUI-only mobile-and-desktop app family.' },
  { question: 'Can TwoFac directly import a Proton Authenticator backup?', answer: 'Not today. Proton Authenticator backups are designed around Proton\'s own encrypted export/restore flow, so the safest migration path to TwoFac is to re-register each account or add TwoFac as a second authenticator where the service allows it.' },
  { question: 'Why would I pick TwoFac if Proton already does encrypted sync?', answer: 'Because TwoFac reaches more places. If you want 2FA codes in a browser popup, a CLI session, a PWA, or on your wrist, TwoFac offers a broader toolkit while keeping the same open-source and encrypted-sync story.' },
]
</script>

<template>
  <ComparePageLayout
    competitor-name="Proton Authenticator"
    :hero-description="heroDescription"
    :glance-stats="glanceStats"
    comparison-subtitle="Both apps are privacy-focused and modern. The difference is how many places you can actually use them."
    :features="features"
    :why-choose-cards="whyChooseCards"
    :faqs="faqs"
    cta-heading="Keep the Privacy, Add More Reach"
    cta-body="If Proton Authenticator appeals to you because it's open source, encrypted, and cross-platform, TwoFac gives you all of that plus browser, web, CLI, and wearable workflows."
    guide-link="/migrate/proton-authenticator"
    guide-link-text="Migration Guide →"
  >
    <template #intro>
      <section class="bg-secondary-50 dark:bg-secondary-900 py-12 sm:py-16">
        <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
          <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-4">What Is Proton Authenticator?</h2>
          <div class="space-y-4">
            <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
              Proton Authenticator is Proton's standalone 2FA app built around the same privacy principles as Proton Mail and Proton
              Drive. It is open source, works offline, supports app locking, and offers end-to-end encrypted sync across Android,
              iPhone, Windows, macOS, and Linux.
            </p>
            <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
              It is a much more modern option than legacy authenticators because it avoids ads, supports import/export, and lets you
              use it locally without creating an account. For users who mainly want a clean mobile + desktop authenticator with secure
              sync, Proton Authenticator is a compelling choice.
            </p>
            <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
              TwoFac differs by expanding the surface area: web, browser extensions, CLI tooling, and smartwatch apps are all first-class
              citizens. If your 2FA life extends beyond phone and desktop windows, TwoFac gives you more ways to get to the same vault.
            </p>
          </div>
        </div>
      </section>
    </template>

    <template #details>
      <section class="bg-secondary-50 dark:bg-secondary-900 py-12 sm:py-16">
        <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
          <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-4">Shared Strengths: Open Source, Private, Cross-Platform</h2>
          <div class="space-y-4">
            <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
              This is not a story of an outdated incumbent versus a modern challenger. Proton Authenticator is modern too: open source,
              encrypted, cross-platform, and designed to avoid vendor lock-in.
            </p>
            <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
              That makes the comparison more interesting. If your shortlist already includes Proton, you are clearly looking for a
              privacy-first authenticator. The decision then comes down to reach and workflow, not whether one app takes security
              seriously and the other doesn't.
            </p>
          </div>
        </div>
      </section>

      <section class="py-12 sm:py-16">
        <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
          <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-4">Where TwoFac Pulls Ahead</h2>
          <div class="space-y-4">
            <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
              Proton Authenticator has no browser extension, no PWA, no CLI, and no wearable apps. If you mostly live inside desktop
              windows, that's fine. But if you bounce between browsers, terminals, watches, and multiple types of devices, the missing
              surfaces become obvious very quickly.
            </p>
            <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
              TwoFac covers <strong class="text-secondary-900 dark:text-white">11+ platforms</strong> and includes dedicated clients for
              browser autofill, web access, command-line workflows, Wear OS, and Apple Watch. It turns your authenticator into an
              everywhere tool rather than a desktop/mobile pair.
            </p>
          </div>
        </div>
      </section>

      <section class="bg-secondary-50 dark:bg-secondary-900 py-12 sm:py-16">
        <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
          <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-4">Who Should Pick Which?</h2>
          <div class="space-y-4">
            <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
              Proton Authenticator is a great fit if you want a straightforward encrypted authenticator for mobile and desktop and you
              already trust Proton's privacy model.
            </p>
            <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
              TwoFac is the better fit if you want the same privacy posture but need more reach: browser autofill, terminal access,
              smartwatch companions, web installability, and a single toolchain that adapts to how you actually log in throughout the day.
            </p>
          </div>
        </div>
      </section>
    </template>
  </ComparePageLayout>
</template>
