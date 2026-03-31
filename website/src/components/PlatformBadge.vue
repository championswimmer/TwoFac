<script setup lang="ts">
withDefaults(
  defineProps<{
    name: string
    icon: string
    available?: boolean
  }>(),
  { available: true },
)
</script>

<template>
  <span
    class="inline-flex items-center gap-1.5 rounded-full px-4 py-1.5 text-sm font-medium transition-colors"
    :class="
      available
        ? 'border border-secondary-300 bg-secondary-50 text-secondary-700 hover:bg-secondary-100 dark:border-secondary-700 dark:bg-secondary-900 dark:text-secondary-300 dark:hover:bg-secondary-800'
        : 'border border-secondary-200 bg-secondary-100 text-secondary-500 dark:border-secondary-800 dark:bg-secondary-950 dark:text-secondary-600'
    "
  >
    <i :class="icon" class="text-base" aria-hidden="true" />
    {{ name }}
  </span>
</template>
