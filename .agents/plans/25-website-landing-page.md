---
name: TwoFac Landing Page Website Plan
status: Planned
progress:
  - "[ ] Phase 1: Project Initialization & Configuration"
  - "[ ] Phase 2: Build-Time Blog Pipeline (The 'Blog Engine')"
  - "[ ] Phase 3: Core UI & Layouts"
  - "[ ] Phase 4: General Pages Implementation"
  - "[ ] Phase 5: SEO Comparison Pages (/compare/*)"
  - "[ ] Phase 6: Polish, Testing, & Launch"
---

# TwoFac Landing Page Website Plan

## 1. Goal
Create a modern, fast, and SEO-optimized landing page for the `twofac` open-source authenticator app. The website will be located in the `./website` directory and built with Vue.js, TypeScript, Vite, Vue Router, and Tailwind CSS. It will showcase the app's features, offer downloads, host a blog (sourced from existing documentation), and include dedicated SEO comparison pages against major competitors.

## 2. Tech Stack & Tooling
- **Framework**: Vue.js 3 (Composition API) + TypeScript
- **Build Tool**: Vite
- **Routing**: Vue Router 4
- **Styling**: Tailwind CSS + PostCSS + Autoprefixer
- **SEO/Head Management**: `@vueuse/head` or `unhead` for meta tags (critical for SEO pages)
- **Markdown/Blog Processing**: Custom build-time script (e.g., Node.js script using `gray-matter` for frontmatter, `marked` or `markdown-it` for HTML conversion) to automatically pull `.md` files and images from `../docs/blogs/` into the Vite build pipeline.

## 3. Directory Structure (Proposed for `./website`)
```text
website/
├── scripts/
│   └── build-blogs.js       # Pre-build script to parse docs/blogs into JSON/assets
├── src/
│   ├── assets/              # Static assets (logos, screenshots)
│   ├── components/          # Reusable UI (Navbar, Footer, FeatureCard, FAQItem)
│   ├── composables/         # Shared state/logic (e.g., useSEOHeader)
│   ├── layouts/             # MainLayout, BlogLayout
│   ├── pages/
│   │   ├── Home.vue
│   │   ├── Features.vue
│   │   ├── Download.vue
│   │   ├── GettingStarted.vue
│   │   ├── FAQ.vue
│   │   ├── blogs/           # Auto-generated/routed blog pages
│   │   │   ├── Index.vue    # Blog listing
│   │   │   └── [slug].vue   # Individual blog post
│   │   └── compare/         # SEO-focused comparison pages
│   │       ├── 2fas.vue
│   │       ├── ente-auth.vue
│   │       ├── bitwarden.vue
│   │       ├── google-authenticator.vue
│   │       └── microsoft-authenticator.vue
│   ├── router/              # Vue Router configuration
│   ├── App.vue
│   └── main.ts
├── tailwind.config.js
├── vite.config.ts
└── package.json
```

## 4. Phase-by-Phase Execution Plan

### Phase 1: Project Initialization & Configuration
1. Scaffold a Vite + Vue + TS project inside the `./website` directory.
2. Install and configure Tailwind CSS. Set up brand colors, typography, and dark mode support (common in dev/security tools).
3. Set up Vue Router with history mode.
4. Install `unhead` for managing SEO meta tags (title, description, og:image) dynamically across pages.

### Phase 2: Build-Time Blog Pipeline (The "Blog Engine")
1. Create a Node.js script (`scripts/build-blogs.js`) to run before the Vite build (`npm run prebuild`/`npm run predev`).
2. The script will:
   - Read all `.md` files from `../docs/blogs/`.
   - Parse frontmatter (title, date, author, description, hero_image).
   - Convert markdown content to HTML.
   - Copy any referenced images from `../docs/blogs/` to the website's `public/images/blogs/` directory.
   - Generate a `blogs.json` file in `src/assets/` containing the metadata and content, which the Vue app will consume to render `pages/blogs/Index.vue` and `pages/blogs/[slug].vue`.

### Phase 3: Core UI & Layouts
1. Build `MainLayout.vue` including a sticky top `Navbar` and a comprehensive `Footer` (with links to GitHub, Discord, Twitter, Legal, etc.).
2. Develop common components:
   - `HeroSection.vue` (Headline, sub-headline, CTA buttons for download/GitHub).
   - `FeatureCard.vue` (Icon, Title, Description - e.g., Open Source, E2E Encryption, Multi-Device Sync).
   - `PlatformBadge.vue` (Android, iOS, CLI, Web, Desktop).
   - `ComparisonTable.vue` (For the SEO pages to compare features side-by-side).

### Phase 4: General Pages Implementation
1. **Home (`/`)**: High-converting hero section, social proof/trust badges, quick feature grid, multi-platform showcase (screenshots), and a final CTA.
2. **Features (`/features`)**: Deep dive into technical capabilities (e.g., WearOS sync, CLI tool, encrypted backups, offline capabilities). Draw inspiration from Ente Auth's multi-device focus and 2FAS's security emphasis.
3. **Getting Started (`/getting-started`)**: Step-by-step onboarding guide. How to install, how to import from Google Auth/Authy, how to set up backups.
4. **Download (`/download`)**: Direct links to App Store, Google Play, GitHub Releases (for Desktop/CLI/Wasm), and browser extensions.
5. **FAQ (`/faq`)**: Expandable accordion list answering common questions (e.g., "What happens if I lose my phone?", "Is the sync end-to-end encrypted?", "How do I migrate?").

### Phase 5: SEO Comparison Pages (`/compare/*`)
*Crucial for capturing high-intent search traffic (e.g., "twofac vs google authenticator").*
1. Design a standardized template (`ComparisonLayout`) that highlights TwoFac's strengths (Open Source, E2EE, Cross-platform, CLI support) against competitors.
2. Build specific pages:
   - **vs 2FAS**: Focus on TwoFac's native desktop/CLI apps and WearOS offline sync vs 2FAS's browser extension approach.
   - **vs Ente Auth**: Highlight architectural differences, CLI capabilities, or self-hosting features if applicable.
   - **vs Bitwarden Authenticator**: Compare standalone 2FA vs integrated password manager 2FA.
   - **vs Google Authenticator**: Emphasize missing features in Google Auth (No E2E encrypted cloud sync by default, no desktop apps, proprietary).
   - **vs Microsoft Authenticator**: Emphasize Open Source, privacy (no tracking), and lightweight nature.

### Phase 6: Polish, Testing, & Launch
1. **SEO Optimization**: Ensure every page has unique meta titles, descriptions, canonical URLs, and OpenGraph tags.
2. **Performance**: Audit with Lighthouse. Ensure images are optimized (WebP) and lazy-loaded. Tailwind purges unused CSS.
3. **Responsive Design**: Test extensively on mobile (critical for an authenticator app landing page).
4. **CI/CD Integration**: Add a GitHub Action to automatically build and deploy the `./website` folder to GitHub Pages, Cloudflare Pages, or Vercel on push to the `main` branch.