<script setup lang="ts">
import { useSEO } from '../../composables/useSEO'
import MigratePageLayout from '../../components/MigratePageLayout.vue'
import type { HowToStep } from '../../components/MigratePageLayout.vue'

useSEO({
  title: 'How to Migrate from Authy to TwoFac: 2026 Migration Guide',
  description: 'Step-by-step guide to migrate your 2FA accounts from Authy to TwoFac manually.',
  canonicalPath: '/migrate/authy',
})

const howToSteps: HowToStep[] = [
  { name: 'The Manual Method', text: 'Log in to each service using your current Authy code. Go to the Security/2FA settings of that account. Disable 2FA temporarily. Re-enable 2FA, which will show you a new QR code. Scan this new QR code using the TwoFac app. Test the new code to make sure TwoFac is correctly registered.' },
  { name: 'Verify Your New Setup', text: 'Do not delete your Authy account until you have verified that every single token works in TwoFac. Enable Biometric Lock in TwoFac and create an Encrypted Backup.' }
]
</script>

<template>
  <MigratePageLayout
    app-name="Authy"
    hero-description="Authy famously blocks users from exporting their 2FA tokens. The official migration method requires manually moving your accounts, ensuring you're no longer trapped."
    :how-to-steps="howToSteps"
  >
    <h2>Why is this manual?</h2>
    <p>
      Exporting 2FA secrets from Authy is notoriously difficult because the app does not provide a native export feature. Since the Authy Desktop app was discontinued in 2024, older community workarounds no longer function, making the manual method the only reliable and official way to migrate.
    </p>

    <h2>Step 1: The Manual Method (Safest & Recommended)</h2>
    <p>
      This is the only official way to ensure you don't lose access to your accounts. It is tedious but guarantees your new app will work securely.
    </p>
    <ol>
      <li><strong>Log in</strong> to each service (e.g., Google, GitHub, Amazon) using your current Authy code.</li>
      <li>Go to the <strong>Security/2FA settings</strong> of that account.</li>
      <li><strong>Disable 2FA</strong> temporarily.</li>
      <li><strong>Re-enable 2FA</strong>, which will show you a new QR code.</li>
      <li>Scan this new QR code using the <strong>TwoFac</strong> app.</li>
      <li><strong>Test the new code</strong> to make sure TwoFac is correctly registered.</li>
    </ol>
    <p>
      <em>Warning:</em> Some services (like Twitch or Gemini) historically used "Authy-only" tokens (7-digit codes) that cannot be exported to other apps. For these, you must disable 2FA on the site and switch to standard "Authenticator App" (TOTP) method before migrating to TwoFac.
    </p>

    <h2>Step 2: Verify Your New Setup</h2>
    <p>
      <strong>Don't Delete Authy Yet:</strong> Do not delete your Authy account until you have verified that every single token works in TwoFac and that you haven't missed any essential services.
    </p>
    <ul>
      <li><strong>Enable Biometric Lock</strong> in TwoFac to ensure only you can access your codes.</li>
      <li><strong>Create an Encrypted Backup</strong> of your accounts to never repeat this tedious process again! Store the backup securely.</li>
    </ul>

    <h2>Advanced: Using an Export Script (Technical)</h2>
    <p>
      If you have dozens of accounts, community-developed tools exist that register as a "new device" on your Authy account to pull the data. Use these at your own risk.
    </p>
    <ul>
      <li><strong>Authy-GDPR-Export-Decryption:</strong> Scripts that use Authy's GDPR data export to decrypt your tokens.</li>
      <li><strong>authy-export (Go library):</strong> This tool attempts to enroll itself as a new device on your account to fetch the TOTP seeds.</li>
      <li><strong>Rooted Android:</strong> If you have a rooted Android phone, you can extract the secrets directly from Authy's local storage by using Aegis or 2FAS (and then exporting it to TwoFac).</li>
    </ul>
  </MigratePageLayout>
</template>
