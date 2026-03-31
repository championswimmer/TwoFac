<script setup lang="ts">
import { RouterLink } from 'vue-router'
import MainLayout from '../layouts/MainLayout.vue'
import ComparisonTable from './ComparisonTable.vue'
import FAQItem from './FAQItem.vue'

export interface GlanceStat {
  value: string
  label: string
  note: string
}

export interface FeatureRow {
  name: string
  twofac: string
  competitor: string
}

export interface WhyChooseCard {
  icon: string
  title: string
  text: string
}

export interface FAQEntry {
  question: string
  answer: string
}

defineProps<{
  /** Competitor display name, e.g. "Google Authenticator" */
  competitorName: string
  /** Hero paragraph describing the comparison context */
  heroDescription: string
  /** Exactly 3 highlight stats for the At-a-Glance grid */
  glanceStats: [GlanceStat, GlanceStat, GlanceStat]
  /** Optional subtitle under the comparison table heading */
  comparisonSubtitle?: string
  /** Feature rows for the comparison table */
  features: FeatureRow[]
  /** 5-6 reason cards for the "Why Choose TwoFac" section */
  whyChooseCards: WhyChooseCard[]
  /** FAQ entries rendered with the animated FAQItem component */
  faqs: FAQEntry[]
  /** CTA section heading */
  ctaHeading: string
  /** CTA section body paragraph */
  ctaBody: string
  /** Secondary CTA route (e.g. /getting-started, /features) */
  guideLink: string
  /** Secondary CTA link text */
  guideLinkText: string
}>()
</script>

<template>
  <MainLayout>
    <!-- Hero -->
    <section class="bg-gradient-to-br from-primary-50 to-white dark:from-secondary-900 dark:to-secondary-950 py-16 sm:py-24">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
        <h1 class="text-3xl sm:text-4xl lg:text-5xl font-bold text-secondary-900 dark:text-white">
          TwoFac vs <span class="text-primary-600 dark:text-primary-400">{{ competitorName }}</span>
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
            href="#comparison-table"
            class="inline-flex items-center justify-center px-6 py-3 rounded-lg border border-secondary-300 dark:border-secondary-600 text-secondary-700 dark:text-secondary-300 font-medium hover:bg-secondary-50 dark:hover:bg-secondary-800 transition-colors"
          >
            See Full Comparison ↓
          </a>
        </div>
      </div>
    </section>

    <!-- At a Glance -->
    <section class="py-12 sm:py-16">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-8 text-center">
          TwoFac vs {{ competitorName }} at a Glance
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

    <!-- "What is <Competitor>" intro section (per-page slot) -->
    <slot name="intro" />

    <!-- Comparison Table -->
    <section id="comparison-table" class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-12 sm:py-16">
      <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-2">Feature-by-Feature Comparison</h2>
      <p class="text-secondary-600 dark:text-secondary-400 mb-6">
        {{ comparisonSubtitle ?? 'A detailed breakdown of every feature that matters.' }}
      </p>
      <ComparisonTable :features="features" :competitor-name="competitorName" />
    </section>

    <!-- Platform / Security / Pricing and any other detail sections (per-page slot) -->
    <slot name="details" />

    <!-- Why Choose TwoFac -->
    <section class="bg-secondary-50 dark:bg-secondary-900 py-12 sm:py-16">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        <h2 class="text-2xl font-bold text-secondary-900 dark:text-white mb-8">
          Why Choose TwoFac Over {{ competitorName }}?
        </h2>
        <div class="grid gap-6 sm:grid-cols-2">
          <div
            v-for="card in whyChooseCards"
            :key="card.title"
            class="rounded-xl bg-white dark:bg-secondary-800 p-6 shadow-sm border border-secondary-200 dark:border-secondary-700"
          >
            <h3 class="font-semibold text-secondary-900 dark:text-white mb-2">{{ card.icon }} {{ card.title }}</h3>
            <p class="text-sm text-secondary-600 dark:text-secondary-400">{{ card.text }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- FAQ -->
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

    <!-- CTA -->
    <section class="bg-secondary-50 dark:bg-secondary-900 py-12 sm:py-16">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
        <h2 class="text-2xl sm:text-3xl font-bold text-secondary-900 dark:text-white mb-4">{{ ctaHeading }}</h2>
        <p class="text-secondary-600 dark:text-secondary-400 mb-8 max-w-2xl mx-auto">{{ ctaBody }}</p>
        <div class="flex flex-col sm:flex-row gap-4 justify-center">
          <RouterLink
            to="/download"
            class="inline-flex items-center justify-center px-8 py-3 rounded-lg bg-primary-600 text-white font-medium hover:bg-primary-700 transition-colors shadow-sm"
          >
            Download TwoFac Free
          </RouterLink>
          <RouterLink
            :to="guideLink"
            class="inline-flex items-center justify-center px-8 py-3 rounded-lg border border-secondary-300 dark:border-secondary-600 text-secondary-700 dark:text-secondary-300 font-medium hover:bg-secondary-50 dark:hover:bg-secondary-800 transition-colors"
          >
            {{ guideLinkText }}
          </RouterLink>
        </div>
      </div>
    </section>
  </MainLayout>
</template>
