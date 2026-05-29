<script setup lang="ts">
import { useSEO } from '../../composables/useSEO'
import ThreeWayComparePageLayout from '../../components/ThreeWayComparePageLayout.vue'
import type { DecisionCard, ThreeWayFAQEntry, ThreeWayFeatureRow, ThreeWayGlanceStat } from '../../components/ThreeWayComparePageLayout.vue'

const canonicalPath = '/compare/2fas-vs-proton-authenticator-vs-twofac'

useSEO({
  title: '2FAS vs Proton Authenticator vs TwoFac: 2FA App Comparison (2026)',
  description: 'Compare 2FAS, Proton Authenticator, and TwoFac across mobile, desktop, browser extension, web, CLI, wearable, sync, and pricing features.',
  canonicalPath,
})

const heroDescription = `2FAS, Proton Authenticator, and TwoFac represent three different approaches to modern two-factor authentication: mobile-first simplicity, privacy ecosystem integration, and broad open-source access across phones, desktops, browsers, web, terminals, and watches.`

const glanceStats: [ThreeWayGlanceStat, ThreeWayGlanceStat, ThreeWayGlanceStat] = [
  { value: 'Mobile', label: '2FAS strength', note: 'Clean phone-first authenticator workflow' },
  { value: 'Privacy', label: 'Proton strength', note: 'Encrypted sync backed by Proton' },
  { value: 'Everywhere', label: 'TwoFac strength', note: 'Desktop, browser, web, CLI, and watches' },
]

const features: ThreeWayFeatureRow[] = [
  { name: 'Open Source', appOne: 'Yes', appTwo: 'Yes', twofac: 'Yes' },
  { name: 'Android App', appOne: 'Yes', appTwo: 'Yes', twofac: 'Yes' },
  { name: 'iOS App', appOne: 'Yes', appTwo: 'Yes', twofac: 'Yes' },
  { name: 'Desktop App', appOne: 'No standalone app', appTwo: 'Yes', twofac: 'Yes' },
  { name: 'Web App / PWA', appOne: 'No', appTwo: 'No', twofac: 'Yes' },
  { name: 'Browser Extension', appOne: 'Yes', appTwo: 'No', twofac: 'Yes' },
  { name: 'CLI for Code Generation', appOne: 'No', appTwo: 'No', twofac: 'Yes' },
  { name: 'Wear OS', appOne: 'No', appTwo: 'No', twofac: 'Yes' },
  { name: 'Apple Watch', appOne: 'Yes', appTwo: 'No', twofac: 'Yes' },
  { name: 'TOTP / HOTP', appOne: 'Yes', appTwo: 'TOTP', twofac: 'Yes' },
  { name: 'Encrypted Sync', appOne: 'Paid cross-platform', appTwo: 'Yes', twofac: 'Yes' },
  { name: 'No Account Required', appOne: 'Yes', appTwo: 'Local-only mode', twofac: 'Yes' },
  { name: 'Biometric / PIN Lock', appOne: 'Yes', appTwo: 'Yes', twofac: 'Yes' },
  { name: 'Completely Free', appOne: 'Free core / paid Pass', appTwo: 'Yes', twofac: 'Yes' },
]

const decisionCards: DecisionCard[] = [
  { icon: 'fa-solid fa-mobile-screen', title: '2FAS is mobile-first', text: '2FAS is a good option if your primary requirement is a polished phone app with browser extension pairing.' },
  { icon: 'fa-solid fa-shield-halved', title: 'Proton is ecosystem-first', text: 'Proton Authenticator is a good fit if you already trust Proton and want encrypted mobile plus desktop coverage.' },
  { icon: 'fa-solid fa-layer-group', title: 'TwoFac is workflow-first', text: 'TwoFac is built for users who move between phone, desktop, browser, web, terminal, and wearable contexts.' },
  { icon: 'fa-solid fa-desktop', title: 'Desktop without phone pairing', text: 'TwoFac and Proton both have desktop apps, but TwoFac also pairs desktop with browser, PWA, and CLI access.' },
  { icon: 'fa-solid fa-gift', title: 'No paid sync tier', text: 'TwoFac keeps cross-platform reach free instead of limiting advanced sync and convenience behind a paid plan.' },
  { icon: 'fa-solid fa-clock', title: 'Wearable coverage', text: 'TwoFac covers both Wear OS and Apple Watch, while 2FAS covers Apple Watch and Proton does not currently focus on wearables.' },
]

const faqs: ThreeWayFAQEntry[] = [
  { question: 'Should I choose 2FAS or Proton Authenticator instead of TwoFac?', answer: 'Choose 2FAS if you want a simple mobile-first authenticator, or Proton if you want a Proton-backed privacy app. Choose TwoFac if your workflow needs more access surfaces without a paid tier.' },
  { question: 'Which has the best browser workflow?', answer: '2FAS and TwoFac both support browser extension workflows. TwoFac adds standalone desktop, web PWA, CLI, and wearable access around that browser experience.' },
  { question: 'Which has the best desktop workflow?', answer: 'Proton Authenticator and TwoFac both offer desktop apps. TwoFac is broader for desktop users because it also provides browser extensions, a PWA, and CLI tooling.' },
  { question: 'Can TwoFac import from 2FAS and Proton Authenticator?', answer: 'Yes. TwoFac supports import flows for both 2FAS and Proton Authenticator exports.' },
]
</script>

<template>
  <ThreeWayComparePageLayout
    app-one-name="2FAS"
    app-two-name="Proton Authenticator"
    :canonical-path="canonicalPath"
    :hero-description="heroDescription"
    :glance-stats="glanceStats"
    :features="features"
    :decision-cards="decisionCards"
    :faqs="faqs"
    cta-heading="Choose the Authenticator That Follows Your Workflow"
    cta-body="TwoFac combines the openness, privacy, and convenience users expect from modern authenticators with a wider set of places to securely access your codes."
  >
    <template #intro>
      <section class="bg-secondary-50 dark:bg-secondary-900 py-12 sm:py-16">
        <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
          <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-4">Quick Summary</h2>
          <div class="space-y-4 text-secondary-600 dark:text-secondary-400 leading-relaxed">
            <p>
              2FAS is strongest as a phone-first authenticator with browser pairing. Proton Authenticator is strongest as a privacy-focused mobile and desktop option backed by Proton.
            </p>
            <p>
              TwoFac is strongest when you want a single open-source authenticator that is useful across more surfaces: mobile, desktop, browser extension, web PWA, CLI, Wear OS, and Apple Watch.
            </p>
          </div>
        </div>
      </section>
    </template>

    <template #details>
      <section class="py-12 sm:py-16">
        <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
          <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-4">Mobile App, Ecosystem App, or Everywhere App?</h2>
          <div class="space-y-4 text-secondary-600 dark:text-secondary-400 leading-relaxed">
            <p>
              The best choice depends on what role your authenticator plays. If it only needs to live on your phone, 2FAS is a capable option. If you want Proton-managed sync and desktop clients, Proton Authenticator is compelling.
            </p>
            <p>
              If your 2FA codes need to follow you across browsers, terminal sessions, desktop windows, web installs, and watches, TwoFac is designed around that broader everyday reality.
            </p>
          </div>
        </div>
      </section>
    </template>
  </ThreeWayComparePageLayout>
</template>
