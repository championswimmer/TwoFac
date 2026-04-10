import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const websiteDir = path.resolve(__dirname, '..')
const blogsFile = path.join(websiteDir, 'src', 'data', 'blogs.json')

export const INDEXABLE_STATIC_ROUTES = [
  { path: '/', changefreq: 'weekly', priority: '1.0' },
  { path: '/features', changefreq: 'monthly', priority: '0.8' },
  { path: '/download', changefreq: 'weekly', priority: '0.9' },
  { path: '/getting-started', changefreq: 'monthly', priority: '0.8' },
  { path: '/screenshots', changefreq: 'monthly', priority: '0.7' },
  { path: '/faq', changefreq: 'monthly', priority: '0.7' },
  { path: '/blog', changefreq: 'weekly', priority: '0.8' },
  { path: '/privacy', changefreq: 'monthly', priority: '0.5' },
  { path: '/terms', changefreq: 'monthly', priority: '0.5' },
  { path: '/compare/2fas', changefreq: 'monthly', priority: '0.7' },
  { path: '/compare/ente-auth', changefreq: 'monthly', priority: '0.7' },
  { path: '/compare/bitwarden', changefreq: 'monthly', priority: '0.7' },
  { path: '/compare/google-authenticator', changefreq: 'monthly', priority: '0.7' },
  { path: '/compare/microsoft-authenticator', changefreq: 'monthly', priority: '0.7' },
]

export const NON_INDEXABLE_STATIC_ROUTES = [
  { path: '/404', changefreq: 'yearly', priority: '0.1' },
]

function readBlogs() {
  if (!fs.existsSync(blogsFile)) {
    return []
  }

  return JSON.parse(fs.readFileSync(blogsFile, 'utf-8'))
}

export function getBlogRouteEntries() {
  return readBlogs().map((blog) => ({
    path: `/blog/${blog.slug}`,
    lastmod: blog.datePublished ? blog.datePublished.split('T')[0] : undefined,
    changefreq: 'monthly',
    priority: '0.6',
  }))
}

export function getPrerenderRoutePaths() {
  return [
    ...INDEXABLE_STATIC_ROUTES.map((route) => route.path),
    ...NON_INDEXABLE_STATIC_ROUTES.map((route) => route.path),
    ...getBlogRouteEntries().map((route) => route.path),
  ]
}

export function getSitemapEntries(today) {
  return [
    ...INDEXABLE_STATIC_ROUTES.map((route) => ({
      ...route,
      lastmod: today,
    })),
    ...getBlogRouteEntries().map((route) => ({
      ...route,
      lastmod: route.lastmod ?? today,
    })),
  ]
}
