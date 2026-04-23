<script setup lang="ts">
import { useSEO } from '../../composables/useSEO'
import MigratePageLayout from '../../components/MigratePageLayout.vue'
import type { HowToStep } from '../../components/MigratePageLayout.vue'

useSEO({
  title: 'How to Migrate from Google Authenticator to TwoFac: 2026 Migration Guide',
  description: 'Step-by-step guide to migrate your 2FA accounts from Google Authenticator to TwoFac using the built-in QR export feature.',
  canonicalPath: '/migrate/google-authenticator',
})

const howToSteps: HowToStep[] = [
  { name: 'Export from Google Authenticator', text: 'Open the Google Authenticator app on your current device. Tap the Menu icon in the top right corner. Select Transfer accounts or Transfer codes. Tap Export accounts. Select the accounts you wish to export and tap Next. A QR code will appear on your screen containing your 2FA secrets.' },
  { name: 'Import into TwoFac', text: 'Open the TwoFac app on your target device. Click or tap the Add Account button. Select the Scan QR Code option. Scan the QR code displayed on your Google Authenticator app. TwoFac will instantly import your accounts.' },
  { name: 'Secure Your New Setup', text: 'Head to Settings to enable Biometric Unlock and create an Encrypted Backup to protect your 2FA codes.' },
  { name: 'Clean Up', text: 'Once you have verified that your codes are generating correctly in TwoFac, you can safely delete the accounts from Google Authenticator and uninstall it.' }
]
</script>

<template>
  <MigratePageLayout
    app-name="Google Authenticator"
    hero-description="Migrating from Google Authenticator to TwoFac takes just a few minutes using Google's built-in QR export feature. No manual re-entry required."
    :how-to-steps="howToSteps"
  >
    <h2>Step 1: Export from Google Authenticator</h2>
    <ol>
      <li>Open the <strong>Google Authenticator</strong> app on your current device.</li>
      <li>Tap the <strong>Menu</strong> icon (three dots or lines) in the top right corner.</li>
      <li>Select <strong>Transfer accounts</strong> or <strong>Transfer codes</strong>.</li>
      <li>Tap <strong>Export accounts</strong>. You may be asked to authenticate with your PIN or biometrics.</li>
      <li>Select the accounts you wish to export and tap <strong>Next</strong>.</li>
      <li>A <strong>QR code</strong> will appear on your screen containing your 2FA secrets.</li>
    </ol>
    <p><em>Note: Google Authenticator may block screenshots on this screen. You will need to scan this code directly using TwoFac on a different device or your computer's webcam.</em></p>

    <h2>Step 2: Import into TwoFac</h2>
    <ol>
      <li>Open the <strong>TwoFac</strong> app on your target device (e.g., your new phone, or desktop app).</li>
      <li>Click or tap the <strong>Add Account</strong> button.</li>
      <li>Select the <strong>Scan QR Code</strong> option (or paste if you somehow extracted the URI).</li>
      <li>Scan the QR code displayed on your Google Authenticator app.</li>
      <li>TwoFac will instantly import your accounts! If you have many accounts, Google Authenticator might display multiple QR codes—just scan them one by one.</li>
    </ol>

    <h2>Step 3: Secure Your New Setup</h2>
    <p>
      Now that your accounts are in TwoFac, we highly recommend heading to <strong>Settings</strong> to:
    </p>
    <ul>
      <li><strong>Enable Biometric Unlock:</strong> Protect your 2FA codes from unauthorized access.</li>
      <li><strong>Create an Encrypted Backup:</strong> Export your accounts to an encrypted file so you never lose them if you lose your device.</li>
    </ul>

    <h2>Step 4: Clean Up (Optional)</h2>
    <p>
      Once you have verified that your codes are generating correctly in TwoFac, you can safely delete the accounts from Google Authenticator and uninstall it. Alternatively, since scanning the export QR code simply copies your secrets, you can also continue to use both authenticator apps simultaneously!
    </p>
  </MigratePageLayout>
</template>
