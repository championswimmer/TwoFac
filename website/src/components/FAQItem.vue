<script setup lang="ts">
import { ref } from 'vue'

defineProps<{
  question: string
  answer: string
}>()

const isOpen = ref(false)
</script>

<template>
  <div
    class="border-b border-secondary-200 dark:border-secondary-800"
  >
    <button
      type="button"
      class="flex w-full items-center justify-between gap-4 py-5 text-left transition-colors hover:text-primary-600 dark:hover:text-primary-400"
      :aria-expanded="isOpen"
      @click="isOpen = !isOpen"
    >
      <span class="text-base font-medium text-secondary-900 dark:text-white">
        {{ question }}
      </span>
      <span
        class="flex h-6 w-6 shrink-0 items-center justify-center rounded-full border border-secondary-300 text-secondary-500 transition-transform duration-200 dark:border-secondary-700 dark:text-secondary-400"
        :class="{ 'rotate-45': isOpen }"
      >
        <svg class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
        </svg>
      </span>
    </button>

    <Transition
      enter-active-class="transition-all duration-300 ease-out"
      enter-from-class="max-h-0 opacity-0"
      enter-to-class="max-h-96 opacity-100"
      leave-active-class="transition-all duration-200 ease-in"
      leave-from-class="max-h-96 opacity-100"
      leave-to-class="max-h-0 opacity-0"
    >
      <div v-show="isOpen" class="overflow-hidden">
        <p class="pb-5 text-sm leading-relaxed text-secondary-600 dark:text-secondary-400">
          {{ answer }}
        </p>
      </div>
    </Transition>
  </div>
</template>
