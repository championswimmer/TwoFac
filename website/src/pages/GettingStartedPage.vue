<script setup lang="ts">
import MainLayout from '../layouts/MainLayout.vue'
import { useSEO } from '../composables/useSEO'

useSEO({
  title: 'Getting Started',
  description:
    'Get started with TwoFac in minutes. Install the app, add your accounts, import from other authenticators, and set up biometric lock and encrypted backups.',
  canonicalPath: '/getting-started',
})

const steps = [
  {
    number: 1,
    title: 'Install TwoFac',
    description:
      'Choose the platform that suits you best — Android, iOS, desktop, web, or CLI — and install TwoFac. All apps are free and available from our download page or GitHub Releases.',
  },
  {
    number: 2,
    title: 'Add Your First Account',
    description:
      'Open TwoFac and tap the add button. Scan the QR code shown by your service (e.g. GitHub, Google, AWS) or enter the secret key manually. Your 2FA codes will start generating immediately.',
  },
  {
    number: 3,
    title: 'Import Existing Accounts',
    description:
      'Already using another authenticator? TwoFac can import your accounts from Authy, 2FAS, Ente Auth, and Google Authenticator in one click. Check out our blog for step-by-step migration guides.',
  },
  {
    number: 4,
    title: 'Set Up Biometric Lock',
    description:
      'Protect your 2FA codes with biometric authentication. Enable fingerprint or face unlock on Android and iOS so only you can access your codes, even if someone picks up your device.',
  },
  {
    number: 5,
    title: 'Enable Backups',
    description:
      'Never lose your codes again. Set up encrypted backups so you can restore your accounts on a new device. Backups are encrypted with a passkey before they leave your device.',
  },
  {
    number: 6,
    title: 'Install on More Devices',
    description:
      'Extend TwoFac to your other devices — add the Wear OS or watchOS companion for wrist access, install the desktop app for quick logins at your workstation, or use the CLI for terminal workflows.',
  },
]
</script>

<template>
  <MainLayout>
    <!-- Header -->
    <section class="bg-primary-600 dark:bg-primary-800 py-16 sm:py-24">
      <div class="mx-auto max-w-7xl px-4 text-center sm:px-6 lg:px-8">
        <h1 class="text-4xl font-bold tracking-tight text-white sm:text-5xl">
          Getting Started with TwoFac
        </h1>
        <p class="mx-auto mt-4 max-w-2xl text-lg text-primary-100">
          Go from zero to fully protected in just a few minutes.
        </p>
      </div>
    </section>

    <div class="mx-auto max-w-3xl px-4 py-16 sm:px-6 sm:py-24 lg:px-8">
      <ol class="space-y-12">
        <li
          v-for="step in steps"
          :key="step.number"
          class="relative flex gap-6"
        >
          <!-- Step Number -->
          <div
            class="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-primary-600 text-lg font-bold text-white"
          >
            {{ step.number }}
          </div>

          <!-- Step Content -->
          <div class="pt-1">
            <h2
              class="text-xl font-semibold text-secondary-900 dark:text-white"
            >
              {{ step.title }}
            </h2>
            <p class="mt-2 text-secondary-600 dark:text-secondary-400 leading-relaxed">
              {{ step.description }}
            </p>
            <router-link
              v-if="step.number === 1"
              to="/download"
              class="mt-3 inline-flex text-sm font-medium text-primary-600 dark:text-primary-400 hover:underline"
            >
              Go to Downloads →
            </router-link>
            <router-link
              v-if="step.number === 3"
              to="/blog"
              class="mt-3 inline-flex text-sm font-medium text-primary-600 dark:text-primary-400 hover:underline"
            >
              Read migration guides →
            </router-link>
          </div>
        </li>
      </ol>

      <!-- Next Steps -->
      <div
        class="mt-16 rounded-xl border border-secondary-200 dark:border-secondary-700 bg-secondary-50 dark:bg-secondary-900 p-6"
      >
        <h3 class="text-lg font-semibold text-secondary-900 dark:text-white">
          What's next?
        </h3>
        <p class="mt-2 text-secondary-600 dark:text-secondary-400">
          Explore the full list of
          <router-link
            to="/features"
            class="font-medium text-primary-600 dark:text-primary-400 hover:underline"
          >features</router-link>,
          read our
          <router-link
            to="/faq"
            class="font-medium text-primary-600 dark:text-primary-400 hover:underline"
          >FAQ</router-link>,
          or check out the
          <router-link
            to="/blog"
            class="font-medium text-primary-600 dark:text-primary-400 hover:underline"
          >blog</router-link>
          for tips and migration guides.
        </p>
      </div>
    </div>
  </MainLayout>
</template>
