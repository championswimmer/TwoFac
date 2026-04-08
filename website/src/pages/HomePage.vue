<script setup lang="ts">
import MainLayout from '../layouts/MainLayout.vue'
import HeroSection from '../components/HeroSection.vue'
import FeatureCard from '../components/FeatureCard.vue'
import PlatformBadge from '../components/PlatformBadge.vue'
import WaitlistSection from '../components/WaitlistSection.vue'
import { useSEO } from '../composables/useSEO'

useSEO({
  title: 'Open Source Cross-Platform 2FA',
  description:
    'TwoFac is a free, open-source two-factor authentication app for Android, iOS, macOS, Windows, Linux, Web, CLI, and wearables. Secure, private, and cross-platform.',
  canonicalPath: '/',
})

const features = [
  {
    icon: 'fa-solid fa-code-branch',
    title: 'Open Source',
    description:
      'Fully open source under MIT license. Audit the code, contribute, or fork it.',
  },
  {
    icon: 'fa-solid fa-shield-halved',
    title: 'End-to-End Encryption',
    description:
      'Your secrets are encrypted before they leave your device. Zero-knowledge architecture.',
  },
  {
    icon: 'fa-solid fa-mobile-screen-button',
    title: 'Cross-Platform',
    description:
      'Native apps for Android, iOS, macOS, Windows, Linux, Web, and CLI.',
  },
  {
    icon: 'fa-solid fa-clock',
    title: 'Wearable Support',
    description:
      'Apple Watch and Wear OS companion apps with offline sync.',
  },
  {
    icon: 'fa-solid fa-database',
    title: 'Encrypted Backups',
    description:
      'Export and import your accounts securely with passkey-encrypted backups.',
  },
  {
    icon: 'fa-solid fa-right-left',
    title: 'Easy Migration',
    description:
      'Import from Google Authenticator, Authy, 2FAS, and Ente Auth with one click.',
  },
]

const platforms = [
  { name: 'Android', icon: 'fa-brands fa-android', available: true },
  { name: 'iOS', icon: 'fa-brands fa-apple', available: true },
  { name: 'macOS', icon: 'fa-brands fa-apple', available: true },
  { name: 'Windows', icon: 'fa-brands fa-windows', available: true },
  { name: 'Linux', icon: 'fa-brands fa-linux', available: true },
  { name: 'Web', icon: 'fa-solid fa-globe', available: true },
  { name: 'CLI', icon: 'fa-solid fa-terminal', available: true },
  { name: 'Wear OS', icon: 'fa-solid fa-clock', available: true },
  { name: 'watchOS', icon: 'fa-solid fa-clock', available: true },
  { name: 'Chrome', icon: 'fa-brands fa-chrome', available: true },
  { name: 'Firefox', icon: 'fa-brands fa-firefox-browser', available: true },
]
</script>

<template>
  <MainLayout>
    <HeroSection />

    <!-- Features Grid -->
    <section class="py-16 sm:py-24 bg-white dark:bg-secondary-950">
      <div class="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <h2
          class="text-center text-3xl font-bold tracking-tight text-secondary-900 dark:text-white sm:text-4xl"
        >
          Why TwoFac?
        </h2>
        <p
          class="mx-auto mt-4 max-w-2xl text-center text-lg text-secondary-600 dark:text-secondary-400"
        >
          A modern 2FA app built with security, privacy, and convenience in mind.
        </p>

        <div class="mt-12 grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
          <FeatureCard
            v-for="feature in features"
            :key="feature.title"
            :icon="feature.icon"
            :title="feature.title"
            :description="feature.description"
          />
        </div>
      </div>
    </section>

    <!-- Platform Showcase -->
    <section class="py-16 sm:py-24 bg-secondary-50 dark:bg-secondary-900">
      <div class="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <h2
          class="text-center text-3xl font-bold tracking-tight text-secondary-900 dark:text-white sm:text-4xl"
        >
          Available Everywhere
        </h2>
        <p
          class="mx-auto mt-4 max-w-2xl text-center text-lg text-secondary-600 dark:text-secondary-400"
        >
          One app, every platform. Take your 2FA codes wherever you go.
        </p>

        <div class="mt-12 flex flex-wrap justify-center gap-4">
          <PlatformBadge
            v-for="platform in platforms"
            :key="platform.name"
            :name="platform.name"
            :icon="platform.icon"
            :available="platform.available"
          />
        </div>
      </div>
    </section>

    <!-- Waitlist Section -->
    <WaitlistSection />

    <!-- CTA Section -->
    <section class="py-16 sm:py-24 border-y border-secondary-200 bg-secondary-100 dark:border-secondary-800 dark:bg-secondary-900">
      <div class="mx-auto max-w-4xl px-4 text-center sm:px-6 lg:px-8">
        <h2 class="text-3xl font-bold tracking-tight text-secondary-900 dark:text-secondary-50 sm:text-4xl">
          Ready to take control of your 2FA?
        </h2>
        <p class="mx-auto mt-4 max-w-2xl text-lg text-secondary-700 dark:text-secondary-300">
          Download TwoFac for free and secure your accounts across all your devices.
        </p>
        <div class="mt-8 flex flex-col items-center justify-center gap-4 sm:flex-row">
          <router-link
            to="/download"
            class="inline-flex items-center rounded-lg border border-primary-700 bg-primary-700 px-6 py-3 text-base font-semibold text-secondary-50 hover:bg-primary-800 transition-colors dark:border-primary-500 dark:bg-primary-600 dark:hover:bg-primary-500"
          >
            Download TwoFac
          </router-link>
          <a
            href="https://github.com/championswimmer/TwoFac"
            target="_blank"
            rel="noopener noreferrer"
            class="inline-flex items-center rounded-lg border border-secondary-400 px-6 py-3 text-base font-semibold text-secondary-800 hover:bg-secondary-200 transition-colors dark:border-secondary-600 dark:text-secondary-200 dark:hover:bg-secondary-800"
          >
            View on GitHub
          </a>
        </div>
      </div>
    </section>
  </MainLayout>
</template>
