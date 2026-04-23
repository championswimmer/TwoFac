import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { getBlogRouteEntries, INDEXABLE_STATIC_ROUTES } from './route-manifest.js'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const websiteDir = path.resolve(__dirname, '..')
const distDir = path.join(websiteDir, 'dist')

function routeToHtmlPath(routePath) {
  if (routePath === '/') {
    return path.join(distDir, 'index.html')
  }

  return path.join(distDir, routePath.replace(/^\//, ''), 'index.html')
}

function assertExists(filePath) {
  if (!fs.existsSync(filePath)) {
    throw new Error(`Expected file to exist: ${filePath}`)
  }
}

function readFile(filePath) {
  assertExists(filePath)
  return fs.readFileSync(filePath, 'utf-8')
}

function assertIncludes(haystack, needle, context) {
  if (!haystack.includes(needle)) {
    throw new Error(`Expected ${context} to include: ${needle}`)
  }
}

const homepageHtml = readFile(routeToHtmlPath('/'))
const compareHtml = readFile(routeToHtmlPath('/compare/google-authenticator'))
const blogIndexHtml = readFile(routeToHtmlPath('/blog'))
const blogDetailRoute = getBlogRouteEntries()[0]
const blogDetailHtml = readFile(routeToHtmlPath(blogDetailRoute.path))
const notFoundHtml = readFile(path.join(distDir, '404.html'))
const sitemapXml = readFile(path.join(distDir, 'sitemap.xml'))

for (const route of INDEXABLE_STATIC_ROUTES) {
  assertExists(routeToHtmlPath(route.path))
}

for (const route of getBlogRouteEntries()) {
  assertExists(routeToHtmlPath(route.path))
}

assertIncludes(homepageHtml, 'Why TwoFac?', 'homepage HTML')
assertIncludes(homepageHtml, '<title>Open Source Cross-Platform 2FA | TwoFac</title>', 'homepage head')
assertIncludes(homepageHtml, 'name="description"', 'homepage head')
assertIncludes(homepageHtml, 'property="og:url" content="https://twofac.app/"', 'homepage head')

assertIncludes(compareHtml, '<title>TwoFac vs Google Authenticator: Best Open Source 2FA Alternative (2026) | TwoFac</title>', 'compare page head')
assertIncludes(compareHtml, 'TwoFac vs Google Authenticator at a Glance', 'compare page HTML')
assertIncludes(compareHtml, 'property="og:url" content="https://twofac.app/compare/google-authenticator"', 'compare page head')

assertIncludes(blogIndexHtml, '>Blog</h1>', 'blog index HTML')
assertIncludes(blogIndexHtml, 'property="og:url" content="https://twofac.app/blog"', 'blog index head')

assertIncludes(blogDetailHtml, 'property="og:type" content="article"', 'blog detail head')
assertIncludes(blogDetailHtml, 'article:published_time', 'blog detail head')
assertIncludes(blogDetailHtml, '<article', 'blog detail HTML')

assertIncludes(notFoundHtml, 'name="robots" content="noindex, nofollow"', '404 head')
assertIncludes(notFoundHtml, 'Page not found', '404 HTML')

for (const route of INDEXABLE_STATIC_ROUTES) {
  assertIncludes(sitemapXml, `https://twofac.app${route.path}`, 'sitemap.xml')
}

for (const route of getBlogRouteEntries()) {
  assertIncludes(sitemapXml, `https://twofac.app${route.path}`, 'sitemap.xml')
}

if (sitemapXml.includes('https://twofac.app/404')) {
  throw new Error('Sitemap should not include /404')
}

console.log(`Validated prerendered HTML for ${INDEXABLE_STATIC_ROUTES.length + getBlogRouteEntries().length} route(s)`)
