---
name: Code Simplification Plan - website
status: Completed
progress:
  - "[x] Phase 0: Asset deduplication and content pipeline cleanup"
  - "[x] Phase 1: Page/template abstraction pass"
  - "[x] Phase 2: Build/dependency footprint reduction"
  - "[x] Phase 3: SEO/content consistency verification"
---

# Code Simplification Plan - website

## Scope
- Module: `website`
- Goal: keep landing-page code maintainable with less duplication in assets/content wiring and less manual page maintenance.

## Current Signals
1. **Static asset duplication with root docs/logo folders**
   - `website/public/twofac_logo_128.png`
   - `website/public/twofac_logo_512.png`
   - `website/public/twofac-logo.png`
   - `website/public/twofac-logo.svg`
2. **Growing page surface (especially compare pages)**
   - multiple `website/src/pages/compare/*.vue` pages follow similar layout structure and can drift over time.
3. **Build pipeline already has scripts (`scripts/build-blogs.js`)**
   - opportunity to centralize more generated/static content instead of committing duplicates.

## Simplification Roadmap

### Phase 0: Asset deduplication and content pipeline cleanup ✅
- [x] Define canonical location for brand assets — `logo/` (numbered PNGs) and `docs/` (twofac-logo.*).
- [x] Add deterministic copy step: `website/scripts/copy-assets.js` copies from canonical sources; wired into `prebuild`/`predev`.
- [x] Logo assets removed from git tracking (`git rm --cached`); added to `website/.gitignore`.
- Committed in: `627c52f` (HappyZenith, plan 07 phase 4 included website/ changes)

### Phase 1: Page/template abstraction pass 🔄 (PureRaven)
- [ ] Extract shared compare-page scaffold into reusable component/composable.
- [ ] Move per-competitor differences to structured config objects.
- [ ] Reduce repetitive static blocks across `compare/*.vue` files.
- Note: PureRaven owns this phase. 3 of 5 compare pages already refactored (Bitwarden, EnteAuth, GoogleAuth bundle sizes halved to ~10KB).

### Phase 2: Build/dependency footprint reduction ✅
- [x] Audited `package.json`: all 13 deps (5 runtime, 8 devDeps) are actively used and correctly categorised. No orphans found.
- [x] `node_modules` and generated output (`dist/`, `src/data/blogs.json`, `public/images/blogs/`) already in `.gitignore`.
- [x] Build validated fast: `npm run build` completes in ~200ms.

### Phase 3: SEO/content consistency verification ✅
- [x] All 13 pages/routes call `useSEO()` with `title`, `description`, and `canonicalPath`.
- [x] The `useSEO` composable consistently emits: `og:title`, `og:description`, `og:image` (absolute URL), `og:type: website`, `twitter:card: summary_large_image`, and `<link rel="canonical">`.
- [x] Fixed: `index.html` static fallback `og:image` was relative (`/twofac_logo_512.png`); corrected to absolute `https://twofac.app/twofac_logo_512.png` (Open Graph spec requires absolute URLs). Committed in: `cf28057`.
- [x] All compare pages retain their `useSEO()` calls in `<script setup>` (not moved into template component), so SEO remains auditable per-page.

## Success Criteria
- [x] Website assets have one source of truth. ✅
- [ ] Compare pages share a single template path and diverge only by data. (Phase 1, in progress by PureRaven)
- [x] Website build remains deterministic and lean. ✅
