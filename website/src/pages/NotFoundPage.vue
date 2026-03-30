<script setup lang="ts">
import MainLayout from '../layouts/MainLayout.vue'
import { useSEO } from '../composables/useSEO'

useSEO({
  title: 'Page Not Found',
  description: 'The page you are looking for does not exist.',
  canonicalPath: '/404',
})
</script>

<template>
  <MainLayout>
    <section class="flex min-h-[60vh] items-center justify-center px-4">
      <div class="text-center">
        <p class="text-6xl font-bold text-primary-500">404</p>
        <h1 class="mt-4 text-3xl font-bold text-secondary-900 dark:text-white">
          Page not found
        </h1>
        <p class="mt-3 text-lg text-secondary-500 dark:text-secondary-400">
          Sorry, the page you're looking for doesn't exist or has been moved.
        </p>
        <RouterLink
          to="/"
          class="mt-8 inline-flex items-center gap-2 rounded-lg bg-primary-600 px-6 py-3 text-sm font-semibold text-white transition-colors hover:bg-primary-700"
        >
          ← Back to Home
        </RouterLink>
      </div>
    </section>
  </MainLayout>
</template>
