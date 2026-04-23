<script setup lang="ts">
import { useSEO } from '../../composables/useSEO'
import MigratePageLayout from '../../components/MigratePageLayout.vue'

useSEO({
  title: 'How to Migrate from 2FAS to TwoFac',
  description: 'Step-by-step guide to migrate your 2FA accounts from 2FAS to TwoFac using the 2FAS export feature.',
  canonicalPath: '/migrate/2fas',
})
</script>

<template>
  <MigratePageLayout
    app-name="2FAS"
    hero-description="Migrating from 2FAS to TwoFac is fast and fully supported. You can export your tokens from 2FAS and import them directly into TwoFac."
  >
    <h2>Step 1: Export from 2FAS</h2>
    <ol>
      <li>Open the <strong>2FAS</strong> app on your mobile device.</li>
      <li>Tap the <strong>Settings</strong> (gear icon) or the Menu button.</li>
      <li>Select <strong>2FAS Backup</strong>.</li>
      <li>Tap <strong>Export to file</strong>.</li>
      <li>
        <strong>Important:</strong> You will be asked if you want to set a password.
        <ul>
          <li><strong>Without a password:</strong> Choose to export without a password. The file will be a plain-text JSON file (<code>.2fas</code>). TwoFac requires this unencrypted JSON format to successfully read and import your codes.</li>
        </ul>
      </li>
      <li>Save the <code>.2fas</code> file to your device's storage.</li>
    </ol>
    <p>
      <em>Security Warning:</em> The unencrypted <code>.2fas</code> file contains your raw 2FA keys. Anyone who gets access to this file can generate your login codes. <strong>Keep it safe during the transfer, and permanently delete the unencrypted file</strong> as soon as you have finished importing it to TwoFac.
    </p>

    <h2>Step 2: Import into TwoFac</h2>
    <ol>
      <li>Open the <strong>TwoFac</strong> app on your device.</li>
      <li>Go to <strong>Settings</strong>.</li>
      <li>Under the <em>Backup & Restore</em> section, find the option to <strong>Import</strong>.</li>
      <li>Select the <code>.2fas</code> file you exported in the previous step.</li>
      <li>TwoFac will instantly import your accounts! Review your account list to ensure everything transferred correctly.</li>
    </ol>

    <h2>Step 3: Clean Up and Secure Your New Setup</h2>
    <p>
      Now that your accounts are safely in TwoFac:
    </p>
    <ul>
      <li><strong>Delete the exported <code>.2fas</code> file</strong> from your device to ensure your secrets don't remain in plain text.</li>
      <li><strong>Enable Biometric Unlock</strong> in TwoFac to protect your 2FA codes from unauthorized access.</li>
      <li><strong>Create an Encrypted Backup:</strong> TwoFac offers natively encrypted backups that you can safely store anywhere.</li>
    </ul>

    <h2>Step 4: Say Goodbye to 2FAS (Or Don't!)</h2>
    <p>
      Once you have verified that your codes are generating correctly in TwoFac across your devices, you can safely uninstall 2FAS. However, since exporting does not invalidate your current secrets, you can also choose to keep 2FAS installed and use both authenticator apps simultaneously!
    </p>
  </MigratePageLayout>
</template>
