<script setup lang="ts">
import { RouterLink } from 'vue-router'

const productLinks = [
  { name: 'Features', to: '/features' },
  { name: 'Download', to: '/download' },
  { name: 'Getting Started', to: '/getting-started' },
]

const resourceRouterLinks = [
  { name: 'Blog', to: '/blog' },
  { name: 'FAQ', to: '/faq' },
]

const resourceExternalLinks = [
  { name: 'Documentation', href: '#' },
]

const compareLinks = [
  { name: 'vs Google Authenticator', to: '/compare/google-authenticator' },
  { name: 'vs 2FAS', to: '/compare/2fas' },
  { name: 'vs Ente Auth', to: '/compare/ente-auth' },
]

const communityLinks = [
  { name: 'GitHub', href: '#' },
  { name: 'Discord', href: '#' },
]
</script>

<template>
  <footer class="bg-secondary-900 text-secondary-300 dark:bg-secondary-950">
    <!-- Main footer grid -->
    <div class="mx-auto max-w-7xl px-4 py-12 sm:px-6 lg:px-8 lg:py-16">
      <div class="grid grid-cols-2 gap-8 md:grid-cols-4">
        <!-- Product -->
        <div>
          <h3 class="text-sm font-semibold uppercase tracking-wider text-white">Product</h3>
          <ul class="mt-4 space-y-3">
            <li v-for="link in productLinks" :key="link.to">
              <RouterLink
                :to="link.to"
                class="text-sm text-secondary-400 transition-colors hover:text-white"
              >
                {{ link.name }}
              </RouterLink>
            </li>
          </ul>
        </div>

        <!-- Resources -->
        <div>
          <h3 class="text-sm font-semibold uppercase tracking-wider text-white">Resources</h3>
          <ul class="mt-4 space-y-3">
            <li v-for="link in resourceRouterLinks" :key="link.name">
              <RouterLink
                :to="link.to"
                class="text-sm text-secondary-400 transition-colors hover:text-white"
              >
                {{ link.name }}
              </RouterLink>
            </li>
            <li v-for="link in resourceExternalLinks" :key="link.name">
              <a
                :href="link.href"
                class="text-sm text-secondary-400 transition-colors hover:text-white"
              >
                {{ link.name }}
              </a>
            </li>
          </ul>
        </div>

        <!-- Compare -->
        <div>
          <h3 class="text-sm font-semibold uppercase tracking-wider text-white">Compare</h3>
          <ul class="mt-4 space-y-3">
            <li v-for="link in compareLinks" :key="link.to">
              <RouterLink
                :to="link.to"
                class="text-sm text-secondary-400 transition-colors hover:text-white"
              >
                {{ link.name }}
              </RouterLink>
            </li>
          </ul>
        </div>

        <!-- Community -->
        <div>
          <h3 class="text-sm font-semibold uppercase tracking-wider text-white">Community</h3>
          <ul class="mt-4 space-y-3">
            <li v-for="link in communityLinks" :key="link.name">
              <a
                :href="link.href"
                target="_blank"
                rel="noopener noreferrer"
                class="text-sm text-secondary-400 transition-colors hover:text-white"
              >
                {{ link.name }}
              </a>
            </li>
          </ul>
        </div>
      </div>
    </div>

    <!-- Bottom bar -->
    <div class="border-t border-secondary-800">
      <div
        class="mx-auto flex max-w-7xl flex-col items-center justify-between gap-4 px-4 py-6 sm:flex-row sm:px-6 lg:px-8"
      >
        <div class="flex items-center gap-2.5">
          <img src="/twofac-logo.svg" alt="TwoFac logo" class="h-6 w-6" />
          <p class="text-sm text-secondary-500">
            &copy; {{ new Date().getFullYear() }} TwoFac. Open source under MIT License.
          </p>
        </div>

        <!-- Social links -->
        <div class="flex items-center gap-4">
          <a
            href="#"
            target="_blank"
            rel="noopener noreferrer"
            class="text-secondary-500 transition-colors hover:text-white"
            aria-label="GitHub"
          >
            <svg class="h-5 w-5" fill="currentColor" viewBox="0 0 24 24" aria-hidden="true">
              <path
                fill-rule="evenodd"
                d="M12 2C6.477 2 2 6.484 2 12.017c0 4.425 2.865 8.18 6.839 9.504.5.092.682-.217.682-.483 0-.237-.008-.868-.013-1.703-2.782.605-3.369-1.343-3.369-1.343-.454-1.158-1.11-1.466-1.11-1.466-.908-.62.069-.608.069-.608 1.003.07 1.531 1.032 1.531 1.032.892 1.53 2.341 1.088 2.91.832.092-.647.35-1.088.636-1.338-2.22-.253-4.555-1.113-4.555-4.951 0-1.093.39-1.988 1.029-2.688-.103-.253-.446-1.272.098-2.65 0 0 .84-.27 2.75 1.026A9.564 9.564 0 0112 6.844c.85.004 1.705.115 2.504.337 1.909-1.296 2.747-1.027 2.747-1.027.546 1.379.202 2.398.1 2.651.64.7 1.028 1.595 1.028 2.688 0 3.848-2.339 4.695-4.566 4.943.359.309.678.92.678 1.855 0 1.338-.012 2.419-.012 2.747 0 .268.18.58.688.482A10.019 10.019 0 0022 12.017C22 6.484 17.522 2 12 2z"
                clip-rule="evenodd"
              />
            </svg>
          </a>
          <a
            href="#"
            target="_blank"
            rel="noopener noreferrer"
            class="text-secondary-500 transition-colors hover:text-white"
            aria-label="Discord"
          >
            <svg class="h-5 w-5" fill="currentColor" viewBox="0 0 24 24" aria-hidden="true">
              <path
                d="M20.317 4.37a19.791 19.791 0 00-4.885-1.515.074.074 0 00-.079.037c-.21.375-.444.864-.608 1.25a18.27 18.27 0 00-5.487 0 12.64 12.64 0 00-.617-1.25.077.077 0 00-.079-.037A19.736 19.736 0 003.677 4.37a.07.07 0 00-.032.027C.533 9.046-.32 13.58.099 18.057a.082.082 0 00.031.057 19.9 19.9 0 005.993 3.03.078.078 0 00.084-.028c.462-.63.874-1.295 1.226-1.994a.076.076 0 00-.041-.106 13.107 13.107 0 01-1.872-.892.077.077 0 01-.008-.128 10.2 10.2 0 00.372-.292.074.074 0 01.077-.01c3.928 1.793 8.18 1.793 12.062 0a.074.074 0 01.078.01c.12.098.246.198.373.292a.077.077 0 01-.006.127 12.299 12.299 0 01-1.873.892.077.077 0 00-.041.107c.36.698.772 1.362 1.225 1.993a.076.076 0 00.084.028 19.839 19.839 0 006.002-3.03.077.077 0 00.032-.054c.5-5.177-.838-9.674-3.549-13.66a.061.061 0 00-.031-.03zM8.02 15.33c-1.183 0-2.157-1.085-2.157-2.419 0-1.333.956-2.419 2.157-2.419 1.21 0 2.176 1.095 2.157 2.42 0 1.333-.956 2.418-2.157 2.418zm7.975 0c-1.183 0-2.157-1.085-2.157-2.419 0-1.333.955-2.419 2.157-2.419 1.21 0 2.176 1.095 2.157 2.42 0 1.333-.946 2.418-2.157 2.418z"
              />
            </svg>
          </a>
        </div>
      </div>
    </div>
  </footer>
</template>
