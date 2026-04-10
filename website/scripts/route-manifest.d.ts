export interface RouteManifestEntry {
  path: string
  changefreq: string
  priority: string
  lastmod?: string
}

export const INDEXABLE_STATIC_ROUTES: RouteManifestEntry[]
export const NON_INDEXABLE_STATIC_ROUTES: RouteManifestEntry[]
export function getBlogRouteEntries(): RouteManifestEntry[]
export function getPrerenderRoutePaths(): string[]
export function getSitemapEntries(today: string): RouteManifestEntry[]
