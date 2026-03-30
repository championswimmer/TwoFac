<script setup lang="ts">
import { RouterLink } from 'vue-router'
import MainLayout from '../../layouts/MainLayout.vue'
import ComparisonTable from '../../components/ComparisonTable.vue'
import { useSEO } from '../../composables/useSEO'

useSEO({
  title: 'TwoFac vs Bitwarden Authenticator',
  description: 'Compare TwoFac and Bitwarden Authenticator. TwoFac is a dedicated, lightweight 2FA app — no account or subscription required.',
  canonicalPath: '/compare/bitwarden',
})

const features = [
  { name: 'Standalone 2FA App', twofac: '✓', competitor: '✗ (part of password manager)' },
  { name: 'Open Source', twofac: '✓', competitor: '✓' },
  { name: 'Free', twofac: '✓', competitor: 'Partial' },
  { name: 'Desktop App', twofac: '✓', competitor: '✓' },
  { name: 'CLI App', twofac: '✓', competitor: '✗' },
  { name: 'Wear OS', twofac: '✓', competitor: '✗' },
  { name: 'watchOS', twofac: '✓', competitor: '✗' },
  { name: 'Browser Extension', twofac: '✓', competitor: '✓' },
  { name: 'Lightweight', twofac: '✓', competitor: '✗' },
  { name: 'No Account Required', twofac: '✓', competitor: '✗' },
]
</script>

<template>
  <MainLayout>
    <!-- Hero -->
    <section class="bg-gradient-to-br from-primary-50 to-white dark:from-secondary-900 dark:to-secondary-950 py-16 sm:py-20">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
        <h1 class="text-3xl sm:text-4xl lg:text-5xl font-bold text-secondary-900 dark:text-white">
          TwoFac vs <span class="text-primary-600 dark:text-primary-400">Bitwarden Authenticator</span>
        </h1>
        <p class="mt-4 text-lg text-secondary-600 dark:text-secondary-400 max-w-2xl mx-auto">
          Bitwarden bundles 2FA into its password manager, but TwoFac is purpose-built for two-factor authentication —
          lightweight, free, and requiring no account to get started.
        </p>
      </div>
    </section>

    <!-- Comparison Table -->
    <section class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-12 sm:py-16">
      <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-6">Feature Comparison</h2>
      <ComparisonTable :features="features" competitor-name="Bitwarden" />
    </section>

    <!-- Why Choose TwoFac -->
    <section class="bg-secondary-50 dark:bg-secondary-900 py-12 sm:py-16">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-6">Why Choose TwoFac?</h2>
        <div class="grid gap-6 sm:grid-cols-2">
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2">🎯 Purpose-Built</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              TwoFac is a dedicated 2FA app, not an add-on to a password manager. It does one thing and does it exceptionally well.
            </p>
          </div>
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2">🆓 Completely Free</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              No subscriptions, no premium tiers. All features are available for free, forever.
            </p>
          </div>
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2">🪶 Lightweight</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              No bloated password manager bundled in. TwoFac is fast, lightweight, and focused.
            </p>
          </div>
          <div class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm">
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2">🔓 No Account Needed</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">
              Start using TwoFac immediately — no sign-up, no account creation, no email verification required.
            </p>
          </div>
        </div>
      </div>
    </section>

    <!-- CTA -->
    <section class="py-12 sm:py-16">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
        <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-4">Try TwoFac today</h2>
        <p class="text-secondary-600 dark:text-secondary-400 mb-6">
          A dedicated 2FA app that works on every platform. No account required.
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
