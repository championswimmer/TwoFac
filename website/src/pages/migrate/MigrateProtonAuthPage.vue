<script setup lang="ts">
import { useSEO } from '../../composables/useSEO'
import MigratePageLayout from '../../components/MigratePageLayout.vue'
import type { HowToStep } from '../../components/MigratePageLayout.vue'

useSEO({
  title: 'How to Migrate from Proton Authenticator to TwoFac: 2026 Migration Guide',
  description: 'Step-by-step guide to migrate your 2FA accounts from Proton Authenticator to TwoFac using JSON exports.',
  canonicalPath: '/migrate/proton-authenticator',
})

const howToSteps: HowToStep[] = [
  { name: 'Export from Proton', text: 'Open Proton Authenticator or Proton Pass. Go to Settings and find the Export option. Choose to export your data in unencrypted JSON format and save the file securely to your device.' },
  { name: 'Import into TwoFac', text: 'Open the TwoFac app on your device. Navigate to Settings and select the Import option. Choose the JSON file you just exported from Proton. TwoFac will parse the file and securely import all your 2FA accounts.' },
  { name: 'Secure Your Setup', text: 'Delete the unencrypted JSON export file permanently to protect your secrets. Enable Biometric Lock in TwoFac and set up an Encrypted Backup.' },
]
</script>

<template>
  <MigratePageLayout
    app-name="Proton Authenticator"
    hero-description="Migrating from Proton Authenticator to TwoFac is easy using Proton's JSON export functionality. You can quickly bring all your 2FA accounts into TwoFac's broader ecosystem."
    :how-to-steps="howToSteps"
  >
    <h2>Step 1: Export from Proton</h2>
    <p>
      Proton Authenticator and Proton Pass allow you to export your data in a standard format.
    </p>
    <ol>
      <li>Open <strong>Proton Authenticator</strong> or <strong>Proton Pass</strong> on your device.</li>
      <li>Go to <strong>Settings</strong> (gear icon) and select <strong>Export</strong>.</li>
      <li>Choose the <strong>unencrypted JSON</strong> format. This creates a file containing your 2FA secrets that TwoFac can read.</li>
      <li>Save the file to a secure location on your device.</li>
    </ol>
    <p>
      <em>Security Warning:</em> The unencrypted JSON export contains your raw 2FA secrets. Anyone with this file can generate your login codes. <strong>Keep it secure and delete it permanently</strong> once the import is complete.
    </p>

    <h2>Step 2: Import into TwoFac</h2>
    <ol>
      <li>Open the <strong>TwoFac</strong> app.</li>
      <li>Navigate to <strong>Settings</strong>.</li>
      <li>Under the <em>Backup & Restore</em> section, choose the <strong>Import</strong> option.</li>
      <li>Select the JSON file you exported from Proton.</li>
      <li>TwoFac will parse the file and securely import all your 2FA accounts. Review your account list to ensure everything transferred correctly.</li>
    </ol>

    <h2>Step 3: Secure Your Setup</h2>
    <p>
      Now that your accounts are safely in TwoFac, make sure to take advantage of its security features:
    </p>
    <ul>
      <li><strong>Delete the JSON export file:</strong> Ensure the plain text export file is permanently removed from your computer or phone.</li>
      <li><strong>Enable Biometric Lock:</strong> Protect TwoFac using fingerprint or face recognition.</li>
      <li><strong>Create an Encrypted Backup:</strong> Generate an encrypted backup directly from TwoFac so future backups are secured end-to-end with a passkey.</li>
    </ul>

    <h2>Step 4: Keep Proton Until You Are Confident</h2>
    <p>
      Exporting from Proton does not invalidate your tokens, so you can keep Proton Authenticator installed while you confirm everything works in TwoFac. Once you trust your new setup, you can remove Proton or keep it temporarily as a fallback.
    </p>
  </MigratePageLayout>
</template>
