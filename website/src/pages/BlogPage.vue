<script setup lang="ts">
import { RouterLink } from 'vue-router'
import MainLayout from '../layouts/MainLayout.vue'
import { useSEO } from '../composables/useSEO'
import blogs from '../data/blogs.json'

useSEO({
  title: 'Blog',
  description: 'Read the latest articles about TwoFac, two-factor authentication, Kotlin Multiplatform, and security best practices.',
  canonicalPath: '/blog',
})

function formatDate(dateString: string): string {
  return new Date(dateString).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  })
}
</script>

<template>
  <MainLayout>
    <section class="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-12 sm:py-16">
      <h1 class="text-3xl sm:text-4xl font-bold text-secondary-900 dark:text-white mb-4">Blog</h1>
      <p class="text-lg text-secondary-600 dark:text-secondary-400 mb-10">
        Thoughts on 2FA, security, and building cross-platform apps with Kotlin Multiplatform.
      </p>

      <div class="grid grid-cols-1 md:grid-cols-2 gap-6 lg:gap-8">
        <RouterLink
          v-for="post in blogs"
          :key="post.slug"
          :to="`/blog/${post.slug}`"
          class="group block rounded-xl border border-secondary-200 dark:border-secondary-700 bg-white dark:bg-secondary-900 overflow-hidden hover:shadow-lg hover:border-primary-300 dark:hover:border-primary-600 transition-all duration-200"
        >
          <!-- Cover image -->
          <div v-if="post.cover" class="aspect-video overflow-hidden bg-secondary-100 dark:bg-secondary-800">
            <img
              :src="post.cover"
              :alt="post.title"
              class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
            />
          </div>

          <div class="p-5 sm:p-6">
            <!-- Date -->
            <time
              :datetime="post.datePublished"
              class="text-xs font-medium text-secondary-500 dark:text-secondary-400 uppercase tracking-wide"
            >
              {{ formatDate(post.datePublished) }}
            </time>

            <!-- Title -->
            <h2 class="mt-2 text-lg sm:text-xl font-semibold text-secondary-900 dark:text-white group-hover:text-primary-600 dark:group-hover:text-primary-400 transition-colors line-clamp-2">
              {{ post.title }}
            </h2>

            <!-- Excerpt -->
            <p class="mt-2 text-sm text-secondary-600 dark:text-secondary-400 line-clamp-3">
              {{ post.excerpt }}
            </p>

            <!-- Tags -->
            <div class="mt-4 flex flex-wrap gap-2">
              <span
                v-for="tag in post.tags"
                :key="tag"
                class="inline-block px-2.5 py-0.5 text-xs font-medium rounded-full bg-primary-50 dark:bg-primary-900/30 text-primary-700 dark:text-primary-300"
              >
                {{ tag }}
              </span>
            </div>

            <!-- Read more -->
            <span class="mt-4 inline-flex items-center text-sm font-medium text-primary-600 dark:text-primary-400 group-hover:underline">
              Read more <i class="fa-solid fa-arrow-right-long ml-1" aria-hidden="true" />
            </span>
          </div>
        </RouterLink>
      </div>
    </section>
  </MainLayout>
</template>
