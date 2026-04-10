import { defineConfig, type UserConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'
import { getPrerenderRoutePaths } from './scripts/route-manifest.js'

type ViteSSGConfig = UserConfig & {
  ssgOptions?: {
    dirStyle?: 'flat' | 'nested'
    includedRoutes?: () => string[]
  }
}

export default defineConfig({
  plugins: [vue(), tailwindcss()],
  ssgOptions: {
    dirStyle: 'nested',
    includedRoutes() {
      return getPrerenderRoutePaths()
    },
  },
} as ViteSSGConfig)
