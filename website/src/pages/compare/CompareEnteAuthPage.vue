<script setup lang="ts">
import { RouterLink } from 'vue-router'
import MainLayout from '../../layouts/MainLayout.vue'
import ComparisonTable from '../../components/ComparisonTable.vue'
import { useSEO } from '../../composables/useSEO'

useSEO({
  title: 'TwoFac vs Ente Auth',
  description: 'Compare TwoFac and Ente Auth. TwoFac offers CLI, browser extensions, and wearable apps — built with Kotlin Multiplatform for true native performance.',
  canonicalPath: '/compare/ente-auth',
})

const features = [
  { name: 'Open Source', twofac: '✓', competitor: '✓' },
  { name: 'Android App', twofac: '✓', competitor: '✓' },
  { name: 'iOS App', twofac: '✓', competitor: '✓' },
  { name: 'Desktop App', twofac: '✓', competitor: '✓' },
  { name: 'CLI App', twofac: '✓', competitor: '✗' },
  { name: 'Web App', twofac: '✓', competitor: '✓' },
  { name: 'Browser Extension', twofac: '✓', competitor: '✗' },
  { name: 'Wear OS', twofac: '✓', competitor: '✗' },
  { name: 'watchOS', twofac: '✓', competitor: '✗' },
  { name: 'Encrypted Backups', twofac: '✓', competitor: '✓' },
  { name: 'Kotlin Multiplatform', twofac: '✓', competitor: '✗' },
]
</script>

<template>
  <MainLayout>
    <!-- Hero -->
    <section class="bg-gradient-to-br from-primary-50 to-white dark:from-secondary-900 dark:to-secondary-950 py-16 sm:py-20">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
        <h1 class="text-3xl sm:text-4xl lg:text-5xl font-bold text-secondary-900 dark:text-white">
          TwoFac vs <span class="text-primary-600 dark:text-primary-400">Ente Auth</span>
        </h1>
        <p class="mt-4 text-lg text-secondary-600 dark:text-secondary-400 max-w-2xl mx-auto">
          TwoFac and Ente Auth are both open-source authenticators with desktop and web support.
          TwoFac goes further with a CLI, browser extensions, and wearable apps — all built natively with Kotlin Multiplatform.
        </p>
      </div>
    </section>

    <!-- Comparison Table -->
    <section class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-12 sm:py-16">
      <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-6">Feature Comparison</h2>
      <ComparisonTable :features="features" competitor-name="Ente Auth" />
    </section>

    <!-- Why Choose TwoFac -->
    <section class="bg-secondary-50 dark:bg-secondary-900 py-12 sm:py-16">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-6">Why Choose TwoFac?</h2>
        <div class="grid gap-6 sm:grid-cols-2">
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2">💻 CLI Access</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              Generate 2FA codes right from your terminal. Perfect for developers and server administrators who live in the command line.
            </p>
          </div>
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2">🧩 Browser Extension</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              Auto-fill 2FA codes directly in your browser. No need to switch between apps or type codes manually.
            </p>
          </div>
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2">⌚ Wearable Apps</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              Native apps for Wear OS and watchOS let you view 2FA codes on your wrist — something Ente Auth doesn't offer.
            </p>
          </div>
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2">🔧 True Native Performance</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              Built with Kotlin Multiplatform, TwoFac compiles to native code on each platform — no Flutter runtime overhead.
            </p>
          </div>
        </div>
      </div>
    </section>

    <!-- CTA -->
    <section class="py-12 sm:py-16">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
        <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-4">Ready to try TwoFac?</h2>
        <p class="text-secondary-600 dark:text-secondary-400 mb-6">
          Available on 11+ platforms. Download and get started in minutes.
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
