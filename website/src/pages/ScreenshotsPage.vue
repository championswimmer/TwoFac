<script setup lang="ts">
import MainLayout from '../layouts/MainLayout.vue'
import DeviceMockup from '../components/DeviceMockup.vue'
import { useSEO } from '../composables/useSEO'
import { ref } from 'vue'

useSEO({
  title: 'Screenshots & Product Images',
  description: 'Explore the interface of TwoFac across iOS and Android devices.',
  canonicalPath: '/screenshots',
})

const activeTab = ref<'ios' | 'android' | 'watch'>('ios')

interface Screenshot {
  src: string
  title: string
  platform?: 'ios' | 'android' | 'watchos' | 'wearos'
}

const screenshots: Record<'ios' | 'android' | 'watch', Screenshot[]> = {
  ios: [
    { src: '/images/screenshots/ios-01-lock-screen.png', title: 'Lock Screen' },
    { src: '/images/screenshots/ios-02-passkey-entered.png', title: 'Passkey Auth' },
    { src: '/images/screenshots/ios-03-home-screen.png', title: 'Home Screen' },
    { src: '/images/screenshots/ios-07-home-with-accounts.png', title: 'Home (With Accounts)' },
    { src: '/images/screenshots/ios-04-accounts-screen.png', title: 'Accounts' },
    { src: '/images/screenshots/ios-08-accounts-list-with-accounts.png', title: 'Accounts List' },
    { src: '/images/screenshots/ios-06-add-account-screen.png', title: 'Add Account' },
    { src: '/images/screenshots/ios-09-account-detail.png', title: 'Account Detail' },
    { src: '/images/screenshots/ios-05-settings-screen.png', title: 'Settings' },
  ],
  android: [
    { src: '/images/screenshots/android-01-lock-screen.png', title: 'Lock Screen' },
    { src: '/images/screenshots/android-02-passkey-entered.png', title: 'Passkey Auth' },
    { src: '/images/screenshots/android-03-home-screen.png', title: 'Home Screen' },
    { src: '/images/screenshots/android-07-home-with-accounts.png', title: 'Home (With Accounts)' },
    { src: '/images/screenshots/android-04-accounts-screen.png', title: 'Accounts' },
    { src: '/images/screenshots/android-08-accounts-list-with-accounts.png', title: 'Accounts List' },
    { src: '/images/screenshots/android-06-add-account-screen.png', title: 'Add Account' },
    { src: '/images/screenshots/android-09-account-detail.png', title: 'Account Detail' },
    { src: '/images/screenshots/android-05-settings-screen.png', title: 'Settings' },
  ],
  watch: [
    { src: '/images/screenshots/watchos-01-account.png', title: 'Apple Watch', platform: 'watchos' },
    { src: '/images/screenshots/wearos-01-account.png', title: 'Android Wear', platform: 'wearos' },
  ],
}
</script>

<template>
  <MainLayout>
    <section class="border-b border-secondary-200 bg-secondary-100 py-16 sm:py-24 dark:border-secondary-800 dark:bg-secondary-900">
      <div class="mx-auto max-w-7xl px-4 text-center sm:px-6 lg:px-8">
        <h1 class="text-4xl font-bold tracking-tight text-secondary-900 dark:text-secondary-50 sm:text-5xl">
          Product Screenshots
        </h1>
        <p class="mx-auto mt-4 max-w-2xl text-lg text-secondary-700 dark:text-secondary-300">
          Take a look at the beautiful, native interface of TwoFac across iOS and Android.
        </p>

        <!-- Tabs -->
        <div class="mt-8 flex justify-center gap-4">
          <button
            class="px-6 py-2.5 rounded-full font-semibold transition-colors"
            :class="activeTab === 'ios' ? 'bg-primary-600 text-white' : 'bg-secondary-200 dark:bg-secondary-800 text-secondary-800 dark:text-secondary-200 hover:bg-secondary-300 dark:hover:bg-secondary-700'"
            @click="activeTab = 'ios'"
          >
            <i class="fa-brands fa-apple mr-2" /> iOS
          </button>
          <button
            class="px-6 py-2.5 rounded-full font-semibold transition-colors"
            :class="activeTab === 'android' ? 'bg-primary-600 text-white' : 'bg-secondary-200 dark:bg-secondary-800 text-secondary-800 dark:text-secondary-200 hover:bg-secondary-300 dark:hover:bg-secondary-700'"
            @click="activeTab = 'android'"
          >
            <i class="fa-brands fa-android mr-2" /> Android
          </button>
          <button
            class="px-6 py-2.5 rounded-full font-semibold transition-colors"
            :class="activeTab === 'watch' ? 'bg-primary-600 text-white' : 'bg-secondary-200 dark:bg-secondary-800 text-secondary-800 dark:text-secondary-200 hover:bg-secondary-300 dark:hover:bg-secondary-700'"
            @click="activeTab = 'watch'"
          >
            <i class="fa-solid fa-clock mr-2" /> Watch
          </button>
        </div>
      </div>
    </section>

    <section class="py-16 sm:py-24 bg-secondary-50 dark:bg-secondary-950">
      <div class="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-12 justify-items-center">
          <div v-for="shot in screenshots[activeTab]" :key="shot.src" class="flex flex-col items-center">
            <DeviceMockup :platform="(shot.platform || (activeTab === 'watch' ? 'watchos' : activeTab)) as any" :screenshotSrc="shot.src" :altText="shot.title" class="w-64 sm:w-72" />
            <p class="mt-6 text-lg font-semibold text-secondary-900 dark:text-white">
              {{ shot.title }}
            </p>
          </div>
        </div>
      </div>
    </section>
  </MainLayout>
</template>
