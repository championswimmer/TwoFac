---
name: Migrate From Pages Plan
status: Completed
progress:
  - "[x] Phase 1 - Write the plan"
  - "[x] Phase 2 - Create MigratePageLayout.vue"
  - "[x] Phase 3 - Create individual migration pages for each app"
  - "[x] Phase 4 - Add the 'Migrate from' column in Footer.vue"
  - "[x] Phase 5 - Add links from compare pages to migrate pages"
  - "[x] Phase 6 - Update router and route manifest"
---

# Migrate From Pages Plan

## Goal
Based on research, provide step-by-step guides on how users can migrate their 2FA secrets from 2FAS, Ente Auth, Google Authenticator, Microsoft Authenticator, Authy, and Bitwarden Authenticator over to TwoFac.

These guides will live in the `website/` project under `/migrate/<app>` and be linked from both the site footer and the respective comparison pages.

## Phase 1: Research & Plan
Research the export flow for each app:
1. **2FAS**: Export to `.2fas` file (unencrypted JSON). TwoFac can import this directly if the user imports via 2FAS JSON import.
2. **Ente Auth**: Export to Plain Text (`otpauth://` URIs) via Settings > Data > Export Codes. TwoFac can import these.
3. **Google Authenticator**: Export accounts generates a QR code. TwoFac can scan Google Authenticator QR codes directly.
4. **Microsoft Authenticator**: No official export. Requires manual re-setup by logging into each service and re-enabling 2FA. (Or advanced rooted extraction).
5. **Authy**: No official export. Requires manual re-setup per service. (Or advanced community scripts).
6. **Bitwarden Authenticator**: Export to JSON via Settings > Export. Contains `otpauth://` URIs or standard JSON format.

## Phase 2: Layout Component
Create `website/src/components/MigratePageLayout.vue` to standardise the look and feel of the migration guides, featuring a hero section, step-by-step instructions, and a call-to-action.

## Phase 3: Migration Pages
Create the following pages in `website/src/pages/migrate/`:
- `Migrate2FASPage.vue`
- `MigrateEnteAuthPage.vue`
- `MigrateGoogleAuthPage.vue`
- `MigrateMicrosoftAuthPage.vue`
- `MigrateAuthyPage.vue`
- `MigrateBitwardenPage.vue`

## Phase 4: Footer Updates
Update `website/src/components/Footer.vue` to include a new "Migrate from" column next to the "Compare" column, linking to each of the above pages.

## Phase 5: Compare Page Cross-links
Modify the existing compare pages in `website/src/pages/compare/` to add a link pointing to the respective migration guide. This can be placed in the CTA section or a new section.

## Phase 6: Routing & SSG
Register the new routes in `website/src/router/routes.ts` and add them to `website/scripts/route-manifest.js` to ensure they are statically generated.
