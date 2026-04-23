<script setup lang="ts">
import { useSEO } from '../../composables/useSEO'
import MigratePageLayout from '../../components/MigratePageLayout.vue'

useSEO({
  title: 'How to Migrate from Bitwarden Authenticator to TwoFac',
  description: 'Step-by-step guide to migrate your 2FA accounts from Bitwarden Authenticator or Bitwarden Password Manager to TwoFac.',
  canonicalPath: '/migrate/bitwarden',
})
</script>

<template>
  <MigratePageLayout
    app-name="Bitwarden Authenticator"
    hero-description="Migrating from Bitwarden Authenticator (or the Bitwarden Password Manager) to TwoFac involves exporting your vault data and transferring your TOTP seeds."
  >
    <h2>Understanding Your Bitwarden Setup</h2>
    <p>
      Bitwarden has two places where your 2FA codes might live:
    </p>
    <ul>
      <li><strong>Standalone App:</strong> The mobile-only Bitwarden Authenticator app.</li>
      <li><strong>Password Manager Vault:</strong> The integrated authenticator inside your Bitwarden password manager (Premium feature).</li>
    </ul>

    <h2>Option A: Exporting from the Standalone App</h2>
    <p>If you only have codes stored locally in the Bitwarden Authenticator app:</p>
    <ol>
      <li>Open the <strong>Bitwarden Authenticator</strong> app on your mobile device.</li>
      <li>Tap the <strong>Settings</strong> (gear) icon.</li>
      <li>Tap <strong>Export</strong>.</li>
      <li>Select your preferred File format (<strong>JSON</strong> is recommended).</li>
      <li>Tap <strong>Export items</strong> and save the file to your device.</li>
    </ol>
    <p><em>Note: If you use the "Sync with Bitwarden" feature, your codes are in your main vault instead. Use Option B below.</em></p>

    <h2>Option B: Exporting from the Bitwarden Password Manager</h2>
    <p>If your 2FA codes are synced inside your Bitwarden Password Manager vault:</p>
    <ol>
      <li>Log in to the <strong>Bitwarden Web Vault</strong> or Desktop app.</li>
      <li>Go to <strong>Tools</strong> &rarr; <strong>Export Vault</strong>.</li>
      <li>Select <strong>.json</strong> as the format. This is the most reliable format for preserving TOTP seeds (the field is named <code>totp</code>).</li>
      <li>Enter your master password to confirm and download the JSON file.</li>
    </ol>

    <h2>Step 2: Import into TwoFac</h2>
    <p>
      Because Bitwarden exports may contain full password vault data (if you used Option B), TwoFac does not natively ingest Bitwarden's entire password JSON schema out-of-the-box. The safest way to migrate is manually mapping your TOTP seeds, or using the standalone app's <code>otpauth://</code> strings.
    </p>
    <ol>
      <li>Open the exported JSON file on a secure, offline computer.</li>
      <li>Locate the <code>totp</code> fields (or <code>otpauth://</code> URIs) for each of your accounts.</li>
      <li>Open <strong>TwoFac</strong> and click <strong>Add Account</strong>.</li>
      <li>Manually paste the secret seeds or the <code>otpauth://</code> URIs to securely add each account.</li>
    </ol>
    <p>
      <em>Security Warning:</em> The exported JSON file contains your 2FA secrets (and potentially your passwords!) in <strong>plain text</strong>. Once you have successfully imported your secrets into TwoFac, permanently delete the export file.
    </p>

    <h2>Step 3: Secure Your New Setup</h2>
    <p>
      Keep your 2FA codes separated from your password manager for maximum security:
    </p>
    <ul>
      <li><strong>Delete the unencrypted export file</strong> from your computer and empty the trash.</li>
      <li><strong>Enable Biometric Unlock</strong> in TwoFac to protect your codes.</li>
      <li><strong>Create an Encrypted Backup:</strong> Generate an encrypted backup directly from TwoFac so that your future backups are secured with a passkey.</li>
    </ul>
  </MigratePageLayout>
</template>
