import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  scrollBehavior(_to, _from, savedPosition) {
    return savedPosition || { top: 0 }
  },
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('../pages/HomePage.vue'),
    },
    {
      path: '/features',
      name: 'features',
      component: () => import('../pages/FeaturesPage.vue'),
    },
    {
      path: '/download',
      name: 'download',
      component: () => import('../pages/DownloadPage.vue'),
    },
    {
      path: '/getting-started',
      name: 'getting-started',
      component: () => import('../pages/GettingStartedPage.vue'),
    },
    {
      path: '/screenshots',
      name: 'screenshots',
      component: () => import('../pages/ScreenshotsPage.vue'),
    },
    {
      path: '/faq',
      name: 'faq',
      component: () => import('../pages/FAQPage.vue'),
    },
    {
      path: '/blog',
      name: 'blog',
      component: () => import('../pages/BlogPage.vue'),
    },
    {
      path: '/blog/:slug',
      name: 'blog-post',
      component: () => import('../pages/BlogPostPage.vue'),
    },
    {
      path: '/privacy',
      name: 'privacy',
      component: () => import('../pages/PrivacyPage.vue'),
    },
    {
      path: '/terms',
      name: 'terms',
      component: () => import('../pages/TermsPage.vue'),
    },
    {
      path: '/compare/2fas',
      name: 'compare-2fas',
      component: () => import('../pages/compare/Compare2FASPage.vue'),
    },
    {
      path: '/compare/ente-auth',
      name: 'compare-ente-auth',
      component: () => import('../pages/compare/CompareEnteAuthPage.vue'),
    },
    {
      path: '/compare/bitwarden',
      name: 'compare-bitwarden',
      component: () => import('../pages/compare/CompareBitwardenPage.vue'),
    },
    {
      path: '/compare/google-authenticator',
      name: 'compare-google-authenticator',
      component: () => import('../pages/compare/CompareGoogleAuthPage.vue'),
    },
    {
      path: '/compare/microsoft-authenticator',
      name: 'compare-microsoft-authenticator',
      component: () => import('../pages/compare/CompareMicrosoftAuthPage.vue'),
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('../pages/NotFoundPage.vue'),
    },
  ],
})

export default router
