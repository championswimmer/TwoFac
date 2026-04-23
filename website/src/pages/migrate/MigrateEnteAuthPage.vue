<script setup lang="ts">
import { useSEO } from '../../composables/useSEO'
import MigratePageLayout from '../../components/MigratePageLayout.vue'
import type { HowToStep } from '../../components/MigratePageLayout.vue'

useSEO({
  title: 'How to Migrate from Ente Auth to TwoFac: 2026 Migration Guide',
  description: 'Step-by-step guide to migrate your 2FA accounts from Ente Auth to TwoFac using Plain Text exports.',
  canonicalPath: '/migrate/ente-auth',
})

const howToSteps: HowToStep[] = [
  { name: 'Export from Ente Auth', text: 'Open the Ente Auth app. Open the Navigation Menu and select Settings or Data. Click or tap on Export Codes. Choose the Plain Text Export format. Save the file to a secure location on your device.' },
  { name: 'Import into TwoFac', text: 'Open the TwoFac app. Navigate to Settings and find the Import section. Select the plain text (.txt) file you exported from Ente Auth. TwoFac will parse the URIs and store your accounts.' },
  { name: 'Secure Your Setup', text: 'Delete the plain text export file permanently. Enable Biometric Lock in TwoFac. Set up an Encrypted Backup directly from TwoFac.' }
]
</script>

<template>
  <MigratePageLayout
    app-name="Ente Auth"
    hero-description="Migrating from Ente Auth to TwoFac is incredibly easy using the Ente Auth export functionality. TwoFac officially supports importing Ente Auth text exports."
    :how-to-steps="howToSteps"
  >
    <h2>Step 1: Export from Ente Auth</h2>
    <p>You can export your secrets using either the Ente Auth mobile or desktop apps.</p>
    <ol>
      <li>Open the <strong>Ente Auth</strong> app on your device.</li>
      <li>Open the <strong>Navigation Menu</strong> (three horizontal lines or "hamburger" icon) in the top-left corner.</li>
      <li>Select <strong>Settings</strong> (or <strong>Data</strong>).</li>
      <li>Click or tap on <strong>Export Codes</strong>.</li>
      <li>Choose the <strong>Plain Text Export</strong> format.
        <ul>
          <li><em>Why Plain Text?</em> This generates an unencrypted <code>.txt</code> file containing a list of your secrets in standard <code>otpauth://</code> URI format, which TwoFac uses.</li>
        </ul>
      </li>
      <li>Save the file to a secure location on your device.</li>
    </ol>
    <p>
      <em>Security Warning:</em> The unencrypted export file contains your raw 2FA secrets. Never share or upload this file to insecure cloud storage. <strong>Delete it permanently</strong> as soon as you are done migrating.
    </p>

    <h2>Step 2: Import into TwoFac</h2>
    <ol>
      <li>Open the <strong>TwoFac</strong> app on your device.</li>
      <li>Navigate to <strong>Settings</strong>.</li>
      <li>Find the <strong>Import</strong> section.</li>
      <li>Select the plain text (<code>.txt</code>) file you exported from Ente Auth.</li>
      <li>TwoFac will parse the <code>otpauth://</code> URIs and securely store all your accounts in your vault.</li>
    </ol>

    <h2>Step 3: Secure Your Setup</h2>
    <p>
      Because your secrets are now in TwoFac, ensure you take advantage of its security features:
    </p>
    <ul>
      <li><strong>Delete the <code>.txt</code> file:</strong> Make sure the plain text export file is permanently removed from your computer or phone.</li>
      <li><strong>Enable Biometric Lock:</strong> Secure TwoFac using fingerprint or face recognition so nobody but you can open it.</li>
      <li><strong>Set up an Encrypted Backup:</strong> Generate an encrypted backup directly from TwoFac so that your future backups are secured end-to-end with a passkey.</li>
    </ul>

    <h2>Step 4: Uninstall Ente Auth (Optional)</h2>
    <p>
      After confirming that your codes are generating correctly in TwoFac, you can delete your Ente Auth account and uninstall the app. Alternatively, since this is a simple export, your codes will continue to work in Ente Auth. You can keep both authenticator apps installed and use them side-by-side if you prefer!
    </p>
  </MigratePageLayout>
</template>
