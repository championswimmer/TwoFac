# TwoFac Website

The `website/` package is the marketing site and blog for TwoFac.

## Rendering model

This site uses **Vue 3 + Vite + `vite-ssg`**.

That means production builds are:
- **pre-rendered at build time** for SEO and social crawlers
- shipped as **static files only**
- hydrated in the browser so the site continues to behave like a Vue SPA after first load

There is **no long-running Node SSR server** in production.

## Commands

```bash
npm install
npm run dev
npm run build
npm run preview
npm run validate:ssg
```

### What `npm run build` does

1. copies static assets into `public/`
2. generates `src/data/blogs.json` from blog sources
3. type-checks the app
4. runs `vite-ssg build`
5. finalizes `dist/404.html` for static-host fallbacks
6. generates `dist/sitemap.xml`

## Route prerendering

The prerender route inventory is centralized in:

- `scripts/route-manifest.js`

That file is the single source of truth for:
- static marketing/legal routes
- blog post routes derived from `src/data/blogs.json`
- sitemap entries

### Current prerendered routes

Static routes:
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
- `/404`

Dynamic prerendered routes:
- every `/blog/:slug` entry generated into `src/data/blogs.json`

## Output shape

The SSG build uses **nested directory output**:

- `/features` → `dist/features/index.html`
- `/blog/some-post` → `dist/blog/some-post/index.html`
- `/404` → `dist/404/index.html`

A root-level `dist/404.html` is also created so static hosts like GitHub Pages can use it as a fallback page for deep links.

## SEO and head tags

Route metadata is declared through `src/composables/useSEO.ts`.

The composable supports:
- title
- description
- canonical URL
- Open Graph tags
- Twitter tags
- `noindex` handling
- article publish time for blog posts

Because the site is prerendered, these tags are written directly into the generated HTML.

## SSR-safe / hydration-safe rules for new pages

When adding new pages or components:

1. Prefer deterministic initial markup.
2. Do **not** read browser-only globals (`window`, `document`, `matchMedia`, etc.) during setup/render.
3. Put browser-only behavior behind `onMounted`, dynamic imports, or explicit guards like:
   - `if (import.meta.env.SSR) return`
   - `if (typeof window === 'undefined') return`
4. Avoid non-deterministic template values such as:
   - `Date.now()`
   - `Math.random()`
   - locale/timezone-sensitive formatting without fixed options
5. If a page needs prerendering, update both:
   - `src/router/routes.ts`
   - `scripts/route-manifest.js`

## Blog posts

Blog pages are generated from build-time content.

Key files:
- `scripts/build-blogs.js`
- `src/data/blogs.json`
- `src/pages/BlogPage.vue`
- `src/pages/BlogPostPage.vue`

Adding a new blog post automatically causes the next `npm run build` to:
- include the post in the blog index
- prerender the post route
- include the post in `sitemap.xml`

## Validation

After building, run:

```bash
npm run validate:ssg
```

This checks representative generated HTML for:
- expected route files
- prerendered content in the body
- route-specific SEO tags
- blog/article metadata
- `404` noindex behavior
- sitemap coverage

## Deployment notes

This build is designed for static hosting.

Requirements for the host:
- serve files from `dist/`
- preserve nested route directories
- use `404.html` as fallback for unknown paths when applicable

If you add routes later, keep the router and route manifest in sync so prerender output, sitemap generation, and fallback handling do not drift.
