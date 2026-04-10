import { ViteSSG } from 'vite-ssg'
import App from './App.vue'
import { routes, scrollBehavior } from './router'
import '@fortawesome/fontawesome-free/css/all.min.css'
import './style.css'

export const createApp = ViteSSG(
  App,
  {
    routes,
    scrollBehavior,
  },
  () => {
    // Shared app setup lives here for both client hydration and SSG.
  },
)
