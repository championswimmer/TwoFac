<script setup lang="ts">
import { useSEO } from '../../composables/useSEO'
import ThreeWayComparePageLayout from '../../components/ThreeWayComparePageLayout.vue'
import type { DecisionCard, ThreeWayFAQEntry, ThreeWayFeatureRow, ThreeWayGlanceStat } from '../../components/ThreeWayComparePageLayout.vue'

const canonicalPath = '/compare/ente-auth-vs-2fas-vs-twofac'

useSEO({
  title: 'Ente vs 2FAS vs TwoFac: Best Open Source 2FA App (2026)',
  description: 'Compare Ente Auth, 2FAS, and TwoFac across platforms, sync, browser support, CLI access, wearables, pricing, and migration options.',
  canonicalPath,
})

const heroDescription = `Ente Auth, 2FAS, and TwoFac are all strong open-source authenticator choices. This three-way comparison helps you decide whether you want Ente's encrypted app family, 2FAS's mobile-first simplicity, or TwoFac's broader desktop, browser, CLI, web, and wearable coverage.`

const glanceStats: [ThreeWayGlanceStat, ThreeWayGlanceStat, ThreeWayGlanceStat] = [
  { value: '3', label: 'Open-source authenticators', note: 'Each app has a privacy-first foundation' },
  { value: '11+', label: 'TwoFac platforms', note: 'Mobile, desktop, web, browser, CLI, and watches' },
  { value: '$0', label: 'TwoFac feature tier', note: 'No paid plan for sync or advanced surfaces' },
]

const features: ThreeWayFeatureRow[] = [
  { name: 'Open Source', appOne: 'Yes', appTwo: 'Yes', twofac: 'Yes' },
  { name: 'Android App', appOne: 'Yes', appTwo: 'Yes', twofac: 'Yes' },
  { name: 'iOS App', appOne: 'Yes', appTwo: 'Yes', twofac: 'Yes' },
  { name: 'Desktop App', appOne: 'Yes', appTwo: 'No standalone app', twofac: 'Yes' },
  { name: 'Web App / PWA', appOne: 'Yes', appTwo: 'No', twofac: 'Yes' },
  { name: 'Browser Extension', appOne: 'No', appTwo: 'Yes', twofac: 'Yes' },
  { name: 'CLI for Code Generation', appOne: 'No interactive CLI', appTwo: 'No', twofac: 'Yes' },
  { name: 'Wear OS', appOne: 'No', appTwo: 'No', twofac: 'Yes' },
  { name: 'Apple Watch', appOne: 'No', appTwo: 'Yes', twofac: 'Yes' },
  { name: 'TOTP / HOTP', appOne: 'Yes', appTwo: 'Yes', twofac: 'Yes' },
  { name: 'Encrypted Sync', appOne: 'Yes, with Ente account', appTwo: 'Paid cross-platform', twofac: 'Yes' },
  { name: 'No Account Required', appOne: 'Local-only mode', appTwo: 'Yes', twofac: 'Yes' },
  { name: 'Biometric Lock', appOne: 'Limited', appTwo: 'Yes', twofac: 'Yes' },
  { name: 'Completely Free', appOne: 'Free tier', appTwo: 'Free core / paid Pass', twofac: 'Yes' },
]

const decisionCards: DecisionCard[] = [
  { icon: 'fa-solid fa-lock', title: 'Choose Ente for Ente sync', text: 'Ente Auth is a good fit if you already use Ente services and want an encrypted app family across phone, desktop, and web.' },
  { icon: 'fa-solid fa-mobile-screen', title: 'Choose 2FAS for mobile simplicity', text: '2FAS is strongest when your authenticator workflow is mostly mobile plus a paired browser extension.' },
  { icon: 'fa-solid fa-layer-group', title: 'Choose TwoFac for platform breadth', text: 'TwoFac reaches mobile, desktop, web, browser extensions, CLI workflows, Wear OS, and Apple Watch from one open-source project.' },
  { icon: 'fa-solid fa-terminal', title: 'TwoFac fits developer workflows', text: 'If terminal access, scripting, and independent desktop clients matter, TwoFac covers workflows Ente Auth and 2FAS do not.' },
  { icon: 'fa-solid fa-gift', title: 'No premium gate', text: 'TwoFac keeps sync and advanced surfaces free, while 2FAS places some cross-platform convenience behind 2FAS Pass.' },
  { icon: 'fa-solid fa-arrows-rotate', title: 'Migration is straightforward', text: 'TwoFac has migration paths for both Ente Auth and 2FAS, so testing it does not require rebuilding your vault manually.' },
]

const faqs: ThreeWayFAQEntry[] = [
  { question: 'Is TwoFac more private than Ente Auth or 2FAS?', answer: 'All three apps are privacy-focused and open source. TwoFac stands out less by claiming a different privacy philosophy and more by combining that privacy posture with more client surfaces.' },
  { question: 'Which app is best if I need desktop access?', answer: 'Ente Auth and TwoFac both offer desktop access, while 2FAS does not have a standalone desktop app. TwoFac adds browser extensions, a web PWA, and CLI access on top of desktop apps.' },
  { question: 'Which one is best for developers?', answer: 'TwoFac is the strongest developer option because it includes CLI workflows and browser autofill alongside desktop and mobile apps.' },
  { question: 'Can I migrate from Ente Auth or 2FAS to TwoFac?', answer: 'Yes. TwoFac includes migration flows for both Ente Auth and 2FAS exports, making it practical to evaluate without starting from scratch.' },
]
</script>

<template>
  <ThreeWayComparePageLayout
    app-one-name="Ente Auth"
    app-two-name="2FAS"
    :canonical-path="canonicalPath"
    :hero-description="heroDescription"
    :glance-stats="glanceStats"
    :features="features"
    :decision-cards="decisionCards"
    :faqs="faqs"
    cta-heading="Want One Authenticator Everywhere?"
    cta-body="TwoFac gives you the open-source foundation you expect from Ente Auth and 2FAS, then adds desktop, browser, CLI, web, and wearable workflows without a premium tier."
  >
    <template #intro>
      <section class="bg-secondary-50 dark:bg-secondary-900 py-12 sm:py-16">
        <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
          <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-4">Quick Summary</h2>
          <div class="space-y-4 text-secondary-600 dark:text-secondary-400 leading-relaxed">
            <p>
              Ente Auth is best known for encrypted sync and a broad app family, while 2FAS is known for a clean mobile-first experience and browser extension pairing.
            </p>
            <p>
              TwoFac competes with both by keeping the open-source and encrypted-authenticator foundation while covering more places you might need a 2FA code: native desktop apps, browser extensions, a web PWA, a CLI, Wear OS, and Apple Watch.
            </p>
          </div>
        </div>
      </section>
    </template>

    <template #details>
      <section class="py-12 sm:py-16">
        <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
          <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-4">The Main Difference Is Reach</h2>
          <div class="space-y-4 text-secondary-600 dark:text-secondary-400 leading-relaxed">
            <p>
              If you only need phone-based codes, all three apps can serve you well. The gap appears when your daily login workflow spans desktops, browsers, terminals, and wearables.
            </p>
            <p>
              TwoFac is built for that wider workflow. It avoids treating the phone as the only primary device and gives every supported surface a first-class path to your vault.
            </p>
          </div>
        </div>
      </section>
    </template>
  </ThreeWayComparePageLayout>
</template>
