<script setup lang="ts">
import MainLayout from '../layouts/MainLayout.vue'
import PlatformBadge from '../components/PlatformBadge.vue'
import { useSEO } from '../composables/useSEO'

useSEO({
  title: 'Features',
  description:
    'Explore TwoFac features: cross-platform 2FA, end-to-end encryption, wearable support, CLI tools, browser extensions, and easy migration from other authenticator apps.',
  canonicalPath: '/features',
})

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
    <!-- Hero Banner -->
    <section class="border-b border-secondary-200 bg-secondary-100 py-16 sm:py-24 dark:border-secondary-800 dark:bg-secondary-900">
      <div class="mx-auto max-w-7xl px-4 text-center sm:px-6 lg:px-8">
        <h1 class="text-4xl font-bold tracking-tight text-secondary-900 dark:text-secondary-50 sm:text-5xl">
          Features
        </h1>
        <p class="mx-auto mt-4 max-w-2xl text-lg text-secondary-700 dark:text-secondary-300">
          Everything you need for secure, convenient two-factor authentication — across every device you own.
        </p>
      </div>
    </section>

    <div class="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
      <!-- Multi-Platform Support -->
      <section class="py-16 sm:py-20 border-b border-secondary-200 dark:border-secondary-800">
        <h2 class="text-3xl font-bold tracking-tight text-secondary-900 dark:text-white">
          <i class="fa-solid fa-mobile-screen-button mr-2" aria-hidden="true" />Multi-Platform Support
        </h2>
        <p class="mt-4 max-w-3xl text-lg text-secondary-600 dark:text-secondary-400">
          TwoFac runs natively on 11+ platforms. From phones and desktops to wearables, browsers,
          and the command line — your 2FA codes are always within reach. Every app is built with
          Kotlin Multiplatform for a truly native experience.
        </p>
        <div class="mt-8 flex flex-wrap gap-3">
          <PlatformBadge
            v-for="platform in platforms"
            :key="platform.name"
            :name="platform.name"
            :icon="platform.icon"
            :available="platform.available"
          />
        </div>
      </section>

      <!-- Security First -->
      <section class="py-16 sm:py-20 border-b border-secondary-200 dark:border-secondary-800">
        <h2 class="text-3xl font-bold tracking-tight text-secondary-900 dark:text-white">
          <i class="fa-solid fa-shield-halved mr-2" aria-hidden="true" />Security First
        </h2>
        <p class="mt-4 max-w-3xl text-lg text-secondary-600 dark:text-secondary-400">
          Your 2FA secrets are encrypted end-to-end before they ever leave your device. TwoFac uses
          a zero-knowledge architecture so even we can't see your codes.
        </p>
        <ul class="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-2">
          <li class="flex items-start gap-3 text-secondary-700 dark:text-secondary-300">
            <i class="mt-1 fa-solid fa-circle-check text-primary-500" aria-hidden="true" />
            <span>End-to-end encryption for all secrets and backups</span>
          </li>
          <li class="flex items-start gap-3 text-secondary-700 dark:text-secondary-300">
            <i class="mt-1 fa-solid fa-circle-check text-primary-500" aria-hidden="true" />
            <span>Biometric authentication on Android and iOS (fingerprint, face unlock)</span>
          </li>
          <li class="flex items-start gap-3 text-secondary-700 dark:text-secondary-300">
            <i class="mt-1 fa-solid fa-circle-check text-primary-500" aria-hidden="true" />
            <span>WebAuthn / passkey support in browser extensions</span>
          </li>
          <li class="flex items-start gap-3 text-secondary-700 dark:text-secondary-300">
            <i class="mt-1 fa-solid fa-circle-check text-primary-500" aria-hidden="true" />
            <span>Secure session management with automatic lock</span>
          </li>
        </ul>
      </section>

      <!-- Wearable Integration -->
      <section class="py-16 sm:py-20 border-b border-secondary-200 dark:border-secondary-800">
        <h2 class="text-3xl font-bold tracking-tight text-secondary-900 dark:text-white">
          <i class="fa-solid fa-clock mr-2" aria-hidden="true" />Wearable Integration
        </h2>
        <p class="mt-4 max-w-3xl text-lg text-secondary-600 dark:text-secondary-400">
          Check your 2FA codes with a glance at your wrist. TwoFac offers companion apps for both
          Wear OS and watchOS, with offline caching so your codes are available even when your phone
          isn't nearby. Accounts sync automatically whenever your watch is connected.
        </p>
      </section>

      <!-- CLI Power Tools -->
      <section class="py-16 sm:py-20 border-b border-secondary-200 dark:border-secondary-800">
        <h2 class="text-3xl font-bold tracking-tight text-secondary-900 dark:text-white">
          <i class="fa-solid fa-terminal mr-2" aria-hidden="true" />CLI Power Tools
        </h2>
        <p class="mt-4 max-w-3xl text-lg text-secondary-600 dark:text-secondary-400">
          Built for developers who live in the terminal. The TwoFac CLI lets you add and manage
          accounts, display codes with auto-refresh, create encrypted backups, and pipe OTP codes
          into scripts — all from the command line.
        </p>
        <div
          class="mt-6 rounded-lg border border-secondary-300 bg-secondary-50 p-4 font-mono text-sm text-secondary-800 overflow-x-auto dark:border-secondary-700 dark:bg-secondary-900 dark:text-secondary-200"
        >
          <p>$ twofac list</p>
          <p>$ twofac code github</p>
          <p>$ twofac add --issuer GitHub --account user@example.com</p>
          <p>$ twofac backup export --encrypted</p>
        </div>
      </section>

      <!-- Browser Extensions -->
      <section class="py-16 sm:py-20 border-b border-secondary-200 dark:border-secondary-800">
        <h2 class="text-3xl font-bold tracking-tight text-secondary-900 dark:text-white">
          <i class="fa-solid fa-puzzle-piece mr-2" aria-hidden="true" />Browser Extensions
        </h2>
        <p class="mt-4 max-w-3xl text-lg text-secondary-600 dark:text-secondary-400">
          Access your 2FA codes directly in your browser with extensions for Chrome and Firefox.
          TwoFac also works as a Progressive Web App (PWA), so you can install it on any platform
          with a modern browser for a lightweight, always-available experience.
        </p>
      </section>

      <!-- Import & Migration -->
      <section class="py-16 sm:py-20 border-b border-secondary-200 dark:border-secondary-800">
        <h2 class="text-3xl font-bold tracking-tight text-secondary-900 dark:text-white">
          <i class="fa-solid fa-right-left mr-2" aria-hidden="true" />Import &amp; Migration
        </h2>
        <p class="mt-4 max-w-3xl text-lg text-secondary-600 dark:text-secondary-400">
          Switching to TwoFac is effortless. Import your existing accounts in one click from popular
          authenticator apps.
        </p>
        <ul class="mt-6 space-y-3 text-secondary-700 dark:text-secondary-300">
          <li class="flex items-start gap-3">
            <i class="mt-1 fa-solid fa-arrow-right text-primary-500" aria-hidden="true" />
            <span><strong>Authy</strong> — Import via JSON export</span>
          </li>
          <li class="flex items-start gap-3">
            <i class="mt-1 fa-solid fa-arrow-right text-primary-500" aria-hidden="true" />
            <span><strong>2FAS</strong> — Import via JSON export</span>
          </li>
          <li class="flex items-start gap-3">
            <i class="mt-1 fa-solid fa-arrow-right text-primary-500" aria-hidden="true" />
            <span><strong>Ente Auth</strong> — Import via plaintext <code class="rounded bg-secondary-100 dark:bg-secondary-800 px-1.5 py-0.5 text-sm">otpauth://</code> URIs</span>
          </li>
          <li class="flex items-start gap-3">
            <i class="mt-1 fa-solid fa-arrow-right text-primary-500" aria-hidden="true" />
            <span><strong>Google Authenticator</strong> — Import via QR code export</span>
          </li>
        </ul>
      </section>

      <!-- Open Source -->
      <section class="py-16 sm:py-20">
        <h2 class="text-3xl font-bold tracking-tight text-secondary-900 dark:text-white">
          <i class="fa-solid fa-code-branch mr-2" aria-hidden="true" />Open Source
        </h2>
        <p class="mt-4 max-w-3xl text-lg text-secondary-600 dark:text-secondary-400">
          TwoFac is fully open source under the MIT license. Every line of code is available for
          audit on GitHub. We believe security tools should be transparent, community-driven, and
          free for everyone to use, modify, and distribute.
        </p>
        <div class="mt-6">
          <a
            href="https://github.com/championswimmer/TwoFac"
            target="_blank"
            rel="noopener noreferrer"
            class="inline-flex items-center rounded-lg border border-primary-700 bg-primary-700 px-5 py-2.5 text-sm font-semibold text-secondary-50 hover:bg-primary-800 transition-colors dark:border-primary-500 dark:bg-primary-600 dark:hover:bg-primary-500"
          >
            View on GitHub <i class="fa-solid fa-arrow-right-long ml-2" aria-hidden="true" />
          </a>
        </div>
      </section>
    </div>
  </MainLayout>
</template>
