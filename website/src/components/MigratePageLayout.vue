<script setup lang="ts">
import { RouterLink } from 'vue-router'
import MainLayout from '../layouts/MainLayout.vue'
import { useHead } from '@unhead/vue'

export interface HowToStep {
  name: string
  text: string
}

const props = defineProps<{
  /** App name being migrated from, e.g. "Google Authenticator" */
  appName: string
  /** Hero paragraph describing the migration context */
  heroDescription: string
  /** Array of steps for the HowTo JSON-LD schema */
  howToSteps?: HowToStep[]
}>()

useHead(() => ({
  script: [
    {
      type: 'application/ld+json',
      innerHTML: JSON.stringify({
        "@context": "https://schema.org",
        "@type": "BreadcrumbList",
        "itemListElement": [
          {
            "@type": "ListItem",
            "position": 1,
            "name": "Home",
            "item": "https://twofac.app/"
          },
          {
            "@type": "ListItem",
            "position": 2,
            "name": "Migrate",
            "item": "https://twofac.app/migrate"
          },
          {
            "@type": "ListItem",
            "position": 3,
            "name": `Migrate from ${props.appName}`,
            "item": `https://twofac.app/migrate/${props.appName.toLowerCase().replace(/\s+/g, '-')}`
          }
        ]
      })
    },
    ...(props.howToSteps?.length ? [{
      type: 'application/ld+json',
      innerHTML: JSON.stringify({
        "@context": "https://schema.org",
        "@type": "HowTo",
        "name": `How to Migrate from ${props.appName} to TwoFac`,
        "description": props.heroDescription,
        "step": props.howToSteps.map((step) => ({
          "@type": "HowToStep",
          "name": step.name,
          "text": step.text
        }))
      })
    }] : [])
  ]
}))
</script>

<template>
  <MainLayout>
    <!-- Hero -->
    <section class="bg-gradient-to-br from-primary-50 to-white dark:from-secondary-900 dark:to-secondary-950 py-16 sm:py-24">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
        <h1 class="text-3xl sm:text-4xl lg:text-5xl font-bold text-secondary-900 dark:text-white">
          Migrate from <span class="text-primary-600 dark:text-primary-400">{{ appName }}</span> to TwoFac
        </h1>
        <p class="mt-6 text-lg sm:text-xl text-secondary-600 dark:text-secondary-400 max-w-3xl mx-auto leading-relaxed">
          {{ heroDescription }}
        </p>
      </div>
    </section>

    <!-- Migration Steps -->
    <section class="py-12 sm:py-16">
      <div class="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="prose prose-secondary dark:prose-invert max-w-none">
          <slot />
        </div>
      </div>
    </section>

    <!-- CTA -->
    <section class="bg-secondary-50 dark:bg-secondary-900 py-12 sm:py-16">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
        <h2 class="text-2xl sm:text-3xl font-bold text-secondary-900 dark:text-white mb-4">Ready to complete your migration?</h2>
        <p class="text-secondary-600 dark:text-secondary-400 mb-8 max-w-2xl mx-auto">
          Download TwoFac free on all your devices and enjoy an open-source, cross-platform 2FA experience.
        </p>
        <div class="flex flex-col sm:flex-row gap-4 justify-center">
          <RouterLink
            to="/download"
            class="inline-flex items-center justify-center px-8 py-3 rounded-lg bg-primary-600 text-white font-medium hover:bg-primary-700 transition-colors shadow-sm"
          >
            Download TwoFac
          </RouterLink>
          <RouterLink
            :to="`/compare/${appName.toLowerCase().replace(' ', '-')}`"
            class="inline-flex items-center justify-center px-8 py-3 rounded-lg border border-secondary-300 dark:border-secondary-600 text-secondary-700 dark:text-secondary-300 font-medium hover:bg-secondary-50 dark:hover:bg-secondary-800 transition-colors"
          >
            See Full Comparison
          </RouterLink>
        </div>
      </div>
    </section>
  </MainLayout>
</template>
