<script setup lang="ts">
defineProps<{
  icon: string
  title: string
  description: string
}>()
</script>

<template>
  <div
    class="group rounded-2xl border border-secondary-200 bg-white p-6 shadow-sm transition-all duration-300 hover:-translate-y-1 hover:shadow-lg dark:border-secondary-800 dark:bg-secondary-900"
  >
    <div
      class="mb-4 flex h-12 w-12 items-center justify-center rounded-xl bg-primary-50 text-2xl transition-colors group-hover:bg-primary-100 dark:bg-primary-950 dark:group-hover:bg-primary-900"
    >
      {{ icon }}
    </div>
    <h3 class="mb-2 text-lg font-semibold text-secondary-900 dark:text-white">
      {{ title }}
    </h3>
    <p class="text-sm leading-relaxed text-secondary-600 dark:text-secondary-400">
      {{ description }}
    </p>
  </div>
</template>
