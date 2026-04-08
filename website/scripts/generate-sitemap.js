import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const websiteDir = path.resolve(__dirname, '..')
const distDir = path.join(websiteDir, 'dist')
const blogsFile = path.join(websiteDir, 'src', 'data', 'blogs.json')

const SITE_URL = 'https://twofac.app'
const TODAY = new Date().toISOString().split('T')[0]

// Static routes: [path, changefreq, priority]
const STATIC_ROUTES = [
  ['/',                                  'weekly',  '1.0'],
  ['/features',                          'monthly', '0.8'],
  ['/download',                          'weekly',  '0.9'],
  ['/getting-started',                   'monthly', '0.8'],
  ['/screenshots',                       'monthly', '0.7'],
  ['/faq',                               'monthly', '0.7'],
  ['/blog',                              'weekly',  '0.8'],
  ['/compare/google-authenticator',      'monthly', '0.7'],
  ['/compare/microsoft-authenticator',   'monthly', '0.7'],
  ['/compare/ente-auth',                 'monthly', '0.7'],
  ['/compare/2fas',                      'monthly', '0.7'],
  ['/compare/bitwarden',                 'monthly', '0.7'],
]

function xmlEscape(str) {
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&apos;')
}

function urlEntry(loc, lastmod, changefreq, priority) {
  return [
    '  <url>',
    `    <loc>${xmlEscape(loc)}</loc>`,
    `    <lastmod>${lastmod}</lastmod>`,
    `    <changefreq>${changefreq}</changefreq>`,
    `    <priority>${priority}</priority>`,
    '  </url>',
  ].join('\n')
}

const entries = []

// Static pages
for (const [routePath, changefreq, priority] of STATIC_ROUTES) {
  entries.push(urlEntry(`${SITE_URL}${routePath}`, TODAY, changefreq, priority))
}

// Dynamic blog posts
if (fs.existsSync(blogsFile)) {
  const blogs = JSON.parse(fs.readFileSync(blogsFile, 'utf-8'))
  for (const blog of blogs) {
    const lastmod = blog.datePublished
      ? blog.datePublished.split('T')[0]
      : TODAY
    entries.push(urlEntry(`${SITE_URL}/blog/${blog.slug}`, lastmod, 'monthly', '0.6'))
  }
}

const sitemap = [
  '<?xml version="1.0" encoding="UTF-8"?>',
  '<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">',
  ...entries,
  '</urlset>',
].join('\n')

fs.mkdirSync(distDir, { recursive: true })
fs.writeFileSync(path.join(distDir, 'sitemap.xml'), sitemap, 'utf-8')
console.log(`Generated sitemap.xml with ${entries.length} URL(s)`)
