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
        ? 'bg-white/20 text-white backdrop-blur-sm hover:bg-white/30'
        : 'bg-white/5 text-white/40 backdrop-blur-sm'
    "
  >
    <span class="text-base" aria-hidden="true">{{ icon }}</span>
    {{ name }}
  </span>
</template>
