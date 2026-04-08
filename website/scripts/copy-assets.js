/**
 * copy-assets.js
 *
 * Copies brand logo assets from their canonical sources into website/public/
 * so that website/public/ copies are generated at build time and do not need
 * to be committed to git.
 *
 * Canonical sources:
 *   logo/twofac_logo_*.png  →  public/twofac_logo_*.png
 *   docs/twofac-logo.*      →  public/twofac-logo.*
 */

import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const repoRoot = path.resolve(__dirname, '..', '..')
const publicDir = path.resolve(__dirname, '..', 'public')

const assets = [
  // Numbered logos — canonical source: logo/
  { src: path.join(repoRoot, 'logo', 'twofac_logo_128.png'), dest: path.join(publicDir, 'twofac_logo_128.png') },
  { src: path.join(repoRoot, 'logo', 'twofac_logo_512.png'), dest: path.join(publicDir, 'twofac_logo_512.png') },
  // Unnumbered logos — canonical source: docs/
  { src: path.join(repoRoot, 'docs', 'twofac-logo.png'), dest: path.join(publicDir, 'twofac-logo.png') },
  { src: path.join(repoRoot, 'docs', 'twofac-logo.svg'), dest: path.join(publicDir, 'twofac-logo.svg') },
]

let copied = 0
for (const { src, dest } of assets) {
  if (!fs.existsSync(src)) {
    console.warn(`copy-assets: source not found, skipping: ${src}`)
    continue
  }
  fs.copyFileSync(src, dest)
  copied++
}
console.log(`copy-assets: copied ${copied}/${assets.length} logo asset(s) to public/`)

// Copy screenshots
const screenshotsDir = path.join(repoRoot, 'docs', 'screenshots')
const destScreenshotsDir = path.join(publicDir, 'images', 'screenshots')

if (fs.existsSync(screenshotsDir)) {
  if (!fs.existsSync(destScreenshotsDir)) {
    fs.mkdirSync(destScreenshotsDir, { recursive: true })
  }
  const files = fs.readdirSync(screenshotsDir)
  let copiedScreenshots = 0
  for (const file of files) {
    if (file.endsWith('.png') || file.endsWith('.jpg')) {
      fs.copyFileSync(path.join(screenshotsDir, file), path.join(destScreenshotsDir, file))
      copiedScreenshots++
    }
  }
  console.log(`copy-assets: copied ${copiedScreenshots} screenshot(s) to public/images/screenshots/`)
}
