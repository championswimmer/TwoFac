<script setup lang="ts">
defineProps<{
  features: Array<{
    name: string
    twofac: boolean | string
    competitor: boolean | string
  }>
  competitorName: string
}>()

function displayValue(value: boolean | string): string {
  if (typeof value === 'string') return value
  return value ? '✓' : '✗'
}

function cellClass(value: boolean | string, isTwoFac: boolean): string {
  if (typeof value === 'boolean') {
    if (value && isTwoFac) return 'text-green-600 dark:text-green-400 font-bold'
    if (value) return 'text-green-600 dark:text-green-400'
    return 'text-secondary-400 dark:text-secondary-600'
  }
  return isTwoFac
    ? 'text-secondary-900 dark:text-white font-medium'
    : 'text-secondary-600 dark:text-secondary-400'
}
</script>

<template>
  <div class="overflow-x-auto rounded-xl border border-secondary-200 dark:border-secondary-800">
    <table class="w-full min-w-[480px] text-sm">
      <thead>
        <tr class="border-b border-secondary-200 bg-secondary-50 dark:border-secondary-800 dark:bg-secondary-900">
          <th class="px-6 py-4 text-left font-semibold text-secondary-900 dark:text-white">
            Feature
          </th>
          <th class="px-6 py-4 text-center font-semibold text-primary-600 dark:text-primary-400">
            TwoFac
          </th>
          <th class="px-6 py-4 text-center font-semibold text-secondary-600 dark:text-secondary-400">
            {{ competitorName }}
          </th>
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="(feature, index) in features"
          :key="feature.name"
          :class="[
            'border-b border-secondary-100 transition-colors hover:bg-secondary-50 dark:border-secondary-800/50 dark:hover:bg-secondary-800/30',
            index % 2 === 0 ? 'bg-white dark:bg-secondary-950' : 'bg-secondary-50/50 dark:bg-secondary-900/30',
          ]"
        >
          <td class="px-6 py-3.5 font-medium text-secondary-700 dark:text-secondary-300">
            {{ feature.name }}
          </td>
          <td class="px-6 py-3.5 text-center text-lg" :class="cellClass(feature.twofac, true)">
            {{ displayValue(feature.twofac) }}
          </td>
          <td class="px-6 py-3.5 text-center text-lg" :class="cellClass(feature.competitor, false)">
            {{ displayValue(feature.competitor) }}
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
