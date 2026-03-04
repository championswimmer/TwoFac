---
name: GitHub Actions PWA Deployment to GitHub Pages
status: Planned
progress:
  - "[ ] Phase 0 - Enable PWA Auto-Update"
  - "[ ] Phase 1 - Define GitHub Actions Workflow for PWA"
  - "[ ] Phase 2 - Configure CNAME and GitHub Pages Action"
  - "[ ] Phase 3 - Test and Validate"
---

# GitHub Actions PWA Deployment to GitHub Pages

## Goal
Automate the deployment of the Kotlin Wasm PWA web application to GitHub Pages (`gh-pages` branch) using GitHub Actions. The workflow should only trigger on tag pushes, configure a custom domain (`web.twofac.app`), and ensure the deployed PWA auto-updates itself when a new version is released.

## Current State & Architecture
- The app builds for the web via the `wasmJs` target.
- The web build outputs to `composeApp/build/dist/wasmJs/productionExecutable/`.
- A `workbox-config-for-wasm.js` file exists in `composeApp/` for generating the Service Worker.
- Service worker registration logic is in `composeApp/src/webMain/resources/registerServiceWorker.js`.

## Detailed Phase-by-Phase Roadmap

### Phase 0 - Enable PWA Auto-Update
To ensure the PWA automatically updates when a new version (tag) is deployed, we need to instruct Workbox to skip the waiting phase and force clients to claim the new service worker.

1. **Update Workbox Configuration:**
   - Edit `composeApp/workbox-config-for-wasm.js`.
   - Add `skipWaiting: true` and `clientsClaim: true` to the exported configuration object.
2. **Update Service Worker Registration:**
   - Edit `composeApp/src/webMain/resources/registerServiceWorker.js`.
   - Add a listener for the `controllerchange` event on `navigator.serviceWorker` to automatically reload the page when the new service worker takes control:
     ```javascript
     let refreshing = false;
     navigator.serviceWorker.addEventListener('controllerchange', () => {
         if (!refreshing) {
             window.location.reload();
             refreshing = true;
         }
     });
     ```

### Phase 1 - Define GitHub Actions Workflow for PWA
1. **Create Workflow File:**
   - Create `.github/workflows/deploy-pwa.yml`.
2. **Trigger Condition:**
   - Configure the workflow to trigger strictly on tag pushes, to prevent unnecessary builds on `master` or PRs:
     ```yaml
     on:
       push:
         tags:
           - '*'
     ```
3. **Build Steps:**
   - Checkout code (`actions/checkout@v4`).
   - Setup Java/JDK (e.g., `actions/setup-java@v4` with JVM version from project).
   - Setup Node.js (`actions/setup-node@v4` to provide `npx` for Workbox).
   - Setup Gradle caching (can use `gradle/actions/setup-gradle@v3`).
   - Run the Compose web build task: `./gradlew :composeApp:wasmJsBrowserDistribution`.
   - Run Workbox to inject the service worker into the generated distribution:
     `npx workbox-cli generateSW composeApp/workbox-config-for-wasm.js`

### Phase 2 - Configure CNAME and GitHub Pages Action
1. **Deploy Step:**
   - Use the popular `peaceiris/actions-gh-pages@v4` action to push the distribution folder to the `gh-pages` branch.
2. **Action Configuration:**
   - Specify the `publish_dir` to point to the Wasm output (`composeApp/build/dist/wasmJs/productionExecutable/`).
   - Use the `cname` input parameter to automatically write the `CNAME` file to the root of the `gh-pages` branch during deployment.
   ```yaml
   - name: Deploy to GitHub Pages
     uses: peaceiris/actions-gh-pages@v4
     with:
       github_token: ${{ secrets.GITHUB_TOKEN }}
       publish_dir: composeApp/build/dist/wasmJs/productionExecutable/
       cname: web.twofac.app
   ```

### Phase 3 - Test and Validate
1. **Permissions Check:**
   - Ensure the `GITHUB_TOKEN` has `contents: write` permissions in the repository settings to allow pushing to the `gh-pages` branch.
2. **Trigger the Pipeline:**
   - Push a test tag (e.g., `v1.0.0-test`).
3. **Validate Deployment:**
   - Verify the action completes successfully.
   - Verify that the `gh-pages` branch is updated and contains both the Wasm distribution files and the `CNAME` file.
   - Open `web.twofac.app` in the browser.
4. **Validate Auto-Update:**
   - Push a subsequent tag with a minor visual change.
   - Revisit the app and verify the `controllerchange` event forces a page reload fetching the new assets from the new service worker.