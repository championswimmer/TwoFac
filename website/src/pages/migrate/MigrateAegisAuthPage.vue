<script setup lang="ts">
import { useSEO } from '../../composables/useSEO'
import MigratePageLayout from '../../components/MigratePageLayout.vue'
import type { HowToStep } from '../../components/MigratePageLayout.vue'

useSEO({
  title: 'How to Migrate from Aegis Authenticator to TwoFac: 2026 Migration Guide',
  description: 'Step-by-step guide to migrate your 2FA accounts from Aegis Authenticator to TwoFac using Aegis plain-text URI exports.',
  canonicalPath: '/migrate/aegis-authenticator',
})

const howToSteps: HowToStep[] = [
  { name: 'Export a Plain-Text URI List from Aegis', text: 'Open Aegis on Android. Go to Settings, then Import / Export. Choose Export and select the plain-text URI list format rather than the encrypted vault export. Save the file to a secure location on your device.' },
  { name: 'Import the URI List into TwoFac', text: 'Open TwoFac, go to Settings, and use the Import option to select the text file exported from Aegis. TwoFac can read newline-separated otpauth:// entries and will add your accounts to the vault.' },
  { name: 'Secure and Verify', text: 'Confirm that every code works in TwoFac, then delete the plain-text export file. Enable biometric lock in TwoFac and create a new encrypted backup.' },
]
</script>

<template>
  <MigratePageLayout
    app-name="Aegis Authenticator"
    hero-description="Migrating from Aegis to TwoFac is usually straightforward because Aegis can export a standard plain-text URI list that TwoFac can import."
    :how-to-steps="howToSteps"
  >
    <h2>Step 1: Export a Plain-Text URI List from Aegis</h2>
    <p>
      Aegis supports several export formats. For migration to TwoFac, the one you want is the
      <strong>plain-text URI list</strong> export because it contains standard <code>otpauth://</code> entries.
    </p>
    <ol>
      <li>Open <strong>Aegis Authenticator</strong> on your Android device.</li>
      <li>Go to <strong>Settings</strong>.</li>
      <li>Open <strong>Import / Export</strong>.</li>
      <li>Tap <strong>Export</strong>.</li>
      <li>Select <strong>Plain-text URI list</strong> rather than an encrypted vault backup or Aegis-specific file.</li>
      <li>Save the file to a secure location you can access from the device where you will import into TwoFac.</li>
    </ol>
    <p>
      <em>Security warning:</em> this export is intentionally readable so other apps can import it. That also means anyone who gets
      the file can generate your codes. Keep it temporary and protected.
    </p>

    <h2>Step 2: Import the File into TwoFac</h2>
    <ol>
      <li>Open <strong>TwoFac</strong>.</li>
      <li>Go to <strong>Settings</strong>.</li>
      <li>Choose the <strong>Import</strong> option.</li>
      <li>Select the text file you exported from Aegis.</li>
      <li>Review the imported accounts in TwoFac and make sure labels, issuers, and account names look correct.</li>
    </ol>
    <p>
      If you accidentally exported an encrypted Aegis vault instead of the plain-text URI list, go back to Aegis and export again in
      the interoperable plain-text format.
    </p>

    <h2>Step 3: Verify Everything Before Cleaning Up</h2>
    <p>
      Before you delete anything, test a few important logins with codes from TwoFac. If you use any less-common HOTP entries, verify
      those carefully because they are counter-based instead of time-based.
    </p>
    <ul>
      <li><strong>Delete the plain-text export file</strong> as soon as you are satisfied the migration worked.</li>
      <li><strong>Enable Biometric Lock</strong> inside TwoFac.</li>
      <li><strong>Create an Encrypted Backup</strong> from TwoFac so future device moves do not require plain-text exports.</li>
    </ul>

    <h2>Step 4: Keep Aegis Until You Are Confident</h2>
    <p>
      Exporting from Aegis does not invalidate your tokens, so you can keep Aegis installed while you confirm everything works in
      TwoFac. Once you trust your new setup, you can remove Aegis or keep it temporarily as a fallback.
    </p>
  </MigratePageLayout>
</template>
