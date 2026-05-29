import { defineConfig, type UserConfig } from 'vite'
import type { ViteSSGContext } from 'vite-ssg'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'
import { Unhead } from '@unhead/vue/vite'
import { transformHtmlTemplate } from '@unhead/vue/server'
import { getPrerenderRoutePaths } from './scripts/route-manifest.js'

type ViteSSGConfig = UserConfig & {
  ssgOptions?: {
    dirStyle?: 'flat' | 'nested'
    includedRoutes?: () => string[]
    onPageRendered?: (
      route: string,
      renderedHTML: string,
      appCtx: ViteSSGContext<true>,
    ) => string | null | undefined | Promise<string | null | undefined>
  }
}

export default defineConfig({
  plugins: [vue(), Unhead(), tailwindcss()],
  ssgOptions: {
    dirStyle: 'nested',
    includedRoutes() {
      return getPrerenderRoutePaths()
    },
    onPageRendered(_route: string, renderedHTML: string, appCtx: ViteSSGContext<true>) {
      return appCtx.head
        ? transformHtmlTemplate(appCtx.head as Parameters<typeof transformHtmlTemplate>[0], renderedHTML)
        : renderedHTML
    },
  },
} as ViteSSGConfig)
