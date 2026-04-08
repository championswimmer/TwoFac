<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink } from 'vue-router'

const mobileMenuOpen = ref(false)

const navLinks = [
  { name: 'Features', to: '/features' },
  { name: 'Screenshots', to: '/screenshots' },
  { name: 'Download', to: '/download' },
  { name: 'Blog', to: '/blog' },
  { name: 'FAQ', to: '/faq' },
]
</script>

<template>
  <header
    class="sticky top-0 z-50 w-full border-b border-secondary-200 bg-secondary-50 dark:border-secondary-800 dark:bg-secondary-950"
  >
    <nav class="mx-auto flex max-w-7xl items-center justify-between px-4 py-3 sm:px-6 lg:px-8">
      <!-- Logo -->
      <RouterLink to="/" class="flex items-center gap-2.5">
        <img src="/twofac-logo.svg" alt="TwoFac logo" class="h-8 w-8" />
        <span class="text-xl font-bold tracking-tight text-secondary-900 dark:text-white">
          TwoFac
        </span>
      </RouterLink>

      <!-- Desktop nav links -->
      <div class="hidden items-center gap-1 md:flex">
        <RouterLink
          v-for="link in navLinks"
          :key="link.to"
          :to="link.to"
          class="rounded-lg px-3.5 py-2 text-sm font-medium text-secondary-600 transition-colors hover:bg-secondary-100 hover:text-secondary-900 dark:text-secondary-400 dark:hover:bg-secondary-800 dark:hover:text-white [&.router-link-active]:text-primary-600 dark:[&.router-link-active]:text-primary-400"
        >
          {{ link.name }}
        </RouterLink>
      </div>

      <!-- GitHub button (desktop) -->
      <div class="hidden items-center gap-3 md:flex">
        <a
          href="https://github.com/championswimmer/TwoFac"
          target="_blank"
          rel="noopener noreferrer"
          class="inline-flex items-center gap-2 rounded-lg border border-secondary-300 bg-secondary-50 px-4 py-2 text-sm font-medium text-secondary-700 transition-colors hover:bg-secondary-100 dark:border-secondary-700 dark:bg-secondary-900 dark:text-secondary-300 dark:hover:bg-secondary-800"
        >
          <svg class="h-4 w-4" fill="currentColor" viewBox="0 0 24 24" aria-hidden="true">
            <path
              fill-rule="evenodd"
              d="M12 2C6.477 2 2 6.484 2 12.017c0 4.425 2.865 8.18 6.839 9.504.5.092.682-.217.682-.483 0-.237-.008-.868-.013-1.703-2.782.605-3.369-1.343-3.369-1.343-.454-1.158-1.11-1.466-1.11-1.466-.908-.62.069-.608.069-.608 1.003.07 1.531 1.032 1.531 1.032.892 1.53 2.341 1.088 2.91.832.092-.647.35-1.088.636-1.338-2.22-.253-4.555-1.113-4.555-4.951 0-1.093.39-1.988 1.029-2.688-.103-.253-.446-1.272.098-2.65 0 0 .84-.27 2.75 1.026A9.564 9.564 0 0112 6.844c.85.004 1.705.115 2.504.337 1.909-1.296 2.747-1.027 2.747-1.027.546 1.379.202 2.398.1 2.651.64.7 1.028 1.595 1.028 2.688 0 3.848-2.339 4.695-4.566 4.943.359.309.678.92.678 1.855 0 1.338-.012 2.419-.012 2.747 0 .268.18.58.688.482A10.019 10.019 0 0022 12.017C22 6.484 17.522 2 12 2z"
              clip-rule="evenodd"
            />
          </svg>
          GitHub
        </a>
      </div>

      <!-- Mobile menu button -->
      <button
        type="button"
        class="inline-flex items-center justify-center rounded-lg p-2 text-secondary-500 transition-colors hover:bg-secondary-100 hover:text-secondary-700 md:hidden dark:text-secondary-400 dark:hover:bg-secondary-800 dark:hover:text-white"
        :aria-expanded="mobileMenuOpen"
        aria-label="Toggle navigation menu"
        @click="mobileMenuOpen = !mobileMenuOpen"
      >
        <!-- Hamburger icon -->
        <svg
          v-if="!mobileMenuOpen"
          class="h-6 w-6"
          fill="none"
          viewBox="0 0 24 24"
          stroke-width="1.5"
          stroke="currentColor"
        >
          <path stroke-linecap="round" stroke-linejoin="round" d="M3.75 9h16.5m-16.5 6.75h16.5" />
        </svg>
        <!-- Close icon -->
        <svg
          v-else
          class="h-6 w-6"
          fill="none"
          viewBox="0 0 24 24"
          stroke-width="1.5"
          stroke="currentColor"
        >
          <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
        </svg>
      </button>
    </nav>

    <!-- Mobile menu dropdown -->
    <Transition
      enter-active-class="transition duration-200 ease-out"
      enter-from-class="opacity-0 -translate-y-1"
      enter-to-class="opacity-100 translate-y-0"
      leave-active-class="transition duration-150 ease-in"
      leave-from-class="opacity-100 translate-y-0"
      leave-to-class="opacity-0 -translate-y-1"
    >
      <div
        v-if="mobileMenuOpen"
        class="border-t border-secondary-200 bg-secondary-50 md:hidden dark:border-secondary-800 dark:bg-secondary-950"
      >
        <div class="space-y-1 px-4 py-3">
          <RouterLink
            v-for="link in navLinks"
            :key="link.to"
            :to="link.to"
            class="block rounded-lg px-3 py-2.5 text-base font-medium text-secondary-600 transition-colors hover:bg-secondary-100 hover:text-secondary-900 dark:text-secondary-400 dark:hover:bg-secondary-800 dark:hover:text-white [&.router-link-active]:text-primary-600 dark:[&.router-link-active]:text-primary-400"
            @click="mobileMenuOpen = false"
          >
            {{ link.name }}
          </RouterLink>
          <a
            href="https://github.com/championswimmer/TwoFac"
            target="_blank"
            rel="noopener noreferrer"
            class="flex items-center gap-2 rounded-lg px-3 py-2.5 text-base font-medium text-secondary-600 transition-colors hover:bg-secondary-100 hover:text-secondary-900 dark:text-secondary-400 dark:hover:bg-secondary-800 dark:hover:text-white"
          >
            <svg class="h-5 w-5" fill="currentColor" viewBox="0 0 24 24" aria-hidden="true">
              <path
                fill-rule="evenodd"
                d="M12 2C6.477 2 2 6.484 2 12.017c0 4.425 2.865 8.18 6.839 9.504.5.092.682-.217.682-.483 0-.237-.008-.868-.013-1.703-2.782.605-3.369-1.343-3.369-1.343-.454-1.158-1.11-1.466-1.11-1.466-.908-.62.069-.608.069-.608 1.003.07 1.531 1.032 1.531 1.032.892 1.53 2.341 1.088 2.91.832.092-.647.35-1.088.636-1.338-2.22-.253-4.555-1.113-4.555-4.951 0-1.093.39-1.988 1.029-2.688-.103-.253-.446-1.272.098-2.65 0 0 .84-.27 2.75 1.026A9.564 9.564 0 0112 6.844c.85.004 1.705.115 2.504.337 1.909-1.296 2.747-1.027 2.747-1.027.546 1.379.202 2.398.1 2.651.64.7 1.028 1.595 1.028 2.688 0 3.848-2.339 4.695-4.566 4.943.359.309.678.92.678 1.855 0 1.338-.012 2.419-.012 2.747 0 .268.18.58.688.482A10.019 10.019 0 0022 12.017C22 6.484 17.522 2 12 2z"
                clip-rule="evenodd"
              />
            </svg>
            GitHub
          </a>
        </div>
      </div>
    </Transition>
  </header>
</template>
