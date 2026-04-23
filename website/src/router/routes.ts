import type { RouteRecordRaw, RouterScrollBehavior } from 'vue-router'

export const scrollBehavior: RouterScrollBehavior = (_to, _from, savedPosition) => {
  return savedPosition || { top: 0 }
}

export const routes: RouteRecordRaw[] = [
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
    path: '/compare/aegis-authenticator',
    name: 'compare-aegis-authenticator',
    component: () => import('../pages/compare/CompareAegisAuthPage.vue'),
  },
  {
    path: '/compare/bitwarden',
    name: 'compare-bitwarden',
    component: () => import('../pages/compare/CompareBitwardenPage.vue'),
  },
  {
    path: '/compare/proton-authenticator',
    name: 'compare-proton-authenticator',
    component: () => import('../pages/compare/CompareProtonAuthPage.vue'),
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
    path: '/migrate/2fas',
    name: 'migrate-2fas',
    component: () => import('../pages/migrate/Migrate2FASPage.vue'),
  },
  {
    path: '/migrate/ente-auth',
    name: 'migrate-ente-auth',
    component: () => import('../pages/migrate/MigrateEnteAuthPage.vue'),
  },
  {
    path: '/migrate/aegis-authenticator',
    name: 'migrate-aegis-authenticator',
    component: () => import('../pages/migrate/MigrateAegisAuthPage.vue'),
  },
  {
    path: '/migrate/bitwarden',
    name: 'migrate-bitwarden',
    component: () => import('../pages/migrate/MigrateBitwardenPage.vue'),
  },
  {
    path: '/migrate/proton-authenticator',
    name: 'migrate-proton-authenticator',
    component: () => import('../pages/migrate/MigrateProtonAuthPage.vue'),
  },
  {
    path: '/migrate/google-authenticator',
    name: 'migrate-google-authenticator',
    component: () => import('../pages/migrate/MigrateGoogleAuthPage.vue'),
  },
  {
    path: '/migrate/microsoft-authenticator',
    name: 'migrate-microsoft-authenticator',
    component: () => import('../pages/migrate/MigrateMicrosoftAuthPage.vue'),
  },
  {
    path: '/migrate/authy',
    name: 'migrate-authy',
    component: () => import('../pages/migrate/MigrateAuthyPage.vue'),
  },
  {
    path: '/404',
    name: '404',
    component: () => import('../pages/NotFoundPage.vue'),
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('../pages/NotFoundPage.vue'),
  },
]
