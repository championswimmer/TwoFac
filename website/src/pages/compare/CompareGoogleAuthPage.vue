<script setup lang="ts">
import { RouterLink } from 'vue-router'
import MainLayout from '../../layouts/MainLayout.vue'
import ComparisonTable from '../../components/ComparisonTable.vue'
import { useSEO } from '../../composables/useSEO'

useSEO({
  title: 'TwoFac vs Google Authenticator',
  description: 'Compare TwoFac and Google Authenticator. TwoFac is open source, works on 11+ platforms, and offers E2E encrypted sync.',
  canonicalPath: '/compare/google-authenticator',
})

const features = [
  { name: 'Open Source', twofac: '✓', competitor: '✗' },
  { name: 'Desktop App', twofac: '✓', competitor: '✗' },
  { name: 'CLI App', twofac: '✓', competitor: '✗' },
  { name: 'Web App', twofac: '✓', competitor: '✗' },
  { name: 'Browser Extension', twofac: '✓', competitor: '✗' },
  { name: 'Wear OS', twofac: '✓', competitor: '✗' },
  { name: 'watchOS', twofac: '✓', competitor: '✗' },
  { name: 'E2E Encrypted Sync', twofac: '✓', competitor: '✗' },
  { name: 'Encrypted Backups', twofac: '✓', competitor: '✗' },
  { name: 'Cross-Platform', twofac: '✓ (11+ platforms)', competitor: 'Mobile only' },
  { name: 'Import/Export', twofac: '✓', competitor: 'Limited' },
]
</script>

<template>
  <MainLayout>
    <!-- Hero -->
    <section class="bg-gradient-to-br from-primary-50 to-white dark:from-secondary-900 dark:to-secondary-950 py-16 sm:py-20">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
        <h1 class="text-3xl sm:text-4xl lg:text-5xl font-bold text-secondary-900 dark:text-white">
          TwoFac vs <span class="text-primary-600 dark:text-primary-400">Google Authenticator</span>
        </h1>
        <p class="mt-4 text-lg text-secondary-600 dark:text-secondary-400 max-w-2xl mx-auto">
          Google Authenticator is the most popular 2FA app, but it's closed-source, mobile-only,
          and lacks end-to-end encryption. TwoFac is the open-source alternative that works everywhere.
        </p>
      </div>
    </section>

    <!-- Comparison Table -->
    <section class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-12 sm:py-16">
      <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-6">Feature Comparison</h2>
      <ComparisonTable :features="features" competitor-name="Google Authenticator" />
    </section>

    <!-- Why Choose TwoFac -->
    <section class="bg-secondary-50 dark:bg-secondary-900 py-12 sm:py-16">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-6">Why Choose TwoFac?</h2>
        <div class="grid gap-6 sm:grid-cols-2">
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2">🔓 Open Source</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              TwoFac's code is fully open and auditable. You can verify exactly how your secrets are handled — unlike Google Authenticator.
            </p>
          </div>
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2">🌍 11+ Platforms</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              Android, iOS, macOS, Windows, Linux, Web, CLI, browser extension, Wear OS, watchOS — TwoFac runs wherever you do.
            </p>
          </div>
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2">🔐 E2E Encrypted Sync</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              Your 2FA secrets are encrypted end-to-end before leaving your device. Google Authenticator's sync has no E2E encryption.
            </p>
          </div>
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2">📦 Easy Migration</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              Import your accounts from Google Authenticator with a simple QR scan or file import. Switching takes minutes.
            </p>
          </div>
        </div>
      </div>
    </section>

    <!-- CTA -->
    <section class="py-12 sm:py-16">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
        <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-4">Upgrade from Google Authenticator</h2>
        <p class="text-secondary-600 dark:text-secondary-400 mb-6">
          Open source, cross-platform, and encrypted. Everything Google Authenticator should be.
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
