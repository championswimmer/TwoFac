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
    <!-- Hero Banner -->
    <section class="bg-primary-600 dark:bg-primary-800 py-16 sm:py-24">
      <div class="mx-auto max-w-7xl px-4 text-center sm:px-6 lg:px-8">
        <h1 class="text-4xl font-bold tracking-tight text-white sm:text-5xl">
          Features
        </h1>
        <p class="mx-auto mt-4 max-w-2xl text-lg text-primary-100">
          Everything you need for secure, convenient two-factor authentication — across every device you own.
        </p>
      </div>
    </section>

    <div class="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
      <!-- Multi-Platform Support -->
      <section class="py-16 sm:py-20 border-b border-secondary-200 dark:border-secondary-800">
        <h2 class="text-3xl font-bold tracking-tight text-secondary-900 dark:text-white">
          📱 Multi-Platform Support
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
          🔐 Security First
        </h2>
        <p class="mt-4 max-w-3xl text-lg text-secondary-600 dark:text-secondary-400">
          Your 2FA secrets are encrypted end-to-end before they ever leave your device. TwoFac uses
          a zero-knowledge architecture so even we can't see your codes.
        </p>
        <ul class="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-2">
          <li class="flex items-start gap-3 text-secondary-700 dark:text-secondary-300">
            <span class="mt-1 text-primary-500">✓</span>
            <span>End-to-end encryption for all secrets and backups</span>
          </li>
          <li class="flex items-start gap-3 text-secondary-700 dark:text-secondary-300">
            <span class="mt-1 text-primary-500">✓</span>
            <span>Biometric authentication on Android and iOS (fingerprint, face unlock)</span>
          </li>
          <li class="flex items-start gap-3 text-secondary-700 dark:text-secondary-300">
            <span class="mt-1 text-primary-500">✓</span>
            <span>WebAuthn / passkey support in browser extensions</span>
          </li>
          <li class="flex items-start gap-3 text-secondary-700 dark:text-secondary-300">
            <span class="mt-1 text-primary-500">✓</span>
            <span>Secure session management with automatic lock</span>
          </li>
        </ul>
      </section>

      <!-- Wearable Integration -->
      <section class="py-16 sm:py-20 border-b border-secondary-200 dark:border-secondary-800">
        <h2 class="text-3xl font-bold tracking-tight text-secondary-900 dark:text-white">
          ⌚ Wearable Integration
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
          ⌨️ CLI Power Tools
        </h2>
        <p class="mt-4 max-w-3xl text-lg text-secondary-600 dark:text-secondary-400">
          Built for developers who live in the terminal. The TwoFac CLI lets you add and manage
          accounts, display codes with auto-refresh, create encrypted backups, and pipe OTP codes
          into scripts — all from the command line.
        </p>
        <div
          class="mt-6 rounded-lg bg-secondary-900 dark:bg-secondary-950 p-4 font-mono text-sm text-green-400 overflow-x-auto"
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
          🧩 Browser Extensions
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
          🔄 Import &amp; Migration
        </h2>
        <p class="mt-4 max-w-3xl text-lg text-secondary-600 dark:text-secondary-400">
          Switching to TwoFac is effortless. Import your existing accounts in one click from popular
          authenticator apps.
        </p>
        <ul class="mt-6 space-y-3 text-secondary-700 dark:text-secondary-300">
          <li class="flex items-start gap-3">
            <span class="mt-1 text-primary-500">→</span>
            <span><strong>Authy</strong> — Import via JSON export</span>
          </li>
          <li class="flex items-start gap-3">
            <span class="mt-1 text-primary-500">→</span>
            <span><strong>2FAS</strong> — Import via JSON export</span>
          </li>
          <li class="flex items-start gap-3">
            <span class="mt-1 text-primary-500">→</span>
            <span><strong>Ente Auth</strong> — Import via plaintext <code class="rounded bg-secondary-100 dark:bg-secondary-800 px-1.5 py-0.5 text-sm">otpauth://</code> URIs</span>
          </li>
          <li class="flex items-start gap-3">
            <span class="mt-1 text-primary-500">→</span>
            <span><strong>Google Authenticator</strong> — Import via QR code export</span>
          </li>
        </ul>
      </section>

      <!-- Open Source -->
      <section class="py-16 sm:py-20">
        <h2 class="text-3xl font-bold tracking-tight text-secondary-900 dark:text-white">
          🔓 Open Source
        </h2>
        <p class="mt-4 max-w-3xl text-lg text-secondary-600 dark:text-secondary-400">
          TwoFac is fully open source under the MIT license. Every line of code is available for
          audit on GitHub. We believe security tools should be transparent, community-driven, and
          free for everyone to use, modify, and distribute.
        </p>
        <div class="mt-6">
          <a
            href="https://github.com/aspect-build/twofac"
            target="_blank"
            rel="noopener noreferrer"
            class="inline-flex items-center rounded-lg bg-secondary-900 dark:bg-white px-5 py-2.5 text-sm font-semibold text-white dark:text-secondary-900 hover:bg-secondary-700 dark:hover:bg-secondary-200 transition-colors"
          >
            View on GitHub →
          </a>
        </div>
      </section>
    </div>
  </MainLayout>
</template>
