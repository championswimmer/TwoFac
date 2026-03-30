<script setup lang="ts">
import { RouterLink } from 'vue-router'
import PlatformBadge from './PlatformBadge.vue'

const platforms = [
  { name: 'Android', icon: '📱' },
  { name: 'iOS', icon: '🍎' },
  { name: 'Desktop', icon: '🖥️' },
  { name: 'Web', icon: '🌐' },
  { name: 'CLI', icon: '⌨️' },
  { name: 'Wear OS', icon: '⌚' },
  { name: 'watchOS', icon: '⌚' },
]
</script>

<template>
  <section class="relative overflow-hidden">
    <!-- Gradient background -->
    <div
      class="absolute inset-0 bg-gradient-to-br from-primary-600 via-primary-700 to-purple-800 dark:from-primary-900 dark:via-primary-950 dark:to-purple-950"
    />

    <!-- Decorative floating shapes -->
    <div class="absolute inset-0 overflow-hidden" aria-hidden="true">
      <div
        class="absolute -left-20 -top-20 h-72 w-72 rounded-full bg-white/5 blur-3xl"
      />
      <div
        class="absolute -bottom-32 right-0 h-96 w-96 rounded-full bg-purple-500/10 blur-3xl"
      />
      <div
        class="absolute left-1/2 top-1/4 h-48 w-48 -translate-x-1/2 rounded-full bg-primary-400/10 blur-2xl"
      />
    </div>

    <!-- Content -->
    <div class="relative mx-auto max-w-7xl px-4 py-24 sm:px-6 sm:py-32 lg:px-8 lg:py-40">
      <div class="mx-auto max-w-3xl text-center">
        <h1
          class="text-4xl font-extrabold tracking-tight text-white sm:text-5xl lg:text-6xl"
        >
          Your Codes. Your Devices.
          <span class="block text-primary-200">Your Control.</span>
        </h1>

        <p class="mx-auto mt-6 max-w-2xl text-lg leading-relaxed text-primary-100/90 sm:text-xl">
          Open source, cross-platform 2FA authenticator for Watch, Mobile, Desktop, Web, and CLI.
        </p>

        <!-- CTA buttons -->
        <div class="mt-10 flex flex-col items-center justify-center gap-4 sm:flex-row">
          <RouterLink
            to="/download"
            class="inline-flex items-center gap-2 rounded-xl bg-white px-8 py-3.5 text-base font-semibold text-primary-700 shadow-lg transition-all hover:bg-primary-50 hover:shadow-xl focus:outline-none focus:ring-2 focus:ring-white focus:ring-offset-2 focus:ring-offset-primary-700"
          >
            <svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" d="M3 16.5v2.25A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75V16.5M16.5 12L12 16.5m0 0L7.5 12m4.5 4.5V3" />
            </svg>
            Download Now
          </RouterLink>

          <a
            href="https://github.com/championswimmer/TwoFac"
            target="_blank"
            rel="noopener noreferrer"
            class="inline-flex items-center gap-2 rounded-xl border-2 border-white/30 px-8 py-3.5 text-base font-semibold text-white backdrop-blur-sm transition-all hover:border-white/60 hover:bg-white/10 focus:outline-none focus:ring-2 focus:ring-white focus:ring-offset-2 focus:ring-offset-primary-700"
          >
            <svg class="h-5 w-5" fill="currentColor" viewBox="0 0 24 24" aria-hidden="true">
              <path
                fill-rule="evenodd"
                d="M12 2C6.477 2 2 6.484 2 12.017c0 4.425 2.865 8.18 6.839 9.504.5.092.682-.217.682-.483 0-.237-.008-.868-.013-1.703-2.782.605-3.369-1.343-3.369-1.343-.454-1.158-1.11-1.466-1.11-1.466-.908-.62.069-.608.069-.608 1.003.07 1.531 1.032 1.531 1.032.892 1.53 2.341 1.088 2.91.832.092-.647.35-1.088.636-1.338-2.22-.253-4.555-1.113-4.555-4.951 0-1.093.39-1.988 1.029-2.688-.103-.253-.446-1.272.098-2.65 0 0 .84-.27 2.75 1.026A9.564 9.564 0 0112 6.844c.85.004 1.705.115 2.504.337 1.909-1.296 2.747-1.027 2.747-1.027.546 1.379.202 2.398.1 2.651.64.7 1.028 1.595 1.028 2.688 0 3.848-2.339 4.695-4.566 4.943.359.309.678.92.678 1.855 0 1.338-.012 2.419-.012 2.747 0 .268.18.58.688.482A10.019 10.019 0 0022 12.017C22 6.484 17.522 2 12 2z"
                clip-rule="evenodd"
              />
            </svg>
            View on GitHub
          </a>
        </div>

        <!-- Platform badges -->
        <div class="mt-14 flex flex-wrap items-center justify-center gap-3">
          <PlatformBadge
            v-for="platform in platforms"
            :key="platform.name"
            :name="platform.name"
            :icon="platform.icon"
          />
        </div>
      </div>
    </div>
  </section>
</template>
