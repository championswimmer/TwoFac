<script setup lang="ts">
import { RouterLink } from 'vue-router'
import { useHead } from '@unhead/vue'
import MainLayout from '../layouts/MainLayout.vue'
import FAQItem from './FAQItem.vue'

export interface ThreeWayGlanceStat {
  value: string
  label: string
  note: string
}

export interface ThreeWayFeatureRow {
  name: string
  appOne: string
  appTwo: string
  twofac: string
}

export interface DecisionCard {
  icon: string
  title: string
  text: string
}

export interface ThreeWayFAQEntry {
  question: string
  answer: string
}

const props = defineProps<{
  appOneName: string
  appTwoName: string
  canonicalPath: string
  heroDescription: string
  glanceStats: [ThreeWayGlanceStat, ThreeWayGlanceStat, ThreeWayGlanceStat]
  features: ThreeWayFeatureRow[]
  decisionCards: DecisionCard[]
  faqs: ThreeWayFAQEntry[]
  ctaHeading: string
  ctaBody: string
}>()

useHead(() => ({
  script: [
    {
      type: 'application/ld+json',
      innerHTML: JSON.stringify({
        "@context": "https://schema.org",
        "@type": "FAQPage",
        "mainEntity": props.faqs.map(faq => ({
          "@type": "Question",
          "name": faq.question,
          "acceptedAnswer": {
            "@type": "Answer",
            "text": faq.answer
          }
        }))
      })
    },
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
            "name": "Compare",
            "item": "https://twofac.app/compare"
          },
          {
            "@type": "ListItem",
            "position": 3,
            "name": `${props.appOneName} vs ${props.appTwoName} vs TwoFac`,
            "item": `https://twofac.app${props.canonicalPath}`
          }
        ]
      })
    }
  ]
}))
</script>

<template>
  <MainLayout>
    <section class="bg-gradient-to-br from-primary-50 to-white dark:from-secondary-900 dark:to-secondary-950 py-16 sm:py-24">
      <div class="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
        <h1 class="text-3xl sm:text-4xl lg:text-5xl font-bold text-secondary-900 dark:text-white">
          {{ appOneName }} vs {{ appTwoName }} vs <span class="text-primary-600 dark:text-primary-400">TwoFac</span>
        </h1>
        <p class="mt-6 text-lg sm:text-xl text-secondary-600 dark:text-secondary-400 max-w-3xl mx-auto leading-relaxed">
          {{ heroDescription }}
        </p>
        <div class="mt-8 flex flex-col sm:flex-row gap-4 justify-center">
          <RouterLink
            to="/download"
            class="inline-flex items-center justify-center px-6 py-3 rounded-lg bg-primary-600 text-white font-medium hover:bg-primary-700 transition-colors shadow-sm"
          >
            Download TwoFac Free
          </RouterLink>
          <a
            href="#three-way-comparison-table"
            class="inline-flex items-center justify-center px-6 py-3 rounded-lg border border-secondary-300 dark:border-secondary-600 text-secondary-700 dark:text-secondary-300 font-medium hover:bg-secondary-50 dark:hover:bg-secondary-800 transition-colors"
          >
            Compare All Three ↓
          </a>
        </div>
      </div>
    </section>

    <section class="py-12 sm:py-16">
      <div class="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8">
        <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-8 text-center">
          {{ appOneName }} vs {{ appTwoName }} vs TwoFac at a Glance
        </h2>
        <div class="grid gap-6 sm:grid-cols-3">
          <div
            v-for="stat in glanceStats"
            :key="stat.label"
            class="rounded-xl bg-primary-50 dark:bg-primary-900/20 border border-primary-200 dark:border-primary-800 p-6 text-center"
          >
            <div class="text-3xl font-bold text-primary-600 dark:text-primary-400">{{ stat.value }}</div>
            <div class="mt-2 text-sm font-medium text-secondary-700 dark:text-secondary-300">{{ stat.label }}</div>
            <div class="mt-1 text-xs text-secondary-500 dark:text-secondary-400">{{ stat.note }}</div>
          </div>
        </div>
      </div>
    </section>

    <slot name="intro" />

    <section id="three-way-comparison-table" class="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-12 sm:py-16">
      <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-2">Three-Way Feature Comparison</h2>
      <p class="text-secondary-600 dark:text-secondary-400 mb-6">
        A side-by-side breakdown of the features that matter when choosing a modern authenticator.
      </p>
      <div class="overflow-x-auto rounded-xl border border-secondary-200 dark:border-secondary-800">
        <table class="w-full min-w-[760px] text-sm">
          <thead>
            <tr class="border-b border-secondary-200 bg-secondary-50 dark:border-secondary-800 dark:bg-secondary-900">
              <th class="px-6 py-4 text-left font-semibold text-secondary-900 dark:text-white">Feature</th>
              <th class="px-6 py-4 text-center font-semibold text-secondary-600 dark:text-secondary-400">{{ appOneName }}</th>
              <th class="px-6 py-4 text-center font-semibold text-secondary-600 dark:text-secondary-400">{{ appTwoName }}</th>
              <th class="px-6 py-4 text-center font-semibold text-primary-600 dark:text-primary-400">TwoFac</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="(feature, index) in features"
              :key="feature.name"
              :class="[
                'border-b border-secondary-100 transition-colors hover:bg-secondary-50 dark:border-secondary-800/50 dark:hover:bg-secondary-800/30',
                index % 2 === 0 ? 'bg-white dark:bg-secondary-950' : 'bg-secondary-50/50 dark:bg-secondary-900/30',
              ]"
            >
              <td class="px-6 py-3.5 font-medium text-secondary-700 dark:text-secondary-300">{{ feature.name }}</td>
              <td class="px-6 py-3.5 text-center text-secondary-600 dark:text-secondary-400">{{ feature.appOne }}</td>
              <td class="px-6 py-3.5 text-center text-secondary-600 dark:text-secondary-400">{{ feature.appTwo }}</td>
              <td class="px-6 py-3.5 text-center font-medium text-secondary-900 dark:text-white">{{ feature.twofac }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <slot name="details" />

    <section class="bg-secondary-50 dark:bg-secondary-900 py-12 sm:py-16">
      <div class="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8">
        <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-8">
          How to Choose Between {{ appOneName }}, {{ appTwoName }}, and TwoFac
        </h2>
        <div class="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
          <div
            v-for="card in decisionCards"
            :key="card.title"
            class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm border border-secondary-200 dark:border-secondary-700"
          >
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2 flex items-center gap-2">
              <i :class="card.icon" class="text-primary-600 dark:text-primary-400 w-5 text-center"></i>
              <span>{{ card.title }}</span>
            </h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">{{ card.text }}</p>
          </div>
        </div>
      </div>
    </section>

    <section class="py-12 sm:py-16">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-8">Frequently Asked Questions</h2>
        <div class="divide-y divide-secondary-200 dark:divide-secondary-800 rounded-xl border border-secondary-200 dark:border-secondary-800 overflow-hidden">
          <FAQItem
            v-for="faq in faqs"
            :key="faq.question"
            :question="faq.question"
            :answer="faq.answer"
          />
        </div>
      </div>
    </section>

    <section class="bg-secondary-50 dark:bg-secondary-900 py-12 sm:py-16">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
        <h2 class="text-2xl sm:text-3xl font-bold text-secondary-900 dark:text-white mb-4">{{ ctaHeading }}</h2>
        <p class="text-secondary-600 dark:text-secondary-400 mb-8 max-w-2xl mx-auto">{{ ctaBody }}</p>
        <RouterLink
          to="/download"
          class="inline-flex items-center justify-center px-8 py-3 rounded-lg bg-primary-600 text-white font-medium hover:bg-primary-700 transition-colors shadow-sm"
        >
          Download TwoFac Free
        </RouterLink>
      </div>
    </section>
  </MainLayout>
</template>
