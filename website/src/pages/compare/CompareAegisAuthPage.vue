<script setup lang="ts">
import { useSEO } from '../../composables/useSEO'
import ComparePageLayout from '../../components/ComparePageLayout.vue'
import type { GlanceStat, WhyChooseCard, FAQEntry, FeatureRow } from '../../components/ComparePageLayout.vue'

useSEO({
  title: 'TwoFac vs Aegis Authenticator: Best Open Source 2FA Alternative (2026)',
  description: 'Detailed comparison of TwoFac and Aegis Authenticator. Both are open source and privacy-focused, but TwoFac adds iOS, desktop, web, CLI, browser extensions, and wearable apps beyond Aegis\'s Android-only vault.',
  canonicalPath: '/compare/aegis-authenticator',
})

const heroDescription = `Aegis Authenticator is one of the best Android-only 2FA apps available: open source, locally encrypted,
and packed with import/export tools. TwoFac keeps the same privacy-first spirit while adding iOS, desktop, web,
browser extensions, CLI workflows, and wearable apps — plus end-to-end encrypted sync when you need more than one device.`

const glanceStats: [GlanceStat, GlanceStat, GlanceStat] = [
  { value: '11+', label: 'Platforms for TwoFac', note: 'Aegis is Android-only' },
  { value: 'E2E', label: 'Encrypted multi-device sync', note: 'Aegis relies on manual backup/restore' },
  { value: '⌚', label: 'Wearables + browser + CLI', note: 'Aegis stays mobile-only' },
]

const features: FeatureRow[] = [
  { name: 'Open Source', twofac: '✓', competitor: '✓' },
  { name: 'Android App', twofac: '✓', competitor: '✓' },
  { name: 'iOS App', twofac: '✓', competitor: '✗' },
  { name: 'Desktop App (macOS/Windows/Linux)', twofac: '✓', competitor: '✗' },
  { name: 'Web App (PWA)', twofac: '✓', competitor: '✗' },
  { name: 'Browser Extension', twofac: '✓', competitor: '✗' },
  { name: 'CLI Tool', twofac: '✓', competitor: '✗' },
  { name: 'Wear OS', twofac: '✓', competitor: '✗' },
  { name: 'watchOS', twofac: '✓', competitor: '✗' },
  { name: 'TOTP Support', twofac: '✓', competitor: '✓' },
  { name: 'HOTP Support', twofac: '✓', competitor: '✓' },
  { name: 'Encrypted Backups / Vault', twofac: '✓', competitor: '✓' },
  { name: 'Biometric Lock', twofac: '✓', competitor: '✓' },
  { name: 'Import / Export', twofac: '✓', competitor: '✓' },
  { name: 'End-to-End Encrypted Sync', twofac: '✓', competitor: '✗' },
  { name: 'No Account Required', twofac: '✓', competitor: '✓' },
  { name: 'Completely Free', twofac: '✓', competitor: '✓' },
]

const whyChooseCards: WhyChooseCard[] = [
  { icon: 'fa-solid fa-mobile-screen-button', title: 'Cross-Platform Instead of Android-Only', text: 'Aegis is outstanding on Android, but it stops there. TwoFac brings the same authenticator workflow to iPhone, desktop, browser, CLI, and smartwatches.' },
  { icon: 'fa-solid fa-arrows-rotate', title: 'Encrypted Sync When You Need It', text: 'Aegis intentionally avoids built-in sync, so moving between devices depends on manual backups. TwoFac keeps end-to-end encryption while letting your devices stay in sync automatically.' },
  { icon: 'fa-solid fa-terminal', title: 'Desktop and CLI Workflows', text: 'Generate codes on macOS, Windows, Linux, or directly in the terminal. Aegis has no desktop app, no browser extension, and no CLI for developer workflows.' },
  { icon: 'fa-solid fa-clock', title: 'Wearable Access', text: 'TwoFac includes both Wear OS and Apple Watch companions, so your codes are available on your wrist. Aegis requires pulling out your Android phone every time.' },
  { icon: 'fa-solid fa-puzzle-piece', title: 'Browser Autofill and PWA Access', text: 'Chrome and Firefox extensions plus a web PWA make TwoFac easier to reach during desktop logins. Aegis has no web or browser layer.' },
  { icon: 'fa-solid fa-shield-halved', title: 'Privacy Still Intact', text: 'Choosing TwoFac does not mean giving up the privacy values that make Aegis popular. TwoFac is open source, free, no-account by default, and uses end-to-end encryption for sync and backups.' },
]

const faqs: FAQEntry[] = [
  { question: 'Is Aegis Authenticator still a great app?', answer: 'Absolutely. Aegis is one of the strongest Android authenticators available: open source, locally encrypted, biometric-protected, and rich in import/export options. TwoFac becomes the better choice when you need those same values across more than just Android.' },
  { question: 'Does Aegis sync across devices?', answer: 'Not natively. Aegis relies on encrypted backups and manual restore workflows. That local-first model is great for some users, but if you want the same vault on phones, desktops, browsers, and watches, TwoFac is dramatically more convenient.' },
  { question: 'Can I use Aegis on iPhone or desktop?', answer: 'No. The Aegis maintainers explicitly position it as an Android app and do not plan ports to iOS, Windows, macOS, browser extensions, or other platforms. TwoFac covers all of those form factors.' },
  { question: 'Does TwoFac give up security compared with Aegis?', answer: 'No. Both are open source and both protect your secrets with encryption and biometric unlock. The big difference is that TwoFac adds end-to-end encrypted sync and many more clients, while Aegis stays strictly local-first.' },
  { question: 'Can I move from Aegis to TwoFac without re-registering every account?', answer: 'Usually, yes. Aegis can export a plain-text URI list using standard otpauth:// entries, and TwoFac can import that style of export. Just make sure you export a plain-text URI list rather than an encrypted Aegis vault backup.' },
]
</script>

<template>
  <ComparePageLayout
    competitor-name="Aegis Authenticator"
    :hero-description="heroDescription"
    :glance-stats="glanceStats"
    comparison-subtitle="Aegis is excellent for Android-only local vaults. TwoFac is built for people who need that experience everywhere."
    :features="features"
    :why-choose-cards="whyChooseCards"
    :faqs="faqs"
    cta-heading="Want Aegis-Level Privacy on Every Device?"
    cta-body="If you like Aegis because it's open source, secure, and tracker-free, TwoFac delivers that same philosophy across phones, desktops, browsers, terminals, and wearables."
    guide-link="/migrate/aegis-authenticator"
    guide-link-text="Migration Guide →"
  >
    <template #intro>
      <section class="bg-secondary-50 dark:bg-secondary-900 py-12 sm:py-16">
        <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
          <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-4">What Is Aegis Authenticator?</h2>
          <div class="space-y-4">
            <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
              Aegis Authenticator is a free, open-source 2FA app for Android focused on doing one job extremely well: keeping your
              TOTP and HOTP tokens in a locally encrypted vault. Its vault uses strong encryption, supports biometric unlock, blocks
              screenshots, and offers one of the best import/export toolsets in the Android authenticator ecosystem.
            </p>
            <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
              Aegis also includes features many authenticators still miss: automatic backups to a folder of your choice, custom icons,
              grouping, search, QR/image import, and broad compatibility with exports from other apps. For Android-only users who
              prefer a strictly local workflow, it's an excellent choice.
            </p>
            <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
              Where Aegis falls short is platform reach. There is no iPhone app, no desktop companion, no web client, no browser
              extension, and no smartwatch support. TwoFac picks up exactly where Aegis stops.
            </p>
          </div>
        </div>
      </section>
    </template>

    <template #details>
      <section class="bg-secondary-50 dark:bg-secondary-900 py-12 sm:py-16">
        <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
          <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-4">Platform Coverage: Android Specialist vs Full Ecosystem</h2>
          <div class="space-y-4">
            <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
              Aegis is unapologetically Android-first. That focus helps it feel polished on Android, but it also means your vault
              stays trapped on one platform unless you manually shuttle backup files around.
            </p>
            <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
              TwoFac gives you the same authenticator experience on <strong class="text-secondary-900 dark:text-white">11+ platforms</strong>:
              Android, iPhone, macOS, Windows, Linux, web, browser extensions, CLI, Wear OS, and Apple Watch. If you log in from
              more than one device type, this difference is enormous.
            </p>
          </div>
        </div>
      </section>

      <section class="py-12 sm:py-16">
        <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
          <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-4">Security Philosophy: Local Vault vs Optional Encrypted Sync</h2>
          <div class="space-y-4">
            <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
              Aegis wins a lot of trust because it is simple and local-first: your secrets live in an encrypted Android vault, and
              backups happen only where you point them. No account, no analytics, no mandatory cloud.
            </p>
            <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
              TwoFac shares the same privacy posture by default, but adds <strong class="text-secondary-900 dark:text-white">end-to-end
              encrypted sync</strong> for people who need multiple devices. You keep the security properties of encrypted storage while
              avoiding the pain of manual export and restore every time you add a new device.
            </p>
          </div>
        </div>
      </section>

      <section class="bg-secondary-50 dark:bg-secondary-900 py-12 sm:py-16">
        <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
          <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-4">Why Power Users Outgrow Aegis</h2>
          <div class="space-y-4">
            <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
              Aegis gives Android users excellent vault management, but it cannot help when you need a code on your Linux desktop,
              inside a browser login form, on a remote SSH session, or from your smartwatch while your phone is charging.
            </p>
            <p class="text-secondary-600 dark:text-secondary-400 leading-relaxed">
              That's where TwoFac stands apart: browser autofill, a real CLI, native desktop apps, wearable clients, and a PWA all
              work together as one authenticator instead of forcing you back to your phone for every login.
            </p>
          </div>
        </div>
      </section>
    </template>
  </ComparePageLayout>
</template>
