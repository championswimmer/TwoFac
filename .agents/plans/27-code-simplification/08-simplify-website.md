---
name: Code Simplification Plan - website
status: Planned
progress:
  - "[ ] Phase 0: Asset deduplication and content pipeline cleanup"
  - "[ ] Phase 1: Page/template abstraction pass"
  - "[ ] Phase 2: Build/dependency footprint reduction"
  - "[ ] Phase 3: SEO/content consistency verification"
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

### Phase 0: Asset deduplication and content pipeline cleanup
- [ ] Define canonical location for brand assets (`logo/` or `website/public/`, choose one).
- [ ] Add deterministic copy step in website build/prebuild if website needs local copies.
- [ ] Keep generated/public duplicates out of git where practical.

### Phase 1: Page/template abstraction pass
- [ ] Extract shared compare-page scaffold into reusable component/composable.
- [ ] Move per-competitor differences to structured config objects.
- [ ] Reduce repetitive static blocks across `compare/*.vue` files.

### Phase 2: Build/dependency footprint reduction
- [ ] Audit `package.json` for strictly required deps/devDeps.
- [ ] Ensure local `node_modules` and generated output remain ignored.
- [ ] Validate build remains fast (`npm run build`) after refactor.

### Phase 3: SEO/content consistency verification
- [ ] Verify all generated and hand-authored pages use consistent SEO metadata patterns.
- [ ] Ensure canonical URLs and OpenGraph fields remain intact after templating changes.

## Success Criteria
- Website assets have one source of truth.
- Compare pages share a single template path and diverge only by data.
- Website build remains deterministic and lean.
