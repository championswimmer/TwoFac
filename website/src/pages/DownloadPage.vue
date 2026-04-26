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

const cliInstallCommand = 'curl -fsSL https://twofac.app/install.sh | bash -s --'

const categories: { title: string; icon: string; cards: DownloadCard[] }[] = [
  {
    title: 'Mobile',
    icon: 'fa-solid fa-mobile-screen-button',
    cards: [
      {
        icon: 'fa-brands fa-android',
        name: 'Android',
        description: 'Full-featured 2FA app with biometric lock and Wear OS companion.',
        link: '#',
        linkText: 'Google Play',
        external: true,
      },
      {
        icon: 'fa-brands fa-apple',
        name: 'iOS',
        description: 'Native iOS app with Face ID / Touch ID and watchOS companion.',
        link: '#',
        linkText: 'App Store',
        external: true,
      },
    ],
  },
  {
    title: 'Desktop',
    icon: 'fa-solid fa-desktop',
    cards: [
      {
        icon: 'fa-brands fa-apple',
        name: 'macOS',
        description: 'Native desktop app packaged as a DMG.',
        link: 'https://github.com/championswimmer/TwoFac/releases',
        linkText: 'Download DMG',
        external: true,
      },
      {
        icon: 'fa-brands fa-windows',
        name: 'Windows',
        description: 'Windows installer packaged as an MSI.',
        link: 'https://github.com/championswimmer/TwoFac/releases',
        linkText: 'Download MSI',
        external: true,
      },
      {
        icon: 'fa-brands fa-linux',
        name: 'Linux',
        description: 'Debian package for Ubuntu and other distributions.',
        link: 'https://github.com/championswimmer/TwoFac/releases',
        linkText: 'Download DEB',
        external: true,
      },
    ],
  },
  {
    title: 'Web',
    icon: 'fa-solid fa-globe',
    cards: [
      {
        icon: 'fa-solid fa-globe',
        name: 'PWA',
        description: 'Install TwoFac as a Progressive Web App in any modern browser.',
        link: 'https://web.twofac.app',
        linkText: 'Open Web App',
        external: true,
      },
      {
        icon: 'fa-brands fa-chrome',
        name: 'Chrome Extension',
        description: 'Access your 2FA codes directly from the Chrome toolbar.',
        link: '#',
        linkText: 'Chrome Web Store',
        external: true,
      },
      {
        icon: 'fa-brands fa-firefox-browser',
        name: 'Firefox Extension',
        description: 'Access your 2FA codes directly from the Firefox toolbar.',
        link: '#',
        linkText: 'Firefox Add-ons',
        external: true,
      },
    ],
  },
  {
    title: 'CLI',
    icon: 'fa-solid fa-terminal',
    cards: [
      {
        icon: 'fa-brands fa-apple',
        name: 'macOS CLI',
        description: 'Native CLI binary for macOS (arm64 and x86_64).',
        link: 'https://github.com/championswimmer/TwoFac/releases',
        linkText: 'GitHub Releases',
        external: true,
      },
      {
        icon: 'fa-brands fa-windows',
        name: 'Windows CLI',
        description: 'Native CLI binary for Windows.',
        link: 'https://github.com/championswimmer/TwoFac/releases',
        linkText: 'GitHub Releases',
        external: true,
      },
      {
        icon: 'fa-brands fa-linux',
        name: 'Linux CLI',
        description: 'Native CLI binary for Linux (x86_64).',
        link: 'https://github.com/championswimmer/TwoFac/releases',
        linkText: 'GitHub Releases',
        external: true,
      },
    ],
  },
  {
    title: 'Wearables',
    icon: 'fa-solid fa-clock',
    cards: [
      {
        icon: 'fa-solid fa-clock',
        name: 'Wear OS',
        description: 'Bundled with the Android app. Syncs codes automatically.',
        link: '#',
        linkText: 'Included with Android',
      },
      {
        icon: 'fa-solid fa-clock',
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
    <section class="border-b border-secondary-200 bg-secondary-100 py-16 sm:py-24 dark:border-secondary-800 dark:bg-secondary-900">
      <div class="mx-auto max-w-7xl px-4 text-center sm:px-6 lg:px-8">
        <h1 class="text-4xl font-bold tracking-tight text-secondary-900 dark:text-secondary-50 sm:text-5xl">
          Download TwoFac
        </h1>
        <p class="mx-auto mt-4 max-w-2xl text-lg text-secondary-700 dark:text-secondary-300">
          Free, open-source 2FA for every platform. Pick yours and get started in seconds.
        </p>
      </div>
    </section>

     <div class="mx-auto max-w-7xl px-4 py-16 sm:px-6 sm:py-24 lg:px-8">
       <section
         class="mb-16 rounded-2xl border border-primary-200 bg-primary-50 p-6 shadow-sm dark:border-primary-900 dark:bg-primary-950/40"
       >
         <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
           <div class="max-w-3xl">
             <p class="text-sm font-semibold uppercase tracking-wide text-primary-700 dark:text-primary-300">
               CLI installer
             </p>
             <h2 class="mt-2 text-2xl font-bold tracking-tight text-secondary-900 dark:text-white">
               Install the TwoFac CLI in one command
             </h2>
             <p class="mt-3 text-secondary-700 dark:text-secondary-300">
               For macOS (Apple Silicon and Intel) and Linux x86_64, use the hosted installer to
               fetch the latest CLI release automatically.
             </p>
           </div>
           <a
             href="https://github.com/championswimmer/TwoFac/releases"
             target="_blank"
             rel="noopener noreferrer"
             class="inline-flex items-center justify-center rounded-lg border border-primary-300 px-4 py-2 text-sm font-semibold text-primary-700 transition-colors hover:bg-primary-100 dark:border-primary-700 dark:text-primary-300 dark:hover:bg-primary-900/60"
           >
             Manual downloads
           </a>
         </div>

         <div class="mt-5 rounded-xl bg-secondary-950 px-4 py-4 text-sm text-secondary-50">
           <code class="block overflow-x-auto font-mono">{{ cliInstallCommand }}</code>
         </div>

         <p class="mt-3 text-sm text-secondary-600 dark:text-secondary-400">
           Windows CLI binaries are still available from GitHub Releases as direct `.exe`
           downloads.
         </p>
       </section>

       <!-- Download Categories -->
       <div class="space-y-16">
        <section v-for="category in categories" :key="category.title">
          <h2
            class="text-2xl font-bold tracking-tight text-secondary-900 dark:text-white sm:text-3xl"
          >
            <i :class="category.icon" class="mr-2" aria-hidden="true" />{{ category.title }}
          </h2>

          <div class="mt-6 grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3">
            <div
              v-for="card in category.cards"
              :key="card.name"
              class="flex flex-col rounded-xl border border-secondary-200 dark:border-secondary-700 bg-secondary-50 dark:bg-secondary-900 p-6 transition-colors hover:bg-secondary-100 dark:hover:bg-secondary-800"
            >
              <div class="text-4xl text-primary-600 dark:text-primary-400"><i :class="card.icon" aria-hidden="true" /></div>
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
            href="https://github.com/championswimmer/TwoFac/releases"
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
