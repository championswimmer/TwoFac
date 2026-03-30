<script setup lang="ts">
import MainLayout from '../layouts/MainLayout.vue'
import HeroSection from '../components/HeroSection.vue'
import FeatureCard from '../components/FeatureCard.vue'
import PlatformBadge from '../components/PlatformBadge.vue'
import { useSEO } from '../composables/useSEO'

useSEO({
  title: 'Open Source Cross-Platform 2FA',
  description:
    'TwoFac is a free, open-source two-factor authentication app for Android, iOS, macOS, Windows, Linux, Web, CLI, and wearables. Secure, private, and cross-platform.',
  canonicalPath: '/',
})

const features = [
  {
    icon: '🔓',
    title: 'Open Source',
    description:
      'Fully open source under MIT license. Audit the code, contribute, or fork it.',
  },
  {
    icon: '🔐',
    title: 'End-to-End Encryption',
    description:
      'Your secrets are encrypted before they leave your device. Zero-knowledge architecture.',
  },
  {
    icon: '📱',
    title: 'Cross-Platform',
    description:
      'Native apps for Android, iOS, macOS, Windows, Linux, Web, and CLI.',
  },
  {
    icon: '⌚',
    title: 'Wearable Support',
    description:
      'Apple Watch and Wear OS companion apps with offline sync.',
  },
  {
    icon: '💾',
    title: 'Encrypted Backups',
    description:
      'Export and import your accounts securely with passkey-encrypted backups.',
  },
  {
    icon: '🔄',
    title: 'Easy Migration',
    description:
      'Import from Google Authenticator, Authy, 2FAS, and Ente Auth with one click.',
  },
]

const platforms = [
  { name: 'Android', icon: '🤖', available: true },
  { name: 'iOS', icon: '🍎', available: true },
  { name: 'macOS', icon: '🖥️', available: true },
  { name: 'Windows', icon: '🪟', available: true },
  { name: 'Linux', icon: '🐧', available: true },
  { name: 'Web', icon: '🌐', available: true },
  { name: 'CLI', icon: '⌨️', available: true },
  { name: 'Wear OS', icon: '⌚', available: true },
  { name: 'watchOS', icon: '⌚', available: true },
  { name: 'Chrome', icon: '🧩', available: true },
  { name: 'Firefox', icon: '🦊', available: true },
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

    <!-- CTA Section -->
    <section class="py-16 sm:py-24 bg-primary-600 dark:bg-primary-800">
      <div class="mx-auto max-w-4xl px-4 text-center sm:px-6 lg:px-8">
        <h2 class="text-3xl font-bold tracking-tight text-white sm:text-4xl">
          Ready to take control of your 2FA?
        </h2>
        <p class="mx-auto mt-4 max-w-2xl text-lg text-primary-100">
          Download TwoFac for free and secure your accounts across all your devices.
        </p>
        <div class="mt-8 flex flex-col items-center justify-center gap-4 sm:flex-row">
          <router-link
            to="/download"
            class="inline-flex items-center rounded-lg bg-white px-6 py-3 text-base font-semibold text-primary-700 shadow-sm hover:bg-primary-50 transition-colors"
          >
            Download TwoFac
          </router-link>
          <a
            href="https://github.com/aspect-build/twofac"
            target="_blank"
            rel="noopener noreferrer"
            class="inline-flex items-center rounded-lg border-2 border-white px-6 py-3 text-base font-semibold text-white hover:bg-white/10 transition-colors"
          >
            View on GitHub
          </a>
        </div>
      </div>
    </section>
  </MainLayout>
</template>
