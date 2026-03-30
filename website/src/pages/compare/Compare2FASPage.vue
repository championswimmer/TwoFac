<script setup lang="ts">
import { RouterLink } from 'vue-router'
import MainLayout from '../../layouts/MainLayout.vue'
import ComparisonTable from '../../components/ComparisonTable.vue'
import { useSEO } from '../../composables/useSEO'

useSEO({
  title: 'TwoFac vs 2FAS',
  description: 'Compare TwoFac and 2FAS authenticator apps. See how TwoFac offers desktop apps, CLI tools, and wearable support that 2FAS lacks.',
  canonicalPath: '/compare/2fas',
})

const features = [
  { name: 'Open Source', twofac: '✓', competitor: '✓' },
  { name: 'Android App', twofac: '✓', competitor: '✓' },
  { name: 'iOS App', twofac: '✓', competitor: '✓' },
  { name: 'Desktop App', twofac: '✓', competitor: '✗' },
  { name: 'CLI App', twofac: '✓', competitor: '✗' },
  { name: 'Web App (PWA)', twofac: '✓', competitor: '✗' },
  { name: 'Browser Extension', twofac: '✓', competitor: '✓' },
  { name: 'Wear OS', twofac: '✓', competitor: '✗' },
  { name: 'watchOS', twofac: '✓', competitor: '✗' },
  { name: 'Encrypted Backups', twofac: '✓', competitor: '✓' },
  { name: 'Biometric Auth', twofac: '✓', competitor: '✓' },
]
</script>

<template>
  <MainLayout>
    <!-- Hero -->
    <section class="bg-gradient-to-br from-primary-50 to-white dark:from-secondary-900 dark:to-secondary-950 py-16 sm:py-20">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
        <h1 class="text-3xl sm:text-4xl lg:text-5xl font-bold text-secondary-900 dark:text-white">
          TwoFac vs <span class="text-primary-600 dark:text-primary-400">2FAS</span>
        </h1>
        <p class="mt-4 text-lg text-secondary-600 dark:text-secondary-400 max-w-2xl mx-auto">
          Both TwoFac and 2FAS are open-source authenticator apps. While 2FAS focuses on mobile platforms,
          TwoFac goes further with native desktop, CLI, and wearable support.
        </p>
      </div>
    </section>

    <!-- Comparison Table -->
    <section class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-12 sm:py-16">
      <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-6">Feature Comparison</h2>
      <ComparisonTable :features="features" competitor-name="2FAS" />
    </section>

    <!-- Why Choose TwoFac -->
    <section class="bg-secondary-50 dark:bg-secondary-900 py-12 sm:py-16">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-6">Why Choose TwoFac?</h2>
        <div class="grid gap-6 sm:grid-cols-2">
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2">🖥️ Desktop & CLI</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              TwoFac offers native desktop apps for macOS, Windows, and Linux, plus a powerful CLI tool — something 2FAS doesn't provide.
            </p>
          </div>
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2">⌚ Wearable Support</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              Access your 2FA codes directly from your Wear OS watch or Apple Watch — no need to reach for your phone.
            </p>
          </div>
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2">🌐 Web & Browser Extension</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              Use TwoFac as a progressive web app or browser extension for quick access to your codes from any browser.
            </p>
          </div>
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2">🔧 Kotlin Multiplatform</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              Built with Kotlin Multiplatform for true native performance on every platform with a single shared codebase.
            </p>
          </div>
        </div>
      </div>
    </section>

    <!-- CTA -->
    <section class="py-12 sm:py-16">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
        <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-4">Ready to switch?</h2>
        <p class="text-secondary-600 dark:text-secondary-400 mb-6">
          TwoFac supports importing from 2FAS. Get started in minutes.
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
