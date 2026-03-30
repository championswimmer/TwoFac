<script setup lang="ts">
import MainLayout from '../layouts/MainLayout.vue'
import { useSEO } from '../composables/useSEO'

useSEO({
  title: 'Download',
  description:
    'Download TwoFac for Android, iOS, macOS, Windows, Linux, Web, CLI, and wearables. Free, open-source two-factor authentication for every platform.',
  canonicalPath: '/download',
})

interface DownloadCard {
  icon: string
  name: string
  description: string
  link: string
  linkText: string
  external?: boolean
}

const categories: { title: string; cards: DownloadCard[] }[] = [
  {
    title: '📱 Mobile',
    cards: [
      {
        icon: '🤖',
        name: 'Android',
        description: 'Full-featured 2FA app with biometric lock and Wear OS companion.',
        link: '#',
        linkText: 'Google Play',
        external: true,
      },
      {
        icon: '🍎',
        name: 'iOS',
        description: 'Native iOS app with Face ID / Touch ID and watchOS companion.',
        link: '#',
        linkText: 'App Store',
        external: true,
      },
    ],
  },
  {
    title: '🖥️ Desktop',
    cards: [
      {
        icon: '🍎',
        name: 'macOS',
        description: 'Native desktop app packaged as a DMG.',
        link: 'https://github.com/aspect-build/twofac/releases',
        linkText: 'Download DMG',
        external: true,
      },
      {
        icon: '🪟',
        name: 'Windows',
        description: 'Windows installer packaged as an MSI.',
        link: 'https://github.com/aspect-build/twofac/releases',
        linkText: 'Download MSI',
        external: true,
      },
      {
        icon: '🐧',
        name: 'Linux',
        description: 'Debian package for Ubuntu and other distributions.',
        link: 'https://github.com/aspect-build/twofac/releases',
        linkText: 'Download DEB',
        external: true,
      },
    ],
  },
  {
    title: '🌐 Web',
    cards: [
      {
        icon: '🌐',
        name: 'PWA',
        description: 'Install TwoFac as a Progressive Web App in any modern browser.',
        link: 'https://web.twofac.app',
        linkText: 'Open Web App',
        external: true,
      },
      {
        icon: '🧩',
        name: 'Chrome Extension',
        description: 'Access your 2FA codes directly from the Chrome toolbar.',
        link: '#',
        linkText: 'Chrome Web Store',
        external: true,
      },
      {
        icon: '🦊',
        name: 'Firefox Extension',
        description: 'Access your 2FA codes directly from the Firefox toolbar.',
        link: '#',
        linkText: 'Firefox Add-ons',
        external: true,
      },
    ],
  },
  {
    title: '⌨️ CLI',
    cards: [
      {
        icon: '🍎',
        name: 'macOS CLI',
        description: 'Native CLI binary for macOS (arm64 and x86_64).',
        link: 'https://github.com/aspect-build/twofac/releases',
        linkText: 'GitHub Releases',
        external: true,
      },
      {
        icon: '🪟',
        name: 'Windows CLI',
        description: 'Native CLI binary for Windows.',
        link: 'https://github.com/aspect-build/twofac/releases',
        linkText: 'GitHub Releases',
        external: true,
      },
      {
        icon: '🐧',
        name: 'Linux CLI',
        description: 'Native CLI binary for Linux (x86_64).',
        link: 'https://github.com/aspect-build/twofac/releases',
        linkText: 'GitHub Releases',
        external: true,
      },
    ],
  },
  {
    title: '⌚ Wearables',
    cards: [
      {
        icon: '⌚',
        name: 'Wear OS',
        description: 'Bundled with the Android app. Syncs codes automatically.',
        link: '#',
        linkText: 'Included with Android',
      },
      {
        icon: '⌚',
        name: 'watchOS',
        description: 'Bundled with the iOS app. Syncs codes automatically.',
        link: '#',
        linkText: 'Included with iOS',
      },
    ],
  },
]
</script>

<template>
  <MainLayout>
    <!-- Header -->
    <section class="bg-primary-600 dark:bg-primary-800 py-16 sm:py-24">
      <div class="mx-auto max-w-7xl px-4 text-center sm:px-6 lg:px-8">
        <h1 class="text-4xl font-bold tracking-tight text-white sm:text-5xl">
          Download TwoFac
        </h1>
        <p class="mx-auto mt-4 max-w-2xl text-lg text-primary-100">
          Free, open-source 2FA for every platform. Pick yours and get started in seconds.
        </p>
      </div>
    </section>

    <div class="mx-auto max-w-7xl px-4 py-16 sm:px-6 sm:py-24 lg:px-8">
      <!-- Download Categories -->
      <div class="space-y-16">
        <section v-for="category in categories" :key="category.title">
          <h2
            class="text-2xl font-bold tracking-tight text-secondary-900 dark:text-white sm:text-3xl"
          >
            {{ category.title }}
          </h2>

          <div class="mt-6 grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
            <div
              v-for="card in category.cards"
              :key="card.name"
              class="flex flex-col rounded-xl border border-secondary-200 dark:border-secondary-700 bg-white dark:bg-secondary-900 p-6 shadow-sm transition-shadow hover:shadow-md"
            >
              <div class="text-4xl">{{ card.icon }}</div>
              <h3
                class="mt-3 text-lg font-semibold text-secondary-900 dark:text-white"
              >
                {{ card.name }}
              </h3>
              <p class="mt-1 flex-1 text-sm text-secondary-600 dark:text-secondary-400">
                {{ card.description }}
              </p>
              <a
                :href="card.link"
                :target="card.external ? '_blank' : undefined"
                :rel="card.external ? 'noopener noreferrer' : undefined"
                class="mt-4 inline-flex items-center justify-center rounded-lg bg-primary-600 px-4 py-2 text-sm font-semibold text-white hover:bg-primary-700 transition-colors"
                :class="{ 'pointer-events-none opacity-60': card.link === '#' }"
              >
                {{ card.linkText }}
              </a>
            </div>
          </div>
        </section>
      </div>

      <!-- GitHub Releases Note -->
      <div
        class="mt-16 rounded-xl border border-secondary-200 dark:border-secondary-700 bg-secondary-50 dark:bg-secondary-900 p-6 text-center"
      >
        <p class="text-secondary-700 dark:text-secondary-300">
          All desktop and CLI binaries are available on
          <a
            href="https://github.com/aspect-build/twofac/releases"
            target="_blank"
            rel="noopener noreferrer"
            class="font-semibold text-primary-600 dark:text-primary-400 hover:underline"
          >
            GitHub Releases
          </a>.
        </p>
      </div>
    </div>
  </MainLayout>
</template>
