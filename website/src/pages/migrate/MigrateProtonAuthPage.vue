<script setup lang="ts">
import { useSEO } from '../../composables/useSEO'
import MigratePageLayout from '../../components/MigratePageLayout.vue'
import type { HowToStep } from '../../components/MigratePageLayout.vue'

useSEO({
  title: 'How to Migrate from Proton Authenticator to TwoFac: 2026 Migration Guide',
  description: 'Step-by-step guide to migrate your 2FA accounts from Proton Authenticator to TwoFac using the safest manual method.',
  canonicalPath: '/migrate/proton-authenticator',
})

const howToSteps: HowToStep[] = [
  { name: 'Prepare Your Accounts', text: 'Keep Proton Authenticator installed and gather your recovery codes before changing anything. For each service, sign in and open its security or two-factor authentication settings.' },
  { name: 'Add TwoFac Account-by-Account', text: 'Where a service allows multiple authenticator apps, add TwoFac as a second app first. Otherwise disable the old authenticator app method, re-enable it, and scan the new QR code with TwoFac.' },
  { name: 'Verify and Remove Proton Later', text: 'Test every migrated account with a fresh code from TwoFac before removing it from Proton Authenticator. When everything is confirmed, create an encrypted backup in TwoFac and then remove Proton if you want.' },
]
</script>

<template>
  <MigratePageLayout
    app-name="Proton Authenticator"
    hero-description="The safest way to migrate from Proton Authenticator to TwoFac today is to re-register each account one by one while keeping Proton installed until every login is verified."
    :how-to-steps="howToSteps"
  >
    <h2>Why the Manual Method Is the Safest Option</h2>
    <p>
      Proton Authenticator uses its own encrypted backup and restore flow. That is great for Proton-to-Proton restores, but it is not
      a direct import path for TwoFac today. Because of that, the most reliable migration method is the classic one:
      <strong>re-register each service with TwoFac</strong>.
    </p>
    <p>
      It takes longer than importing a plain-text export, but it avoids format-conversion mistakes and guarantees that every service
      is explicitly enrolled with your new authenticator.
    </p>

    <h2>Step 1: Prepare Before Changing Anything</h2>
    <ol>
      <li>Keep <strong>Proton Authenticator</strong> installed and working throughout the migration.</li>
      <li>Collect any <strong>recovery codes</strong> the service gives you before you modify 2FA settings.</li>
      <li>Make a shortlist of your most important accounts first: email, password manager, cloud storage, banking, GitHub, and work logins.</li>
      <li>For each account, sign in and open its <strong>Security</strong> or <strong>Two-Factor Authentication</strong> settings.</li>
    </ol>

    <h2>Step 2: Add TwoFac One Service at a Time</h2>
    <p>
      Different services behave differently, so use the safest option each site offers:
    </p>
    <ol>
      <li>
        <strong>If the service allows more than one authenticator app:</strong> add TwoFac as a second authenticator first, test it,
        and only then remove Proton from that account.
      </li>
      <li>
        <strong>If the service allows only one authenticator app:</strong> temporarily disable authenticator-app 2FA, re-enable it,
        and scan the newly issued QR code using <strong>TwoFac</strong>.
      </li>
      <li>Immediately test the new code before leaving the security page.</li>
    </ol>
    <p>
      This slow-and-steady approach is especially important for email accounts and password managers, because those are the accounts
      you need to recover everything else.
    </p>

    <h2>Step 3: Verify Everything Before Removing Proton</h2>
    <p>
      Do not uninstall Proton Authenticator until every important service has been tested with a live login using a code from TwoFac.
    </p>
    <ul>
      <li><strong>Enable Biometric Lock</strong> in TwoFac.</li>
      <li><strong>Create an Encrypted Backup</strong> in TwoFac once your accounts are migrated.</li>
      <li><strong>Store new recovery codes securely</strong> if any service regenerated them during re-enrollment.</li>
    </ul>

    <h2>Step 4: Remove Proton Authenticator Only When Ready</h2>
    <p>
      Once you have successfully used TwoFac for all critical services across your devices, you can remove the entries from Proton
      Authenticator or uninstall it entirely. Until then, keeping Proton as a temporary fallback is the safest choice.
    </p>
  </MigratePageLayout>
</template>
