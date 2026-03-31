# Logo — Canonical Source

This directory is the **single source of truth** for all TwoFac logo assets.

| File | Size | Usage |
|---|---|---|
| `twofac_logo_32.png`  | 32 × 32 px  | Favicon, small UI |
| `twofac_logo_64.png`  | 64 × 64 px  | Mid-size UI |
| `twofac_logo_128.png` | 128 × 128 px | Standard icon |
| `twofac_logo_512.png` | 512 × 512 px | Store listings, high-DPI |

## Copies elsewhere in the repo

Other directories contain derived copies that should always match the files here:

- `docs/` — used directly in `README.md` (kept in sync manually)
- `website/public/` — copied at build time by `website/scripts/copy-assets.js`
- `composeApp/src/commonMain/composeResources/drawable/` — app runtime resources

> **Do not edit logo files in the derivative locations.**
> Update assets here in `logo/` and let the build/sync steps propagate them.
