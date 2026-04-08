<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  platform: 'ios' | 'android' | 'watchos' | 'wearos'
  screenshotSrc: string
  altText: string
}>()

const frameSrc = computed(() => {
  if (props.platform === 'ios') return '/images/devices/iphone.svg'
  if (props.platform === 'android') return '/images/devices/pixel.svg'
  if (props.platform === 'watchos') return '/images/devices/apple-watch.svg'
  if (props.platform === 'wearos') return '/images/devices/pixel-watch.svg'
  return '/images/devices/pixel.svg'
})

const screenshotStyles = computed(() => {
  if (props.platform === 'ios') {
    return {
      left: '3.2%',
      top: '1.53139%',
      width: '93.6%',
      height: '96.9372%',
      borderRadius: '11.11% / 5.13%',
    }
  }
  if (props.platform === 'watchos') {
    return {
      left: '30.8%',
      top: '27.0%',
      width: '38.4%',
      height: '45.6%',
      borderRadius: '21.87% / 18.42%',
    }
  }
  if (props.platform === 'wearos') {
    return {
      left: '26.6%',
      top: '27.6%',
      width: '45.6%',
      height: '45.6%',
      borderRadius: '50%',
    }
  }
  return {
    left: '3.4482%',
    top: '1.5974%',
    width: '93.1034%',
    height: '96.8051%',
    borderRadius: '8.33% / 3.71%',
  }
})

const screenshotBgClass = computed(() => {
  return (props.platform === 'watchos' || props.platform === 'wearos')
    ? 'bg-black'
    : 'bg-secondary-100 dark:bg-secondary-900'
})
const filterStyle = computed(() => {
  return (props.platform === 'watchos' || props.platform === 'wearos')
    ? 'filter: drop-shadow(0 25px 35px rgba(0, 0, 0, 0.5));'
    : ''
})
</script>

<template>
  <div class="relative inline-block leading-none" :style="filterStyle">
    <!-- The screenshot image (sits underneath the glare) -->
    <img
      :src="screenshotSrc"
      :alt="altText"
      class="absolute z-0 block object-cover object-top"
      :class="screenshotBgClass"
      :style="screenshotStyles"
    />
    <!-- The device frame SVG overlay (contains glare so it goes on top) -->
    <img
      :src="frameSrc"
      class="pointer-events-none absolute inset-0 z-10 block w-full h-auto"
      aria-hidden="true"
    />
    <!-- Invisible placeholder to give the container height -->
    <img
      :src="frameSrc"
      class="pointer-events-none relative z-[-1] block w-full h-auto opacity-0"
      aria-hidden="true"
    />
  </div>
</template>
