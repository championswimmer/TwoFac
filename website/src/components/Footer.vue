<script setup lang="ts">
import { RouterLink } from 'vue-router'

const productLinks = [
  { name: 'Features', to: '/features' },
  { name: 'Screenshots', to: '/screenshots' },
  { name: 'Download', to: '/download' },
  { name: 'Getting Started', to: '/getting-started' },
]

const resourceRouterLinks = [
  { name: 'Blog', to: '/blog' },
  { name: 'FAQ', to: '/faq' },
]

const resourceExternalLinks = [
  { name: 'Documentation', href: '/getting-started' },
]

const compareLinks = [
  { name: 'vs Google Authenticator', to: '/compare/google-authenticator' },
  { name: 'vs Microsoft Authenticator', to: '/compare/microsoft-authenticator' },
  { name: 'vs 2FAS', to: '/compare/2fas' },
  { name: 'vs Ente Auth', to: '/compare/ente-auth' },
  { name: 'vs Bitwarden', to: '/compare/bitwarden' },
]

const communityLinks = [
  { name: 'GitHub', href: 'https://github.com/championswimmer/TwoFac' },
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
            href="https://github.com/championswimmer/TwoFac"
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
        </div>
      </div>
    </div>
  </footer>
</template>
