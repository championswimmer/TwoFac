<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  platform: 'ios' | 'android'
  screenshotSrc: string
  altText: string
}>()

const frameSrc = computed(() => {
  return props.platform === 'ios'
    ? '/images/devices/iphone.svg'
    : '/images/devices/pixel.svg'
})

const screenshotStyles = computed(() => {
  if (props.platform === 'ios') {
    return {
      left: '3.2%',
      top: '1.53139%',
      width: '93.6%',
      height: '96.9372%',
      borderRadius: '10.5%',
    }
  }
  return {
    left: '3.4482%',
    top: '1.5974%',
    width: '93.1034%',
    height: '96.8051%',
    borderRadius: '7.5%',
  }
})
</script>

<template>
  <div class="relative inline-block w-full max-w-[280px] sm:max-w-[320px]">
    <!-- The device frame SVG overlay -->
    <img
      :src="frameSrc"
      class="pointer-events-none relative z-10 w-full h-auto drop-shadow-2xl"
      aria-hidden="true"
    />
    <!-- The screenshot image -->
    <img
      :src="screenshotSrc"
      :alt="altText"
      class="absolute z-0 object-cover object-top bg-secondary-100 dark:bg-secondary-900"
      :style="screenshotStyles"
    />
  </div>
</template>
