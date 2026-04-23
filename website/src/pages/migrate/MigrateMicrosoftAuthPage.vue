<script setup lang="ts">
import { useSEO } from '../../composables/useSEO'
import MigratePageLayout from '../../components/MigratePageLayout.vue'

useSEO({
  title: 'How to Migrate from Microsoft Authenticator to TwoFac',
  description: 'Step-by-step guide to migrate your 2FA accounts from Microsoft Authenticator to TwoFac manually.',
  canonicalPath: '/migrate/microsoft-authenticator',
})
</script>

<template>
  <MigratePageLayout
    app-name="Microsoft Authenticator"
    hero-description="Microsoft Authenticator intentionally restricts exporting 2FA secrets. Because there is no official export button, standard migration requires manually transferring your accounts, service by service."
  >
    <h2>Why is this manual?</h2>
    <p>
      Microsoft Authenticator does not provide a built-in "Export" feature to move accounts to other apps like Google Authenticator or TwoFac. This is designed as a security measure but creates a vendor lock-in scenario, making migrating away from the app difficult.
    </p>

    <h2>Step 1: The Manual Method (Recommended)</h2>
    <p>The standard and safest way to move your accounts is to manually re-register them in TwoFac.</p>
    <ol>
      <li><strong>Log in</strong> to each service (e.g., Google, GitHub, Facebook) using your current Microsoft Authenticator code.</li>
      <li>Go to the <strong>Security / 2FA Settings</strong> of that website.</li>
      <li><strong>Disable 2FA</strong> temporarily, and then <strong>Re-enable it</strong>.</li>
      <li>When the website shows a new <strong>QR Code</strong>, scan it with the <strong>TwoFac</strong> app.</li>
      <li><strong>Test the new code</strong> to make sure TwoFac is correctly registered.</li>
      <li><strong>Delete</strong> the old entry from Microsoft Authenticator once you've confirmed TwoFac works.</li>
    </ol>
    <p><em>Tip: Don't forget to save the <strong>Recovery Codes</strong> provided by the websites when re-setting up 2FA!</em></p>

    <h2>Step 2: Securing Your New TwoFac Setup</h2>
    <p>Now that your accounts are finally free from vendor lock-in, make sure to secure them:</p>
    <ul>
      <li><strong>Enable Biometric Lock</strong> in TwoFac to ensure only you can access your codes.</li>
      <li><strong>Create an Encrypted Backup</strong> of your accounts to avoid ever needing to perform manual re-setup again! Store the backup securely.</li>
    </ul>

    <h2>Advanced: The "Rooted Android" Method</h2>
    <p>
      If you are a power user with a rooted Android device or comfortable using an Android emulator, you can extract the database file where Microsoft stores the secrets.
    </p>
    <ol>
      <li>Enable "Cloud Backup" in Microsoft Authenticator on your phone, then restore that backup onto a rooted Android device or emulator.</li>
      <li>Use a file explorer with root access to locate the database at: <code>/data/data/com.azure.authenticator/databases/PhoneFactor</code>.</li>
      <li>Extract this file and open it with an SQLite browser.</li>
      <li>Look for the <code>accounts</code> table. The <code>oath_secret_key</code> column contains the raw TOTP seeds, which you can manually add to TwoFac by copying the secrets.</li>
    </ol>
  </MigratePageLayout>
</template>
