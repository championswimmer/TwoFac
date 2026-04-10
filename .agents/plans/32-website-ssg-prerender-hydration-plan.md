---
name: Website SSG Prerender + Hydration Plan
status: In Progress
progress:
  - "[x] Phase 0 - Lock architecture, route inventory, and acceptance criteria"
  - "[x] Phase 1 - Refactor app bootstrap for shared client/SSG entry"
  - "[x] Phase 2 - Add deterministic prerender route generation"
  - "[x] Phase 3 - Make pages SSR-safe and hydration-safe"
  - "[x] Phase 4 - Render SEO/head tags into generated HTML"
  - "[x] Phase 5 - Integrate SSG into npm build and static hosting output"
  - "[ ] Phase 6 - Validate output, hydration, and search-engine readiness"
  - "[ ] Phase 7 - Rollout, docs, and deployment hardening"
---

# Website SSG Prerender + Hydration Plan

## Goal

Convert `website/` from a pure client-rendered Vue SPA into a **pre-rendered static site with client hydration** so that:

1. `npm run build` emits HTML files for all static/known routes.
2. Search engines and social scrapers can read meaningful content and meta tags directly from the generated HTML.
3. Once loaded in the browser, the site hydrates and continues behaving like the current SPA.
4. The solution remains compatible with **static hosting** (GitHub Pages / CDN / object storage) and does **not** require a long-running Node SSR server.

> Important framing: for this website, the right implementation is **SSG / prerendering** (SSR at build time), not runtime SSR. That gives us server-rendered HTML output without introducing a backend server.

---

## Current State Snapshot

### Current rendering model
- `website/src/main.ts` uses `createApp(App).mount('#app')`.
- All page content and `<head>` metadata are produced only after the browser loads JavaScript.
- `@unhead/vue` is already present, but it currently helps only after client boot.

### Current route surface
The router currently contains:
- **14 static routes**
  - `/`
  - `/features`
  - `/download`
  - `/getting-started`
  - `/screenshots`
  - `/faq`
  - `/blog`
  - `/privacy`
  - `/terms`
  - `/compare/2fas`
  - `/compare/ente-auth`
  - `/compare/bitwarden`
  - `/compare/google-authenticator`
  - `/compare/microsoft-authenticator`
- **1 dynamic route family**
  - `/blog/:slug`
- **1 catch-all not-found route**

### Existing build pipeline
`website/package.json` currently does:
- `prebuild`: copy assets + generate blog JSON
- `build`: `vue-tsc -b && vite build && cp dist/index.html dist/404.html && node scripts/generate-sitemap.js`

### Existing dynamic content inputs
- Blog posts are generated at build time into `website/src/data/blogs.json` by `scripts/build-blogs.js`.
- That means blog routes are already deterministic and are excellent candidates for prerendering.

### Known SSR-sensitive areas already visible
- `website/src/pages/BlogPostPage.vue` uses:
  - `window.matchMedia(...)`
  - DOM querying through `blogContent.value.querySelectorAll(...)`
  - `mermaid` rendering in `onMounted`
- These are fine on the client, but must be isolated from the server-render path.

---

## Recommended Technical Direction

## Recommendation: adopt `vite-ssg`
Use **`vite-ssg`** rather than migrating the site to Nuxt or building custom Vite SSR plumbing.

### Why this is the best fit here
1. The site is already a Vue 3 + Vue Router + Vite app.
2. Most routes are static and known ahead of time.
3. Blog slugs are build-time data, so dynamic routes can still be prerendered deterministically.
4. `vite-ssg` generates static HTML per route and hydrates the Vue app on the client.
5. Static hosting remains unchanged.
6. The migration cost is much lower than moving to a different framework.

### What the end state should look like
After implementation, `npm run build` should produce:
- HTML per route (for example: `dist/features/index.html`, `dist/faq/index.html`, `dist/blog/<slug>/index.html`, etc.)
- the JS/CSS bundles needed for hydration
- a `404.html` fallback for static hosting
- an updated `sitemap.xml`

When users open a page directly:
- they get real HTML immediately
- SEO crawlers can parse the content and metadata
- Vue hydrates on top of the markup and SPA navigation continues working

---

## Target Architecture

### High-level architecture
1. **Shared route definitions** live in one place.
2. **Shared app bootstrap** is used by both SSG generation and client hydration.
3. **Deterministic prerender route list** is generated from:
   - static route paths
   - blog slugs from `src/data/blogs.json`
4. **Client-only behaviors** run only after mount.
5. **Head/meta rendering** is resolved during prerender so each generated HTML file has route-specific SEO metadata.

### Likely file changes
- `website/package.json`
- `website/vite.config.ts`
- `website/src/main.ts`
- `website/src/router/index.ts`
- `website/src/pages/BlogPostPage.vue`
- optionally new helpers such as:
  - `website/src/router/routes.ts`
  - `website/src/ssg/includedRoutes.ts`
  - `website/src/ssg/routeManifest.ts`
  - `website/scripts/generate-prerender-routes.js` or equivalent shared utility

## Phase 0 Decisions Locked In

### Final rendering architecture
- Use **`vite-ssg`** for build-time prerendering plus Vue hydration.
- Keep the site deployable as **static files only** with no runtime Node SSR server.
- Keep `vite` as the local dev server for now; no separate SSG dev workflow is required for this rollout.

### Prerender route inventory
- Generate HTML for all current static marketing/legal pages:
  - `/`
  - `/features`
  - `/download`
  - `/getting-started`
  - `/screenshots`
  - `/faq`
  - `/blog`
  - `/privacy`
  - `/terms`
  - `/compare/2fas`
  - `/compare/ente-auth`
  - `/compare/bitwarden`
  - `/compare/google-authenticator`
  - `/compare/microsoft-authenticator`
- Generate HTML for every blog post route discovered from `src/data/blogs.json` as `/blog/:slug`.
- Generate an explicit `/404` page and copy its output to `dist/404.html` for static host fallback handling.
- Do **not** prerender the catch-all matcher directly; it will continue routing to the same not-found page at runtime.

### Acceptance criteria
- Built HTML for representative routes contains meaningful page body content without requiring client execution.
- Built HTML contains route-correct SEO output: `<title>`, description, canonical, Open Graph, and Twitter tags.
- Hydration completes without mismatch warnings on representative pages.
- Client-side navigation continues to behave like the current SPA after first load.
- Sitemap generation stays aligned with the prerender route manifest so future blog routes cannot drift.

---

## Phase 0 - Lock architecture, route inventory, and acceptance criteria

### Objectives
- Decide the exact implementation shape before changing build plumbing.
- Avoid mixing “runtime SSR server” concerns into what should be a static prerender solution.

### Steps
1. Confirm the chosen approach is **SSG with hydration** via `vite-ssg`.
2. Freeze the first-wave prerender route set:
   - all 14 static routes
   - all blog detail routes from `blogs.json`
3. Decide how to treat:
   - `404.html`
   - catch-all route rendering
   - future routes with client-only behavior
4. Define acceptance criteria for completion:
   - direct `curl`/View Source of key pages contains real page content
   - route-specific `<title>`, description, canonical, OG tags are present in built HTML
   - client hydration occurs without warnings/errors
   - internal navigation still works as SPA routing
5. Decide whether dev mode stays as plain `vite` initially or whether a dedicated SSG dev workflow is added later.

### Deliverables
- final architecture decision recorded in this plan
- explicit list of routes to prerender
- explicit definition of SEO and hydration success

---

## Phase 1 - Refactor app bootstrap for shared client/SSG entry

### Objectives
- Convert the app from “client-only boot” to a structure that can be used by both prerender generation and client hydration.

### Steps
1. Replace the direct `createApp(App).mount('#app')` boot pattern with a `vite-ssg` entry.
2. Move router route records into a reusable export so the SSG entry can consume them directly.
3. Register existing plugins (`router`, `@unhead/vue`, FontAwesome CSS, global stylesheet) through the shared app factory.
4. Ensure app setup does not assume browser globals during initial creation.
5. Keep lazy-loaded page components unless SSG reveals route-splitting issues; no need to eagerly import everything on day one.

### Notes
- This is the foundational refactor. Most later steps depend on it.
- The goal is to minimize page-level changes while changing the app entry shape.

### Done when
- the app builds through an SSG-compatible entrypoint
- the browser still hydrates and navigation still works after first load

---

## Phase 2 - Add deterministic prerender route generation

### Objectives
- Tell the SSG build exactly which routes to generate as HTML.

### Steps
1. Create a single source of truth for prerenderable routes.
2. Include all fixed marketing/legal routes explicitly.
3. Generate blog detail routes from `website/src/data/blogs.json`.
4. Wire the route list into `vite.config.ts` using `ssgOptions.includedRoutes(...)` or an equivalent centralized route manifest.
5. Keep the route manifest reusable so sitemap generation and prerender generation do not drift over time.
6. Decide whether to include `/404` as a generated route or continue generating `404.html` from a fallback copy step.

### Recommended implementation detail
Prefer one shared helper that outputs:
- static routes
- blog routes
- optional metadata like changefreq/priority

That helper can then be reused by:
- SSG route inclusion
- sitemap generation
- future link validation/tests

### Done when
- build output contains separate HTML for all static routes and each blog slug
- adding a new blog post automatically results in a new prerendered page on the next build

---

## Phase 3 - Make pages SSR-safe and hydration-safe

### Objectives
- Remove or isolate code that would break during server-side rendering or cause hydration mismatches.

### Focus areas

#### 1. Browser globals
Audit and guard any direct usage of:
- `window`
- `document`
- `matchMedia`
- direct DOM traversal
- browser-only libraries that assume a DOM at import time

#### 2. Blog Mermaid rendering
`BlogPostPage.vue` is the clearest current SSR-sensitive page.

Planned treatment:
1. Ensure Mermaid is only used on the client.
2. Move Mermaid setup behind `onMounted` and/or a dynamic import if the library touches DOM during module evaluation.
3. Keep the server-rendered HTML as readable fallback content until hydration completes.
4. Ensure server HTML and initial client render produce the same structure before Mermaid mutates the DOM.

#### 3. Hydration mismatch audit
Check for values that can differ between server render and client hydration, such as:
- locale/time formatting differences
- `Date.now()` / random values in templates
- viewport-dependent rendering
- dark mode checks performed before hydration

#### 4. Client-interactive sections
Components like `WaitlistSection.vue` can remain interactive as long as:
- they render deterministic initial markup on the server
- network actions only happen on user interaction

### Done when
- SSG build does not crash on browser-only code
- browser console shows no hydration mismatch warnings on representative routes
- blog pages render meaningful HTML before hydration and still enhance correctly after hydration

---

## Phase 4 - Render SEO/head tags into generated HTML

### Objectives
- Ensure SEO improvements are real, not just content-only.

### Steps
1. Verify every prerendered route resolves its `useSEO(...)` metadata during the SSG pass.
2. Confirm the generated HTML includes, per page:
   - `<title>`
   - description meta tag
   - canonical link
   - Open Graph tags
   - Twitter tags
3. Ensure blog post pages emit post-specific metadata, not generic fallback metadata.
4. Improve 404/noindex handling if needed:
   - `404.html` should not become an indexable content trap
   - consider `noindex` for explicit not-found page output if routed directly
5. Decide whether any pages need structured data later (not mandatory for first rollout, but useful future work for blog posts and product pages).

### Extra validation targets
- homepage
- one compare page
- blog listing page
- one blog detail page
- privacy/terms page

### Done when
- “View Source” on generated HTML shows route-correct metadata with no client execution required

---

## Phase 5 - Integrate SSG into npm build and static hosting output

### Objectives
- Make the existing `npm run build` produce prerendered output without changing the deployment model.

### Steps
1. Add `vite-ssg` dependency and update the build command to invoke SSG generation instead of plain `vite build`.
2. Preserve existing prebuild steps:
   - `copy-assets.js`
   - `build-blogs.js`
3. Re-check the order of post-build steps:
   - generate/copy `404.html`
   - generate sitemap from the same route manifest
4. Verify nested route output works correctly on the chosen static host.
5. If the site is served from domain root, keep absolute-path assumptions as-is; if a base path is ever introduced, ensure SSG and router history config are both base-aware.
6. Update preview instructions if needed so local verification reflects the prerendered output.

### Recommended script end state
Conceptually the build pipeline should become:
1. generate content/assets
2. type-check
3. prerender with SSG
4. create/finalize 404 fallback
5. generate sitemap

### Done when
- `npm run build` alone generates the complete SEO-friendly static output
- deployment target does not need a Node runtime

---

## Phase 6 - Validate output, hydration, and search-engine readiness

### Objectives
- Prove the migration actually improved SEO-readiness and did not regress UX.

### Validation checklist

#### Build artifact validation
1. Inspect generated files under `website/dist/`.
2. Confirm route directories/files exist for:
   - `/`
   - `/features`
   - `/faq`
   - at least one compare page
   - `/blog`
   - each blog slug
3. Confirm HTML includes route content, not just `<div id="app"></div>`.

#### Source-level validation
Run checks like:
- open built HTML directly
- `curl`/static host fetch to verify readable body content
- verify canonical/meta tags in raw HTML

#### Hydration validation
1. Load prerendered pages in the browser.
2. Confirm no hydration mismatch warnings.
3. Confirm internal links still navigate client-side without full page reload.
4. Confirm interactive widgets still work:
   - FAQ expand/collapse
   - screenshot tabs
   - waitlist form submission
   - blog Mermaid enhancement

#### Search-engine readiness validation
1. Confirm `sitemap.xml` contains all prerendered routes.
2. Confirm Open Graph tags are route-specific.
3. Run Lighthouse/PageSpeed checks on representative pages.
4. Optionally submit a test deployment to Google Search Console URL Inspection after rollout.

### Done when
- raw HTML is meaningful for crawlers
- hydrated UX matches current SPA behavior
- no critical SEO or routing regressions remain

---

## Phase 7 - Rollout, docs, and deployment hardening

### Objectives
- Make the new architecture maintainable.

### Steps
1. Document the rendering model in `website/README.md` or project docs:
   - this is now an SSG + hydration site
   - how routes are prerendered
   - how to add new prerenderable pages
2. Document the rule for new pages:
   - default to SSR-safe markup
   - move browser-only code behind client guards
3. Document how blog slugs automatically participate in prerendering.
4. Ensure future route additions update the shared route manifest rather than separate build scripts.
5. If the deployment pipeline is separate, verify it does not rely on the old SPA-only output assumptions.

### Done when
- the build process is understandable to future contributors
- adding a new static page or blog post does not require “hidden” SEO wiring

---

## Risks and Mitigations

### Risk 1: Confusing SSG with runtime SSR
**Impact:** unnecessary complexity, server deployment changes, harder maintenance.

**Mitigation:** keep the solution explicitly static-host-friendly and centered on `vite-ssg`.

### Risk 2: Browser-only libraries break prerender build
**Impact:** build failures or missing pages.

**Mitigation:** isolate such code behind `onMounted`, `import.meta.env.SSR` guards, or dynamic import.

### Risk 3: Hydration mismatches from nondeterministic rendering
**Impact:** console warnings, broken UI, SEO/UX regressions.

**Mitigation:** audit date formatting, dark mode checks, DOM mutations, and runtime-only values.

### Risk 4: Route duplication between prerender and sitemap logic
**Impact:** missing pages in build or sitemap drift.

**Mitigation:** centralize route manifest generation.

### Risk 5: Static hosting fallback behavior changes
**Impact:** direct deep links break or incorrect 404 behavior.

**Mitigation:** preserve `404.html` handling and test direct loads of nested routes on the target host.

---

## Definition of Done

This plan is complete when all of the following are true:

1. `website/npm run build` generates prerendered HTML for all static pages and blog posts.
2. Generated HTML contains real page body content and route-specific SEO tags.
3. The website still hydrates into a functioning Vue SPA after load.
4. Browser-only enhancements work only on the client and do not break prerendering.
5. `sitemap.xml` matches the prerendered route set.
6. Static hosting and deep-link behavior still work.
7. The architecture is documented so future pages remain prerender-friendly.

---

## Suggested Execution Order

1. Phase 0 - confirm scope and route set
2. Phase 1 - refactor app entry for SSG
3. Phase 2 - implement prerender route manifest
4. Phase 3 - fix SSR/hydration hazards
5. Phase 4 - verify generated SEO metadata
6. Phase 5 - finalize build pipeline
7. Phase 6 - validate artifacts and browser behavior
8. Phase 7 - document and harden rollout

---

## Nice-to-Have Follow-Ups (Not Required for First Delivery)

1. Add JSON-LD structured data for:
   - product/software homepage
   - blog posts
   - FAQ page
2. Add a small automated test that asserts built HTML for selected routes contains expected headings/meta tags.
3. Add broken-link validation across prerendered pages.
4. Add a CI step that fails if a new blog post exists but no prerendered page is generated.
