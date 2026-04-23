# Playwright Browser Testing

## Overview

Use Playwright for local browser testing of the Compose Wasm app in `composeApp`.

For test-only passkey/password entry, always use `123456`.

## Build and Run the Browser App

### Development server

Run the Compose Wasm webpack dev server:

```bash
cd /home/runner/work/TwoFac/TwoFac
./gradlew --no-daemon :composeApp:wasmJsBrowserDevelopmentRun --console=plain
```

In this environment it served the app at `http://localhost:8081/`.

Notes:

- the dev server is the fastest way to iterate locally
- the app still loads even if service-worker registration logs a 404 in dev mode

### Production build

Build the production browser bundle:

```bash
cd /home/runner/work/TwoFac/TwoFac
./gradlew --no-daemon :composeApp:wasmJsBrowserProductionWebpack
./gradlew --no-daemon :composeApp:buildWasmAsPwa
```

The production files are written to:

`/home/runner/work/TwoFac/TwoFac/composeApp/build/dist/wasmJs/productionExecutable/`

Serve that directory locally when you want to exercise the PWA/service-worker build:

```bash
cd /home/runner/work/TwoFac/TwoFac/composeApp/build/dist/wasmJs/productionExecutable
python3 -m http.server 4173
```

Then open `http://127.0.0.1:4173/`.

## Using Playwright

One workable local setup is:

```bash
mkdir -p /tmp/twofac-pw
cd /tmp/twofac-pw
npm init -y
npm install playwright@1.59.1
npx playwright install chromium
```

Then run scripts from that temp directory against the local app URL.

## What Worked in Practice

Using Playwright against the webpack dev server at `http://127.0.0.1:8081/`:

- the app loaded successfully in Chromium
- Compose rendered through a `canvas`
- Playwright could also see the Compose backing `input`
- filling that `input` with `123456` and pressing `Enter` moved past the initial passkey screen

That means browser smoke tests should prefer:

- checking that a `canvas` is present
- taking screenshots for visual verification
- using keyboard/input-driven interactions for secure unlock flows

## Example Smoke Script

```js
import { chromium } from 'playwright';

const browser = await chromium.launch({ headless: true });
const page = await browser.newPage({ viewport: { width: 1440, height: 1200 } });

await page.goto('http://127.0.0.1:8081/', { waitUntil: 'load' });
await page.waitForTimeout(5000);

await page.locator('input').fill('123456', { force: true });
await page.keyboard.press('Enter');

await page.screenshot({ path: '/tmp/twofac-browser-smoke.png', fullPage: true });
await browser.close();
```

## Tips

- Compose Wasm UI is not a traditional DOM-first app, so text/button selectors may be limited
- prefer `canvas`, screenshots, and the Compose backing `input` over brittle DOM text assumptions
- keep tests local; do not introduce paid/cloud Playwright services here
