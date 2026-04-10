import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'
import { getPrerenderRoutePaths } from './scripts/route-manifest.js'

export default defineConfig({
  plugins: [vue(), tailwindcss()],
  ssgOptions: {
    includedRoutes() {
      return getPrerenderRoutePaths()
    },
  },
})
