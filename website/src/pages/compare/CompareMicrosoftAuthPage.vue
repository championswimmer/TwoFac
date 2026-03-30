<script setup lang="ts">
import { RouterLink } from 'vue-router'
import MainLayout from '../../layouts/MainLayout.vue'
import ComparisonTable from '../../components/ComparisonTable.vue'
import { useSEO } from '../../composables/useSEO'

useSEO({
  title: 'TwoFac vs Microsoft Authenticator',
  description: 'Compare TwoFac and Microsoft Authenticator. TwoFac is open source, privacy-focused, lightweight, and works on all platforms including Linux and CLI.',
  canonicalPath: '/compare/microsoft-authenticator',
})

const features = [
  { name: 'Open Source', twofac: '✓', competitor: '✗' },
  { name: 'Privacy Focused', twofac: '✓', competitor: '✗ (telemetry)' },
  { name: 'Lightweight', twofac: '✓', competitor: '✗' },
  { name: 'Desktop App', twofac: '✓', competitor: '✗' },
  { name: 'CLI App', twofac: '✓', competitor: '✗' },
  { name: 'Linux Support', twofac: '✓', competitor: '✗' },
  { name: 'Web App', twofac: '✓', competitor: '✗' },
  { name: 'Wear OS', twofac: '✓', competitor: '✗' },
  { name: 'watchOS', twofac: '✓', competitor: '✗' },
  { name: 'No Microsoft Account', twofac: '✓', competitor: '✗' },
  { name: 'Browser Extension', twofac: '✓', competitor: '✗' },
]
</script>

<template>
  <MainLayout>
    <!-- Hero -->
    <section class="bg-gradient-to-br from-primary-50 to-white dark:from-secondary-900 dark:to-secondary-950 py-16 sm:py-20">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
        <h1 class="text-3xl sm:text-4xl lg:text-5xl font-bold text-secondary-900 dark:text-white">
          TwoFac vs <span class="text-primary-600 dark:text-primary-400">Microsoft Authenticator</span>
        </h1>
        <p class="mt-4 text-lg text-secondary-600 dark:text-secondary-400 max-w-2xl mx-auto">
          Microsoft Authenticator is tied to the Microsoft ecosystem, collects telemetry, and only runs on mobile.
          TwoFac is the open-source, privacy-first alternative that works on every platform.
        </p>
      </div>
    </section>

    <!-- Comparison Table -->
    <section class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-12 sm:py-16">
      <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-6">Feature Comparison</h2>
      <ComparisonTable :features="features" competitor-name="Microsoft Authenticator" />
    </section>

    <!-- Why Choose TwoFac -->
    <section class="bg-secondary-50 dark:bg-secondary-900 py-12 sm:py-16">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-6">Why Choose TwoFac?</h2>
        <div class="grid gap-6 sm:grid-cols-2">
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2">🛡️ Privacy First</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              No telemetry, no tracking, no data collection. TwoFac respects your privacy — your 2FA secrets stay on your devices.
            </p>
          </div>
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2">🐧 Linux & CLI</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              Full Linux desktop support and a powerful CLI tool — something Microsoft Authenticator will never offer.
            </p>
          </div>
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2">🪶 Lightweight</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              No password manager, no Microsoft account features, no bloat. TwoFac is lean and fast.
            </p>
          </div>
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2">🔓 No Vendor Lock-in</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              No Microsoft account required. No ecosystem lock-in. Export your data anytime in standard formats.
            </p>
          </div>
        </div>
      </div>
    </section>

    <!-- CTA -->
    <section class="py-12 sm:py-16">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
        <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-4">Switch to TwoFac</h2>
        <p class="text-secondary-600 dark:text-secondary-400 mb-6">
          Open source, no tracking, and available on every platform. Take back control of your 2FA.
        </p>
        <RouterLink
          to="/download"
          class="inline-flex items-center px-6 py-3 rounded-lg bg-primary-600 text-white font-medium hover:bg-primary-700 transition-colors shadow-sm"
        >
          Download TwoFac
        </RouterLink>
      </div>
    </section>
  </MainLayout>
</template>
